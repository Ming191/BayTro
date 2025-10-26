package com.example.baytro.data.user

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Persistent cache for user role TYPE using DataStore
 * Survives app restarts and process death
 * Note: This only caches the role type (Tenant/Landlord), not the full role data
 */
class UserRoleCache(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_role_cache")

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("cached_user_id")
        private val ROLE_TYPE_KEY = stringPreferencesKey("cached_role_type")
    }

    /**
     * Get the cached role type for a user
     * Returns "Tenant", "Landlord", or null if not cached
     */
    suspend fun getRoleType(userId: String): String? {
        return try {
            val preferences = context.dataStore.data.first()
            val cachedUserId = preferences[USER_ID_KEY]
            val cachedRoleType = preferences[ROLE_TYPE_KEY]

            if (cachedUserId == userId && cachedRoleType != null) cachedRoleType else null
        } catch (e: Exception) {
            Log.e("UserRoleCache", "Failed to read role cache", e)
            null
        }
    }

    suspend fun setRoleType(userId: String, role: Role) {
        val roleTypeString = when (role) {
            is Role.Tenant -> "Tenant"
            is Role.Landlord -> "Landlord"
        }

        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[ROLE_TYPE_KEY] = roleTypeString
        }
    }

    /**
     * Clear all cached data
     * Should be called when user signs out
     */
    suspend fun clearCache() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Log.d("UserRoleCache", "Cache cleared successfully")
        } catch (e: Exception) {
            Log.e("UserRoleCache", "Failed to clear cache", e)
        }
    }
}