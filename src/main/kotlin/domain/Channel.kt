package domain

data class Channel(
    val name: String,
    private val maxUsers: Int
) {
    private val messages = mutableListOf<Message>()
    private val users = mutableListOf<User>()

    fun getLastMessages(size: Int) = messages.toList().sorted().takeLast(size)

    fun addUser(user: User): Boolean = synchronized(this) {
        if (users.size >= maxUsers) return false
        users.add(user)
        return true
    }

    fun removeUser(user: User) = synchronized(this) { users.remove(user) }
}
