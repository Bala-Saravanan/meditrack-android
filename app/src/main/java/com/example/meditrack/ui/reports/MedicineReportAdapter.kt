package com.example.meditrack.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.model.MedicineAdherenceSummary

class MedicineReportAdapter : ListAdapter<MedicineAdherenceSummary, MedicineReportAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_med_name)
        private val tvRatio: TextView = itemView.findViewById(R.id.tv_med_ratio)
        private val tvPercentBadge: TextView = itemView.findViewById(R.id.tv_med_percent_badge)
        private val pbProgress: ProgressBar = itemView.findViewById(R.id.pb_med_progress)
        private val tvSkipped: TextView = itemView.findViewById(R.id.tv_med_skipped)
        private val tvUnconfirmed: TextView = itemView.findViewById(R.id.tv_med_unconfirmed)
        private val tvLate: TextView = itemView.findViewById(R.id.tv_med_late)

        fun bind(item: MedicineAdherenceSummary) {
            tvName.text = item.medicineName
            tvRatio.text = "${item.takenCount} of ${item.totalScheduled} Doses Taken"
            tvPercentBadge.text = "${item.adherencePercentage}%"
            pbProgress.progress = item.adherencePercentage

            tvSkipped.text = "${item.skippedCount} Skipped"
            tvUnconfirmed.text = "${item.unconfirmedCount} Unconfirmed"
            tvLate.text = "${item.lateCount} Late ${if (item.lateCount == 1) "Entry" else "Entries"}"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MedicineAdherenceSummary>() {
        override fun areItemsTheSame(oldItem: MedicineAdherenceSummary, newItem: MedicineAdherenceSummary): Boolean {
            return oldItem.medicineName == newItem.medicineName
        }

        override fun areContentsTheSame(oldItem: MedicineAdherenceSummary, newItem: MedicineAdherenceSummary): Boolean {
            return oldItem == newItem
        }
    }
}
