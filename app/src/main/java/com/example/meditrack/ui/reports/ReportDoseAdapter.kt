package com.example.meditrack.ui.reports

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

sealed class ReportDoseListItem {
    data class UpcomingHeader(val count: Int) : ReportDoseListItem()
    data class DateHeader(val dateText: String, val count: Int) : ReportDoseListItem()
    data class DoseItem(val dose: DoseRecord) : ReportDoseListItem()
}

class ReportDoseAdapter : ListAdapter<ReportDoseListItem, RecyclerView.ViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_UPCOMING_HEADER = 1
        private const val TYPE_DOSE_ITEM = 2

        object DiffCallback : DiffUtil.ItemCallback<ReportDoseListItem>() {
            override fun areItemsTheSame(
                oldItem: ReportDoseListItem,
                newItem: ReportDoseListItem
            ): Boolean {
                return when {
                    oldItem is ReportDoseListItem.DateHeader && newItem is ReportDoseListItem.DateHeader ->
                        oldItem.dateText == newItem.dateText
                    oldItem is ReportDoseListItem.UpcomingHeader && newItem is ReportDoseListItem.UpcomingHeader ->
                        true
                    oldItem is ReportDoseListItem.DoseItem && newItem is ReportDoseListItem.DoseItem ->
                        oldItem.dose.id == newItem.dose.id
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: ReportDoseListItem,
                newItem: ReportDoseListItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ReportDoseListItem.DateHeader -> TYPE_DATE_HEADER
            is ReportDoseListItem.UpcomingHeader -> TYPE_UPCOMING_HEADER
            is ReportDoseListItem.DoseItem -> TYPE_DOSE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE_HEADER, TYPE_UPCOMING_HEADER -> {
                val view = inflater.inflate(R.layout.item_report_date_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_report_dose_row, parent, false)
                DoseViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ReportDoseListItem.DateHeader -> (holder as HeaderViewHolder).bindDate(item)
            is ReportDoseListItem.UpcomingHeader -> (holder as HeaderViewHolder).bindUpcoming(item)
            is ReportDoseListItem.DoseItem -> (holder as DoseViewHolder).bind(item.dose)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_header_icon)
        private val tvHeading: TextView = itemView.findViewById(R.id.tv_report_date_heading)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_report_date_count)

        fun bindDate(item: ReportDoseListItem.DateHeader) {
            ivIcon.setImageResource(R.drawable.ic_calendar)
            ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.primary))
            tvHeading.text = item.dateText
            tvCount.text = "${item.count} ${if (item.count == 1) "Dose" else "Doses"}"
        }

        fun bindUpcoming(item: ReportDoseListItem.UpcomingHeader) {
            ivIcon.setImageResource(R.drawable.ic_alarm)
            ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.status_due))
            tvHeading.text = "Upcoming Doses (Not Yet Due)"
            tvHeading.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_due))
            tvCount.text = "${item.count} ${if (item.count == 1) "Dose" else "Doses"}"
        }
    }

    inner class DoseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMedicine: TextView = itemView.findViewById(R.id.tv_dose_medicine)
        private val tvScheduled: TextView = itemView.findViewById(R.id.tv_dose_scheduled)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_dose_status)
        private val tvRecorded: TextView = itemView.findViewById(R.id.tv_dose_recorded)
        private val tvLateFlag: TextView = itemView.findViewById(R.id.tv_dose_late_flag)

        fun bind(item: DoseRecord) {
            val context = itemView.context
            val now = System.currentTimeMillis()
            val isUpcoming = item.scheduledAt > now

            tvMedicine.text = item.medicineName

            val formattedTime = timeFormat.format(Date(item.scheduledAt))
            val periodLabel = item.period.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            tvScheduled.text = "Scheduled: $formattedTime • $periodLabel"

            if (isUpcoming) {
                tvStatus.text = "UPCOMING"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_pending)
                tvStatus.backgroundTintList = null

                tvRecorded.visibility = View.GONE
                tvLateFlag.visibility = View.GONE
            } else {
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
                    tvRecorded.visibility = View.VISIBLE
                    tvRecorded.text = "Logged: ${timeFormat.format(Date(item.recordedAt))}"
                } else {
                    tvRecorded.visibility = View.VISIBLE
                    tvRecorded.text = "No user response logged"
                }

                if (item.isLate && item.recordedAt != null) {
                    tvLateFlag.visibility = View.VISIBLE
                    tvLateFlag.text = "Late (>60m)"
                } else {
                    tvLateFlag.visibility = View.GONE
                }
            }
        }
    }
}
