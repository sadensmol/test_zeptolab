package domain

import java.time.LocalDateTime

enum class Messages(val message: String) {
    LOGIN("/login"),
    JOIN("/join"),
    LEAVE("/leave"),
    DISCONNECT("/disconnect"),
    CHANNELS("/list"),
    USERS("/users")
}
sealed class Message(
    val date: LocalDateTime = LocalDateTime.now(),
    val sentBy: User,
    val content: String
) : Comparable<Message> {
    override fun compareTo(other: Message): Int {
        return if (this.date.isEqual(other.date)) 0
        else if (this.date.isAfter(other.date)) 1
        else -1
    }

    override fun toString(): String = "${sentBy.name}: $content at $date"
}
