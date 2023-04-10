import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelId
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder

object Configuration {
    const val port = 8080
    const val amountOfMessages = 10
    const val maxUsersPerChannel = 10
}

class Server {
    private val processor = MessageProcessor()

    private val boss = NioEventLoopGroup()
    private val workers = NioEventLoopGroup()
    private var ch: Channel? = null
    fun stop() {
        try {
            ch?.close()?.awaitUninterruptibly()
        } finally {
            boss.shutdownGracefully()
            workers.shutdownGracefully()
        }
    }

    fun start() {
        try {
            val serverBootstrap = ServerBootstrap().group(boss, workers)
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        override fun initChannel(ch: SocketChannel) {
                            val pipeline: ChannelPipeline = ch.pipeline()
                            pipeline.addLast(StringDecoder())
                            pipeline.addLast(StringEncoder())
                            pipeline.addLast(ChatHandler(channelProcessingService = processor))
                        }
                    })

            val f = serverBootstrap.bind(Configuration.port).awaitUninterruptibly()
            ch = f.channel()
        } catch (e: Throwable) {
            boss.shutdownGracefully()
            workers.shutdownGracefully()
        }
    }

    suspend fun hasUser(userName: String): Boolean {
        return processor.hasUser(userName)
    }
}


fun main() {
    Server().start()
}