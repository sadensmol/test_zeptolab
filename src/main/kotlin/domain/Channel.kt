package domain

import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

data class Channel(
    val name: String,
) {
    private val messages = mutableListOf<Message>()
    private val users = mutableListOf<User>()

    fun getNumUsers() = users.size


    fun findUser(name: String): User? {
        return users.firstOrNull { u -> u.name == name }
    }

    fun getLastMessages(size: Int) = messages.toList().sorted().takeLast(size)

    fun addMessage(message: Message) = messages.add(message)

    fun addUser(user: User) = users.add(user)

    fun removeUser(user: User) = users.remove(user)
}
