import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.Delimiters
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.StringDecoder


object Configuration {
    const val port = 8080
    const val amountOfMessages = 10
    const val maxUsersPerChannel = 10
}

class Server(private val chatService: ChatService) {
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
                            pipeline.addLast(LineEncoder())
                            pipeline.addLast("framer", DelimiterBasedFrameDecoder(8192, * Delimiters.lineDelimiter()))
                            pipeline.addLast(StringDecoder())
                            pipeline.addLast(ChatHandler(chatService))
                        }
                    })

            val f = serverBootstrap.bind(Configuration.port).awaitUninterruptibly()
            ch = f.channel()
        } catch (e: Throwable) {
            boss.shutdownGracefully()
            workers.shutdownGracefully()
        }
    }
}


