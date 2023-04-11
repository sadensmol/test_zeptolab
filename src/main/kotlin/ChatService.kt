import commands.Disconnect
import commands.Join
import commands.Leave
import commands.ListChannels
import commands.ListUsers
import commands.Login
import commands.SendMessage
import domain.Channel
import domain.ChatRequest
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
    fun tryParse(input: String): T?

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

    fun hasUser(userName: String): Boolean {
        return findUserByName(userName)?.let { true } ?: false
    }

    fun addUser(user: User) {
        userRepository.save(user)
    }

    fun removeUserByName(name: String) {
        userRepository.deleteById(name)
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

    fun hasUserInChannel(userName: String, chName: String): Boolean {
        return findChannelByName(chName)?.findUser(userName)?.let { true } ?: false
    }

    fun getAllChannels(): Flow<Channel> {
        return channelRepository.getAll()
    }

    fun getAllUsers(): Flow<User> {
        return userRepository.getAll()
    }

    private fun findAppropriate(input: String): Pair<ICommand<ChatRequest>, ChatRequest>? {
        commands.forEach { cmd ->
            cmd.tryParse(input)?.let { return Pair(cmd, it) }
        }

        return null
    }

    fun executeRequest(ctx: ChannelHandlerContext, msg: String) {
        findAppropriate(msg)?.let { (cmd, msg) ->
            runBlocking {
                cmd.process(ctx, msg) //fixme add proper error handling
            }
        } ?: ctx.writeAndFlush("sorry, your request is wrong")
    }
}