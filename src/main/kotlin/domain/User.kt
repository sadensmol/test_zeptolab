package domain

import io.netty.channel.ChannelId

data class User(
    val name: String,
    private val password: String,
) {
    @Volatile
    var currentChannel: Channel? = null

    fun check(password: String): Boolean = this.password == password
}

