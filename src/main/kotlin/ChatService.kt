import commands.Disconnect
import commands.Join
import commands.Leave
import commands.ListChannels
import commands.ListUsers
import commands.Login
import commands.SendMessage
import domain.Channel
import domain.ChatRequest
import domain.Error
import domain.User
import domain.repository.ICrudRepository
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.AttributeKey
import io.netty.util.concurrent.GlobalEventExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

interface ICommand<out T> {
    //todo add ok idiom or arrow.kt
    fun tryParse(input: String): Pair<T?, Error?>?

    //todo add ok idiom or arrow.kt
    suspend fun process(ctx: ChannelHandlerContext, req: @UnsafeVariance T): Boolean
}

abstract class AbstractCommand<T : ChatRequest>(protected val command: String, protected val chatService: ChatService) :
    ICommand<T> {

}

val ATTRIBUTE_UN = AttributeKey.newInstance<String>("un")
val ATTRIBUTE_CN = AttributeKey.newInstance<String>("cn")

class ChatService(
    private val userRepository: ICrudRepository<User, String>,
    private val channelRepository: ICrudRepository<Channel, String>
) {
    private val commands =
        listOf(
            Login(this),
            Join(this),
            Leave(this),
            Disconnect(this),
            ListChannels(this),
            ListUsers(this),
            SendMessage(this)
        )

    val activeNettyChannels = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    fun findUserByName(name: String): User? {
        return userRepository.findById(name)
    }

    fun registerUser(user: User) {
        userRepository.save(user)
    }

    fun findChannelByName(chName: String): Channel? {
        return channelRepository.findById(chName)
    }

    fun hasChannel(chName: String): Boolean {
        return findChannelByName(chName)?.let { true } ?: false
    }

    fun addChannel(ch: Channel): Channel {
        channelRepository.save(ch)
        return ch
    }

    fun getAllChannels(): Flow<Channel> {
        return channelRepository.getAll()
    }


    private fun findAppropriate(input: String): Triple<ICommand<ChatRequest>?, ChatRequest?, Error?>? {
        commands.forEach { cmd ->
            val p = cmd.tryParse(input)

            p?.second?.let {
                return Triple(null, null, it)
            }
            p?.first?.let {
                return Triple(cmd, it, null)
            }
        }

        return null
    }

    fun executeRequest(ctx: ChannelHandlerContext, msg: String) {
        findAppropriate(msg)?.let { (cmd, msg, err) ->

            err?.let {
                ctx.channel().writeAndFlush("error: ${err.message}")
                return
            }

            if (cmd == null || msg == null) {
                ctx.channel().writeAndFlush("critical error occurred!!!")
                return
            }

            runBlocking {
                cmd.process(ctx, msg) //fixme add proper error handling
            }
        } ?: ctx.writeAndFlush("not supported!")
    }
}