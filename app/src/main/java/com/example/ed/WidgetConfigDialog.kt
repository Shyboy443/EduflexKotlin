package com.example.ed

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.example.ed.models.DashboardWidget
import com.example.ed.models.WidgetSize
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class WidgetConfigDialog : DialogFragment() {

    private lateinit var titleEditText: TextInputEditText
    private lateinit var sizeSpinner: Spinner
    private lateinit var visibilitySwitch: SwitchMaterial
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var widget: DashboardWidget? = null
    private var onWidgetConfiguredListener: ((DashboardWidget) -> Unit)? = null

    companion object {
        private const val ARG_WIDGET = "widget"

        fun newInstance(widget: DashboardWidget): WidgetConfigDialog {
            val fragment = WidgetConfigDialog()
            val args = Bundle()
            args.putSerializable(ARG_WIDGET, widget)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            widget = it.getSerializable(ARG_WIDGET) as? DashboardWidget
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_widget_config, null)

        initializeViews(view)
        setupViews()
        setupClickListeners()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Configure Widget")
            .setView(view)
            .create()
    }

    private fun initializeViews(view: View) {
        titleEditText = view.findViewById(R.id.titleEditText)
        sizeSpinner = view.findViewById(R.id.sizeSpinner)
        visibilitySwitch = view.findViewById(R.id.visibilitySwitch)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupViews() {
        widget?.let { widget ->
            titleEditText.setText(widget.title)
            visibilitySwitch.isChecked = widget.isVisible

            // Setup size spinner
            val sizeOptions = WidgetSize.values().map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sizeOptions
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sizeSpinner.adapter = adapter
            sizeSpinner.setSelection(widget.size.ordinal)
        }
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveConfiguration()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun saveConfiguration() {
        widget?.let { originalWidget ->
            val updatedWidget = originalWidget.copy(
                title = titleEditText.text.toString().trim(),
                size = WidgetSize.values()[sizeSpinner.selectedItemPosition],
                isVisible = visibilitySwitch.isChecked
            )

            onWidgetConfiguredListener?.invoke(updatedWidget)
            dismiss()
        }
    }

    fun setOnWidgetConfiguredListener(listener: (DashboardWidget) -> Unit) {
        onWidgetConfiguredListener = listener
    }
}