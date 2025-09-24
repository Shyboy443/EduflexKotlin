package com.example.ed

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ed.adapters.WidgetTypeAdapter
import com.example.ed.models.WidgetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddWidgetDialog : DialogFragment() {

    private lateinit var widgetTypesRecyclerView: RecyclerView
    private lateinit var cancelButton: MaterialButton

    private var availableWidgets: List<WidgetType> = emptyList()
    private var onWidgetSelectedListener: ((WidgetType) -> Unit)? = null
    private lateinit var widgetTypeAdapter: WidgetTypeAdapter

    companion object {
        private const val ARG_AVAILABLE_WIDGETS = "available_widgets"

        fun newInstance(availableWidgets: List<WidgetType>): AddWidgetDialog {
            val fragment = AddWidgetDialog()
            val args = Bundle()
            args.putSerializable(ARG_AVAILABLE_WIDGETS, ArrayList(availableWidgets))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            availableWidgets = it.getSerializable(ARG_AVAILABLE_WIDGETS) as? List<WidgetType> ?: emptyList()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_widget, null)

        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Widget")
            .setView(view)
            .create()
    }

    private fun initializeViews(view: View) {
        widgetTypesRecyclerView = view.findViewById(R.id.widgetTypesRecyclerView)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupRecyclerView() {
        widgetTypeAdapter = WidgetTypeAdapter(availableWidgets) { widgetType ->
            onWidgetSelectedListener?.invoke(widgetType)
            dismiss()
        }
        
        widgetTypesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        widgetTypesRecyclerView.adapter = widgetTypeAdapter
    }

    private fun setupClickListeners() {
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    fun setOnWidgetSelectedListener(listener: (WidgetType) -> Unit) {
        onWidgetSelectedListener = listener
    }
}