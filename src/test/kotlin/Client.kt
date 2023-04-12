import Configuration.port
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder


class Client {
    private var group: EventLoopGroup = NioEventLoopGroup()
    private var ch: Channel? = null
    private val respCache = StringBuilder()

    fun write(message: String) {
        ch?.writeAndFlush(message)
    }
    fun getRespCache() = respCache.toString()

    fun stop() {
        try {
            ch?.close()
            ch?.closeFuture()?.awaitUninterruptibly()
        } finally {
            group.shutdownGracefully()
        }
    }

    fun start() {
        val b = Bootstrap()
        b.group(group)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    val p: ChannelPipeline = ch.pipeline()
                    p.addLast(StringDecoder())
                    p.addLast(LineEncoder())
                    p.addLast(ClientHandler(respCache))
                }
            })

        // Start the client.
        val f = b.connect("localhost", port).awaitUninterruptibly()
        ch = f.channel()
    }

    fun isConnected(): Boolean {
        return ch?.isActive ?: false
    }


    internal class ClientHandler(private var respCache: StringBuilder) : SimpleChannelInboundHandler<String>() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            respCache.append("connected!")
            super.channelActive(ctx)
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            respCache.append("disconnected!")
            super.channelInactive(ctx)
        }

        @Throws(java.lang.Exception::class)
        override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            respCache.append(msg)
        }
    }

}