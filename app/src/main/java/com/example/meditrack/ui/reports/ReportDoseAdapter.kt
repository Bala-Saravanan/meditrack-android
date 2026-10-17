package com.example.meditrack.ui.reports

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.DoseRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDoseAdapter : ListAdapter<DoseRecord, ReportDoseAdapter.ViewHolder>(DiffCallback) {

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_dose_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMedicine: TextView = itemView.findViewById(R.id.tv_dose_medicine)
        private val tvScheduled: TextView = itemView.findViewById(R.id.tv_dose_scheduled)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_dose_status)
        private val tvRecorded: TextView = itemView.findViewById(R.id.tv_dose_recorded)
        private val tvLateFlag: TextView = itemView.findViewById(R.id.tv_dose_late_flag)

        fun bind(item: DoseRecord) {
            val context = itemView.context
            tvMedicine.text = item.medicineName
            tvScheduled.text = "Scheduled: ${dateTimeFormat.format(Date(item.scheduledAt))}"

            tvStatus.text = item.status
            when (item.status) {
                DoseRecord.STATUS_TAKEN -> {
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_taken))
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_taken)
                    tvStatus.backgroundTintList = null
                }
                DoseRecord.STATUS_SKIPPED -> {
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_skipped))
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_skipped)
                    tvStatus.backgroundTintList = null
                }
                DoseRecord.STATUS_UNCONFIRMED -> {
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_unconfirmed))
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_unconfirmed)
                    tvStatus.backgroundTintList = null
                }
                else -> { // PENDING
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_due))
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_due)
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_due_bg))
                }
            }

            if (item.recordedAt != null && item.recordedAt > 0) {
                tvRecorded.text = "Logged: ${dateTimeFormat.format(Date(item.recordedAt))}"
            } else {
                tvRecorded.text = "No user response logged"
            }

            if (item.isLate) {
                tvLateFlag.visibility = View.VISIBLE
                tvLateFlag.text = "Late (>60m)"
            } else if (item.scheduledAt > System.currentTimeMillis()) {
                tvLateFlag.visibility = View.VISIBLE
                tvLateFlag.text = "Upcoming"
                tvLateFlag.setTextColor(ContextCompat.getColor(context, R.color.outline))
            } else {
                tvLateFlag.visibility = View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DoseRecord>() {
        override fun areItemsTheSame(oldItem: DoseRecord, newItem: DoseRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DoseRecord, newItem: DoseRecord): Boolean {
            return oldItem == newItem
        }
    }
}
