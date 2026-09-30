package com.rivavafi.universal.ui.help

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(val id: String, val text: String, val isUser: Boolean)

@HiltViewModel
class HelpChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(UUID.randomUUID().toString(), "How can we help you today?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String, userProfileInfo: String = "profile page") {
        if (text.isBlank()) return

        val userMsg = ChatMessage(UUID.randomUUID().toString(), text, true)
        val botMsg = ChatMessage(UUID.randomUUID().toString(), "For any query, contact 8881176909.\n\nInfo: $userProfileInfo", false)

        _messages.value = _messages.value + listOf(userMsg, botMsg)
    }
}
