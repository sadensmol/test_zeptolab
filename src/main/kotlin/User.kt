import io.netty.channel.ChannelId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class User(
    val login: String,
    private val password: String,
    var currentIrcChannel: Channel? = null,
    private val connections: MutableList<ChannelId>,
) {
    private val connectionsMutex = Mutex()

    fun check(password: String) : Boolean = this.password == password

    fun getConnections() : List<ChannelId> = connections.toList()

    suspend fun addConnection(channelId: ChannelId) = connectionsMutex.withLock { connections.add(channelId) }

    suspend fun removeConnection(channelId: ChannelId) = connectionsMutex.withLock { connections.remove(channelId) }
}

