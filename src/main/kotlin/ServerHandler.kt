import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.runBlocking

class ServerHandler(private val chatService: ChatService) : SimpleChannelInboundHandler<String>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        println("server: client ${ctx.channel().id()} connected!")
        chatService.activeNettyChannels.add(ctx.channel())
        ctx.writeAndFlush("connected!")
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        println("server: client ${ctx.channel().id()} disconnected!")
        chatService.activeNettyChannels.remove(ctx.channel())
        super.channelInactive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
        println("server: $msg")
        chatService.executeRequest(ctx, msg)
    }
}