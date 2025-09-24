package com.example.ed.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.example.ed.R
import com.example.ed.databinding.ViewLoadingStateBinding

class LoadingStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewLoadingStateBinding
    
    init {
        binding = ViewLoadingStateBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun showLoading(message: String = "Loading...") {
        binding.apply {
            progressBar.isVisible = true
            tvLoadingMessage.isVisible = true
            tvLoadingMessage.text = message
            btnRetry.isVisible = false
            tvErrorMessage.isVisible = false
        }
        isVisible = true
    }

    fun showError(message: String, onRetry: (() -> Unit)? = null) {
        binding.apply {
            progressBar.isVisible = false
            tvLoadingMessage.isVisible = false
            tvErrorMessage.isVisible = true
            tvErrorMessage.text = message
            btnRetry.isVisible = onRetry != null
            btnRetry.setOnClickListener { onRetry?.invoke() }
        }
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }
}