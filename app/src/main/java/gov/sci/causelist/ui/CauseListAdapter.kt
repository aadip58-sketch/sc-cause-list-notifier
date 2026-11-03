package gov.sci.causelist.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gov.sci.causelist.databinding.ItemCauseListBinding
import gov.sci.causelist.model.CauseListItem

class CauseListAdapter(
    private val context: Context
) : ListAdapter<CauseListItem, CauseListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCauseListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCauseListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CauseListItem) {
            binding.tvTitle.text = item.title
            binding.tvDate.text = "Date: ${item.date}"
            binding.tvCourt.text = "Section: ${item.court}"
            binding.tvTag.text = item.category

            binding.btnOpenPdf.setOnClickListener {
                openPdf(item.pdfUrl)
            }

            binding.btnShare.setOnClickListener {
                shareCauseList(item)
            }

            binding.root.setOnClickListener {
                openPdf(item.pdfUrl)
            }
        }

        private fun openPdf(url: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // If no direct PDF viewer, open in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(browserIntent)
            }
        }

        private fun shareCauseList(item: CauseListItem) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Supreme Court Cause List - ${item.date} (${item.category})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Supreme Court Cause List (${item.category})\nDate: ${item.date}\nLink: ${item.pdfUrl}"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Cause List"))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CauseListItem>() {
        override fun areItemsTheSame(oldItem: CauseListItem, newItem: CauseListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CauseListItem, newItem: CauseListItem): Boolean {
            return oldItem == newItem
        }
    }
}
