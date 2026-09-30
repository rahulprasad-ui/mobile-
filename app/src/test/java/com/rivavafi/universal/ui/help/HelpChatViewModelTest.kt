package com.rivavafi.universal.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpChatViewModelTest {

    @Test
    fun testSendMessageReturnsSupportNumber() {
        val viewModel = HelpChatViewModel()

        // Initially there should be one welcome message
        var messages = viewModel.messages.value
        assertEquals(1, messages.size)
        assertEquals("How can we help you today?", messages[0].text)
        assertEquals(false, messages[0].isUser)

        // Send a message
        viewModel.sendMessage("I need help with my account")

        // Now there should be 3 messages: welcome, user message, bot reply
        messages = viewModel.messages.value
        assertEquals(3, messages.size)

        // Check user message
        assertEquals("I need help with my account", messages[1].text)
        assertEquals(true, messages[1].isUser)

        // Check bot reply
        assertEquals("For any query, contact 8881176909.\n\nInfo: profile page", messages[2].text)
        assertEquals(false, messages[2].isUser)
    }

    @Test
    fun testEmptyMessageIsIgnored() {
        val viewModel = HelpChatViewModel()

        viewModel.sendMessage("   ")

        val messages = viewModel.messages.value
        assertEquals(1, messages.size)
    }
}
