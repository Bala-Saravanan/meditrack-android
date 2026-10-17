package com.example.meditrack.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.meditrack.R

class ReportsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_placeholder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_placeholder_title).text = getString(R.string.title_reports)
        view.findViewById<ImageView>(R.id.iv_placeholder_icon).setImageResource(R.drawable.ic_reports)
        view.findViewById<TextView>(R.id.tv_placeholder_heading).text = "Adherence Reports"
        view.findViewById<TextView>(R.id.tv_placeholder_desc).text = "Schedule-specific compliance analytics and PDF exports."
    }
}
