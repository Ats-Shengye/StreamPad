package com.streampad.bt.utils

import android.content.Context

/**
 * Factory for creating ProfileManager instances.
 * Provides centralized instantiation for dependency injection.
 */
object ProfileManagerFactory {
    /**
     * Create a ProfileManager instance with the given context.
     * @param context Application or Activity context
     * @return New ProfileManager instance
     */
    fun create(context: Context): ProfileManager {
        return ProfileManager(context)
    }
}
