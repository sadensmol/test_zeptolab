import Configuration.port
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder


class Client {
    private var group: EventLoopGroup = NioEventLoopGroup()
    private var ch: Channel? = null

    fun write(message: String) {
        ch?.writeAndFlush(message)
    }

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
                    p.addLast(StringEncoder())
                    p.addLast(ClientHandler())
                }
            })

        // Start the client.
        val f = b.connect("localhost", port).awaitUninterruptibly()
        ch = f.channel()
    }

    fun isConnected(): Boolean {
        return ch?.isActive ?: false
    }

    fun getId(): ChannelId? {
        return ch?.id()
    }

    internal class ClientHandler : SimpleChannelInboundHandler<String>() {
        @Throws(java.lang.Exception::class)
        override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
            println("received: $msg")
        }
    }

}