package com.example.ed.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.RecyclerView

object AccessibilityHelper {

    /**
     * Check if accessibility services are enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled
    }

    /**
     * Check if TalkBack is enabled
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_SPOKEN
        )
        return enabledServices.isNotEmpty()
    }

    /**
     * Set content description for better screen reader support
     */
    fun setContentDescription(view: View, description: String) {
        ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = description
            }
        })
    }

    /**
     * Set accessibility actions for interactive elements
     */
    fun setAccessibilityActions(view: View, actions: List<AccessibilityAction>) {
        ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                actions.forEach { action ->
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            action.id,
                            action.label
                        )
                    )
                }
            }

            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: android.os.Bundle?
            ): Boolean {
                actions.find { it.id == action }?.let { accessibilityAction ->
                    accessibilityAction.action()
                    return true
                }
                return super.performAccessibilityAction(host, action, args)
            }
        })
    }

    /**
     * Announce text to screen readers
     */
    fun announceForAccessibility(view: View, text: String) {
        if (isAccessibilityEnabled(view.context)) {
            view.announceForAccessibility(text)
        }
    }

    /**
     * Send accessibility event
     */
    fun sendAccessibilityEvent(view: View, eventType: Int, text: String? = null) {
        if (isAccessibilityEnabled(view.context)) {
            val event = AccessibilityEvent.obtain(eventType)
            text?.let { event.text.add(it) }
            view.sendAccessibilityEventUnchecked(event)
        }
    }

    /**
     * Configure RecyclerView for better accessibility
     */
    fun configureRecyclerViewAccessibility(recyclerView: RecyclerView, itemDescription: String) {
        ViewCompat.setAccessibilityDelegate(recyclerView, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                val adapter = recyclerView.adapter
                if (adapter != null) {
                    info.contentDescription = "$itemDescription list with ${adapter.itemCount} items"
                    info.className = RecyclerView::class.java.name
                }
            }
        })
    }

    /**
     * Set minimum touch target size (48dp)
     */
    fun setMinimumTouchTarget(view: View) {
        val minSize = (48 * view.context.resources.displayMetrics.density).toInt()
        view.minimumWidth = minSize
        view.minimumHeight = minSize
    }

    /**
     * Improve text contrast and readability
     */
    fun improveTextReadability(textView: TextView, isHighContrast: Boolean = false) {
        if (isHighContrast) {
            textView.setTextColor(android.graphics.Color.BLACK)
            textView.setBackgroundColor(android.graphics.Color.WHITE)
        }
        
        // Increase text size for better readability
        val currentSize = textView.textSize
        textView.textSize = currentSize * 1.1f
        
        // Improve line spacing
        textView.setLineSpacing(textView.lineSpacingExtra + 2f, textView.lineSpacingMultiplier)
    }

    /**
     * Set focus order for keyboard navigation
     */
    fun setFocusOrder(views: List<View>) {
        for (i in 0 until views.size - 1) {
            views[i].nextFocusDownId = views[i + 1].id
            views[i + 1].nextFocusUpId = views[i].id
        }
    }

    /**
     * Configure view for keyboard navigation
     */
    fun configureKeyboardNavigation(view: View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        
        // Add visual focus indicator
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.alpha = 0.8f
                announceForAccessibility(v, "Focused on ${v.contentDescription ?: "item"}")
            } else {
                v.alpha = 1.0f
            }
        }
    }

    /**
     * Create accessible form labels
     */
    fun linkLabelToInput(label: TextView, input: View) {
        ViewCompat.setLabelFor(label, input.id)
        input.contentDescription = label.text
    }

    /**
     * Configure error announcements for form validation
     */
    fun announceError(view: View, errorMessage: String) {
        Log.e("AccessibilityHelper", errorMessage)
        announceForAccessibility(view, "Error: $errorMessage")
        sendAccessibilityEvent(view, AccessibilityEvent.TYPE_VIEW_FOCUSED, "Error: $errorMessage")
    }

    /**
     * Configure loading state announcements
     */
    fun announceLoadingState(view: View, isLoading: Boolean, loadingMessage: String = "Loading") {
        if (isLoading) {
            announceForAccessibility(view, loadingMessage)
            view.contentDescription = loadingMessage
        } else {
            announceForAccessibility(view, "Content loaded")
        }
    }

    /**
     * Configure dynamic content changes
     */
    fun announceContentChange(view: View, changeDescription: String) {
        sendAccessibilityEvent(
            view,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            changeDescription
        )
    }

    /**
     * Set up accessible navigation for ViewPager
     */
    fun configureViewPagerAccessibility(viewPager: androidx.viewpager2.widget.ViewPager2, pageDescriptions: List<String>) {
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position < pageDescriptions.size) {
                    announceForAccessibility(viewPager, "Page ${position + 1} of ${pageDescriptions.size}: ${pageDescriptions[position]}")
                }
            }
        })
    }

    /**
     * Configure accessible dialogs
     */
    fun configureDialogAccessibility(dialog: android.app.Dialog, title: String, description: String? = null) {
        dialog.window?.decorView?.let { decorView ->
            decorView.contentDescription = title
            description?.let {
                announceForAccessibility(decorView, "$title. $it")
            }
        }
    }

    /**
     * Set up accessible progress indicators
     */
    fun configureProgressAccessibility(progressView: View, currentProgress: Int, maxProgress: Int, description: String) {
        val progressText = "Progress: $currentProgress of $maxProgress. $description"
        progressView.contentDescription = progressText
        
        ViewCompat.setAccessibilityDelegate(progressView, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.rangeInfo = AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                    AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT,
                    0f,
                    maxProgress.toFloat(),
                    currentProgress.toFloat()
                )
            }
        })
    }

    /**
     * Configure accessible tabs
     */
    fun configureTabAccessibility(tabLayout: com.google.android.material.tabs.TabLayout) {
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.view?.contentDescription = "${tab?.text} tab, ${i + 1} of ${tabLayout.tabCount}"
        }
    }

    /**
     * Set up accessible search functionality
     */
    fun configureSearchAccessibility(searchView: androidx.appcompat.widget.SearchView, resultsCount: Int) {
        searchView.queryHint = "Search courses and assignments"
        
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                announceForAccessibility(searchView, "Search completed. $resultsCount results found for $query")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    data class AccessibilityAction(
        val id: Int,
        val label: String,
        val action: () -> Unit
    )

    /**
     * Common accessibility actions
     */
    object Actions {
        const val ACTION_EDIT = 1001
        const val ACTION_DELETE = 1002
        const val ACTION_SHARE = 1003
        const val ACTION_FAVORITE = 1004
        const val ACTION_DOWNLOAD = 1005
        
        fun createEditAction(onEdit: () -> Unit) = AccessibilityAction(ACTION_EDIT, "Edit", onEdit)
        fun createDeleteAction(onDelete: () -> Unit) = AccessibilityAction(ACTION_DELETE, "Delete", onDelete)
        fun createShareAction(onShare: () -> Unit) = AccessibilityAction(ACTION_SHARE, "Share", onShare)
        fun createFavoriteAction(onFavorite: () -> Unit) = AccessibilityAction(ACTION_FAVORITE, "Add to favorites", onFavorite)
        fun createDownloadAction(onDownload: () -> Unit) = AccessibilityAction(ACTION_DOWNLOAD, "Download", onDownload)
    }

    /**
     * Apply accessibility improvements to entire view hierarchy
     */
    fun applyAccessibilityImprovements(rootView: ViewGroup, context: Context) {
        val isHighContrast = isAccessibilityEnabled(context)
        
        fun processView(view: View) {
            // Set minimum touch targets
            if (view.isClickable || view.isFocusable) {
                setMinimumTouchTarget(view)
                configureKeyboardNavigation(view)
            }
            
            // Improve text readability
            if (view is TextView) {
                improveTextReadability(view, isHighContrast)
            }
            
            // Process child views
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    processView(view.getChildAt(i))
                }
            }
        }
        
        processView(rootView)
    }
}