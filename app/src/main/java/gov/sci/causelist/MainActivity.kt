package gov.sci.causelist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import gov.sci.causelist.databinding.ActivityMainBinding
import gov.sci.causelist.parser.CauseListParser
import gov.sci.causelist.service.BootReceiver
import gov.sci.causelist.service.CauseListForegroundService
import gov.sci.causelist.ui.CauseListAdapter
import gov.sci.causelist.util.NotificationHelper
import gov.sci.causelist.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CauseListAdapter
    private lateinit var prefs: PreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.layoutPermissionAlert.visibility = View.GONE
            Toast.makeText(this, "Notifications enabled for Cause List drops!", Toast.LENGTH_SHORT).show()
        } else {
            binding.layoutPermissionAlert.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // Setup notification channel
        NotificationHelper.createNotificationChannel(this)

        // Start 2-minute real-time foreground monitoring service
        CauseListForegroundService.startService(this)

        binding.tvMonitoringStatus.text = "Background Monitor Active (Every 2 mins)"

        setupRecyclerView()
        setupListeners()
        checkPermissions()
        promptBatteryOptimization()

        // Initial fetch
        loadCauseLists(isPullToRefresh = false)
    }

    private fun setupRecyclerView() {
        adapter = CauseListAdapter(this)
        binding.rvCauseLists.layoutManager = LinearLayoutManager(this)
        binding.rvCauseLists.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadCauseLists(isPullToRefresh = true)
        }

        binding.btnRefresh.setOnClickListener {
            loadCauseLists(isPullToRefresh = false)
        }

        binding.btnGrantPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                binding.layoutPermissionAlert.visibility = View.VISIBLE
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                binding.layoutPermissionAlert.visibility = View.GONE
            }
        } else {
            binding.layoutPermissionAlert.visibility = View.GONE
        }
    }

    private fun promptBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general settings
                }
            }
        }
    }

    private fun loadCauseLists(isPullToRefresh: Boolean) {
        if (!isPullToRefresh) {
            binding.progressBar.visibility = View.VISIBLE
        }
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    CauseListParser.fetchCauseLists()
                }

                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (items.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    adapter.submitList(items)

                    // Track seen items
                    val seenIds = prefs.getSeenIds()
                    val newItems = items.filter { it.id !in seenIds }

                    if (!prefs.isFirstRun() && newItems.isNotEmpty()) {
                        NotificationHelper.sendNewCauseListNotification(this@MainActivity, newItems)
                    }

                    prefs.addSeenIds(items.map { it.id })
                    prefs.setLastChecked(System.currentTimeMillis())
                    updateLastCheckedUi(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                if (adapter.itemCount == 0) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateLastCheckedUi(timeMillis: Long) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        binding.tvLastChecked.text = "Last checked: ${sdf.format(Date(timeMillis))}"
    }

    override fun onResume() {
        super.onResume()
        val lastChecked = prefs.getLastChecked()
        if (lastChecked > 0) {
            updateLastCheckedUi(lastChecked)
        }
    }
}
