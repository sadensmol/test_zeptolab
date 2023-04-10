import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MessageProcessor {
    private val currentUsers = mutableListOf<User>()
    private val usersMutex = Mutex()

    private val currentIrcChannels = mutableListOf<Channel>()
    private val ircChannelsMutex = Mutex()

    private val currentNettyChannels = mutableListOf<io.netty.channel.Channel>()
    private val chMutex = Mutex()

    suspend fun onChannelActive(context: ChannelHandlerContext) = withContext(context = Dispatchers.IO) {
        chMutex.withLock {
            currentNettyChannels.add(context.channel())
        }
    }

    suspend fun onChannelInactive(context: ChannelHandlerContext) = withContext(context = Dispatchers.IO) {
        chMutex.withLock {
            currentNettyChannels.remove(context.channel())
        }
    }

    suspend fun processIncomingMessage(context: ChannelHandlerContext, message: String) =
        withContext(Dispatchers.IO) {
            when (message.substringBefore(delimiter = ' ')) {
                Messages.LOGIN.message -> processLogin(context = context, message = message)
                Messages.JOIN.message -> processJoin(context = context, message = message)
                Messages.LEAVE.message -> processLeave(context = context)
                Messages.USERS.message -> processUsers(context = context)
                Messages.DISCONNECT.message -> processDisconnect(context = context)
                else -> processMessage(context = context, message = message)
            }
        }

    private suspend fun processLogin(context: ChannelHandlerContext, message: String) {
        val splitted = message.split(' ')
        if (splitted.size == 3) {
            val (_, userName, password) = splitted
            val login = userName
            val possibleUser = currentUsers.find { it.login == login }
            if (possibleUser != null) {
                if (possibleUser.check(password)) {
                    possibleUser.addConnection(channelId = context.channel().id())
                    context.channel().writeAndFlush("Welcome again, $userName! ${System.lineSeparator()}")
                    possibleUser.currentIrcChannel?.let {
                        context.channel().writeAndFlush(
                            "Last messages from channel *${it.name}*: ${System.lineSeparator()}${
                                it.getLastMessages(Configuration.amountOfMessages)
                                    .joinToString(System.lineSeparator()) + System.lineSeparator()
                            }"
                        )
                    }
                } else context.channel().writeAndFlush("Wrong password for $userName! ${System.lineSeparator()}")
            } else {
                usersMutex.withLock {
                    currentUsers.add(
                        User(
                            login = login,
                            password = password,
                            connections = mutableListOf(context.channel().id())
                        )
                    )
                }
                context.channel().writeAndFlush("Your profile has been created, $userName ${System.lineSeparator()}")
            }
        } else context.channel()
            .writeAndFlush("Login message is not compliant with format /login <name> <password>. Please, try again with correct format ${System.lineSeparator()}")
    }

    private suspend fun processJoin(context: ChannelHandlerContext, message: String) {
        val splitted = message.split(' ')
        if (splitted.size == 2) {
            val channelName = splitted[1]
            val user = currentUsers.find { it.getConnections().contains(context.channel().id()) }
            if (user != null) {
                if (user.currentIrcChannel?.name == channelName) context.writeAndFlush("You are already in this channel ${System.lineSeparator()}")
                else {
                    val currentIrcChannel =
                        currentIrcChannels.find { it.name == channelName } ?: createChannel(name = channelName)
                    if (currentIrcChannel.getConnectedUsers().size == Configuration.maxUsersPerChannel) context.writeAndFlush(
                        "This channel is full. Please try another channel ${System.lineSeparator()}"
                    )
                    else {
                        user.currentIrcChannel?.disconnect(user)
                        user.currentIrcChannel = currentIrcChannel
                        currentIrcChannel.connect(user)
                        context.channel()
                            .writeAndFlush("Entered the *$channelName* channel. Last messages: ${System.lineSeparator()}")
                        val userNettyChannels = currentNettyChannels.toList()
                            .filter { channel -> user.getConnections().any { it == channel.id() } }
                        userNettyChannels.asFlow().collect {
                            it.writeAndFlush(
                                currentIrcChannel.getLastMessages(Configuration.amountOfMessages)
                                    .joinToString(System.lineSeparator()) + System.lineSeparator()
                            )
                        }
                    }
                }
            } else context.channel()
                .writeAndFlush("This command is not available for not logged in users. Please, login by using /login <name> <password> command ${System.lineSeparator()}")

        } else context.channel()
            .writeAndFlush("Join message is not compliant with format /join <channel>. Please, try again with correct format ${System.lineSeparator()}")
    }

    private suspend fun createChannel(name: String): Channel {
        ircChannelsMutex.withLock {
            val channel = Channel(name = name)
            currentIrcChannels.add(channel)
            return channel
        }
    }

    private suspend fun processLeave(context: ChannelHandlerContext) {
        val currentUser = currentUsers.find { it.getConnections().contains(context.channel().id()) }
        currentUser?.let {
            currentUser.removeConnection(context.channel().id())
            currentUser.currentIrcChannel?.disconnect(currentUser)
            if (currentUser.getConnections().count() == 0) usersMutex.withLock { currentUsers.remove(currentUser) }
        }
        context.channel().writeAndFlush("Bye for now!")
        context.close()
    }

    private fun processUsers(context: ChannelHandlerContext) {
        val currentUser = currentUsers.find { it.getConnections().contains(context.channel().id()) }
        if (currentUser != null) {
            if (currentUser.currentIrcChannel != null) {
                val usersInChannel =
                    currentUser.currentIrcChannel?.getConnectedUsers()?.joinToString("; ") { it.login }
                context.channel()
                    .writeAndFlush("Channel ${currentUser.currentIrcChannel?.name} has next users: $usersInChannel ${System.lineSeparator()}")
            } else context.channel()
                .writeAndFlush("You can use this command after joining a channel ${System.lineSeparator()}")
        } else context.channel()
            .writeAndFlush("This command is not available for not logged in users. Please, login by using /login <name> <password> command ${System.lineSeparator()}")
    }

    private fun processDisconnect(context: ChannelHandlerContext) {
        context.disconnect()
    }

    private suspend fun processMessage(context: ChannelHandlerContext, message: String) {
        val currentUser = currentUsers.find { it.getConnections().contains(context.channel().id()) }
        if (currentUser != null) {
            if (currentUser.currentIrcChannel != null) {
                val content = Message(sentBy = currentUser, content = message)
                currentUser.currentIrcChannel!!.send(message = content)
                currentUser.currentIrcChannel?.getConnectedUsers()?.asFlow()?.collect { user ->
                    user.getConnections().filter { it != context.channel().id() }.asFlow().collect { channelId ->
                        currentNettyChannels.single { it.id() == channelId }
                            .writeAndFlush("$content ${System.lineSeparator()}")
                    }
                }
            } else context.channel()
                .writeAndFlush("You can use this command after joining a channel ${System.lineSeparator()}")
        } else context.channel()
            .writeAndFlush("This command is not available for not logged in users. Please, login by using /login <name> <password> command ${System.lineSeparator()}")
    }

    suspend fun hasUser(userName: String):Boolean {
        usersMutex.withLock {
            return currentUsers.firstOrNull { u->u.login == userName }?.let{true}?:false
        }
    }

}