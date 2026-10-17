package com.example.meditrack.ui.dose

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R

data class DateStripItem(
    val dateMillis: Long,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val isToday: Boolean,
    val isSelected: Boolean
)

class DateStripAdapter(
    private val onDateClicked: (Long) -> Unit
) : RecyclerView.Adapter<DateStripAdapter.DateViewHolder>() {

    private val items = mutableListOf<DateStripItem>()

    fun submitList(newItems: List<DateStripItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_chip, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutChip: View = itemView.findViewById(R.id.layout_date_chip)
        private val tvDayOfWeek: TextView = itemView.findViewById(R.id.tv_day_of_week)
        private val tvDayOfMonth: TextView = itemView.findViewById(R.id.tv_day_of_month)
        private val tvTodaySublabel: TextView = itemView.findViewById(R.id.tv_today_sublabel)

        fun bind(item: DateStripItem) {
            tvDayOfWeek.text = item.dayOfWeek
            tvDayOfMonth.text = item.dayOfMonth

            if (item.isSelected) {
                layoutChip.setBackgroundResource(R.drawable.bg_date_chip_selected)
                tvDayOfWeek.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_fixed))
                tvDayOfMonth.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                if (item.isToday) {
                    tvTodaySublabel.visibility = View.VISIBLE
                    tvTodaySublabel.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_fixed))
                } else {
                    tvTodaySublabel.visibility = View.GONE
                }
            } else {
                layoutChip.setBackgroundResource(R.drawable.bg_date_chip_unselected)
                tvDayOfWeek.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface_variant))
                tvDayOfMonth.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface))
                if (item.isToday) {
                    tvTodaySublabel.visibility = View.VISIBLE
                    tvTodaySublabel.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                } else {
                    tvTodaySublabel.visibility = View.GONE
                }
            }

            itemView.setOnClickListener {
                onDateClicked(item.dateMillis)
            }
        }
    }
}
