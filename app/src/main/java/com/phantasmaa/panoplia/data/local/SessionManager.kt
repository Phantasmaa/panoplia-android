package com.phantasmaa.panoplia.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phantasmaa.panoplia.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("panoplia_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyUsername = stringPreferencesKey("username")
    private val keyIsAdmin = stringPreferencesKey("is_admin")

    val user: Flow<User?> = context.dataStore.data.map { prefs ->
        val name = prefs[keyUsername] ?: return@map null
        User(
            username = name,
            isAdmin = prefs[keyIsAdmin] == "1"
        )
    }

    suspend fun save(user: User) {
        context.dataStore.edit { prefs ->
            prefs[keyUsername] = user.username
            prefs[keyIsAdmin] = if (user.isAdmin) "1" else "0"
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
