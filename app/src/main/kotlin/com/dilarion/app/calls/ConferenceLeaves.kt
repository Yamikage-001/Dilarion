package com.dilarion.app.calls

import com.dilarion.app.data.api.ApiService
import com.dilarion.app.security.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tells the server someone left a conference.
 *
 * The call screen's ViewModel is cleared as soon as the user hangs up, which
 * cancels a normal viewModelScope launch before the request is sent. Everyone
 * still on the call then keeps showing the person who left. This scope is not
 * tied to that screen.
 */
private val leaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun notifyConferenceLeft(sessionManager: SessionManager, apiService: ApiService, conferenceId: Int) {
    if (conferenceId <= 0) return
    leaveScope.launch {
        val token = sessionManager.sessionToken.first() ?: return@launch
        runCatching { apiService.conferenceLeave("Bearer $token", conferenceId) }
    }
}
