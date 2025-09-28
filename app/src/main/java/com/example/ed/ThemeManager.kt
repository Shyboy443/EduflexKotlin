package com.example.ed

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_SELECTED_THEME = "selected_theme"
    private const val KEY_THEME_SELECTED = "theme_selected"
    
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    
    fun getThemePreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getCurrentTheme(context: Context): String {
        val prefs = getThemePreferences(context)
        return prefs.getString(KEY_SELECTED_THEME, THEME_LIGHT) ?: THEME_LIGHT
    }
    
    fun isThemeSelected(context: Context): Boolean {
        val prefs = getThemePreferences(context)
        return prefs.getBoolean(KEY_THEME_SELECTED, false)
    }
    
    fun saveTheme(context: Context, theme: String) {
        val prefs = getThemePreferences(context)
        prefs.edit()
            .putString(KEY_SELECTED_THEME, theme)
            .putBoolean(KEY_THEME_SELECTED, true)
            .apply()
    }
    
    fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
    
    fun applyCurrentTheme(context: Context) {
        val currentTheme = getCurrentTheme(context)
        applyTheme(currentTheme)
    }
    
    fun isDarkMode(context: Context): Boolean {
        return getCurrentTheme(context) == THEME_DARK
    }
    
    fun toggleTheme(context: Context) {
        val currentTheme = getCurrentTheme(context)
        val newTheme = if (currentTheme == THEME_LIGHT) THEME_DARK else THEME_LIGHT
        saveTheme(context, newTheme)
        applyTheme(newTheme)
    }
    
    /**
     * Forces recreation of all activities to apply theme changes
     * Call this after changing theme in an activity
     */
    fun applyThemeAndRecreate(activity: android.app.Activity, theme: String) {
        saveTheme(activity, theme)
        applyTheme(theme)
        activity.recreate()
    }
}