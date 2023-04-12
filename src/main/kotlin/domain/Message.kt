package domain

import java.time.LocalDateTime

class Message(
    private val date: LocalDateTime = LocalDateTime.now(),
    private val sentBy: User,
    private val content: String
) : Comparable<Message> {
    override fun compareTo(other: Message): Int {
        return if (this.date.isEqual(other.date)) 0
        else if (this.date.isAfter(other.date)) 1
        else -1
    }

    override fun toString(): String = "${sentBy.name}: $content"
}
