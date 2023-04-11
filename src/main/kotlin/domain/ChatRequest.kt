package domain

sealed class ChatRequest {}

object EmptyChatRequest : ChatRequest()

data class LoginChatRequest(val name: String, val password: String) : ChatRequest()
data class JoinChatRequest(val name: String) : ChatRequest()
data class SendMessageRequest(val message: String) : ChatRequest()