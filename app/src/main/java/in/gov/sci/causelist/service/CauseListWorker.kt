package in.gov.sci.causelist.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import in.gov.sci.causelist.parser.CauseListParser
import in.gov.sci.causelist.util.NotificationHelper
import in.gov.sci.causelist.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CauseListWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(applicationContext)

        if (!prefs.isNotificationsEnabled()) {
            return@withContext Result.success()
        }

        try {
            val causeLists = CauseListParser.fetchCauseLists()
            if (causeLists.isEmpty()) {
                return@withContext Result.retry()
            }

            val seenIds = prefs.getSeenIds()
            val isFirstRun = prefs.isFirstRun()

            if (isFirstRun) {
                // On first run, seed existing cause lists so we don't spam notifications
                prefs.addSeenIds(causeLists.map { it.id })
            } else {
                // Find cause lists that weren't seen before
                val newLists = causeLists.filter { it.id !in seenIds }
                if (newLists.isNotEmpty()) {
                    NotificationHelper.sendNewCauseListNotification(applicationContext, newLists)
                    prefs.addSeenIds(newLists.map { it.id })
                }
            }

            prefs.setLastChecked(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
