import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class ChatHandler(private val chatService: ChatService) : SimpleChannelInboundHandler<String>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        chatService.activeNettyChannels.add(ctx.channel())
        ctx.channel().writeAndFlush("connected!")
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        chatService.activeNettyChannels.remove(ctx.channel())
        super.channelInactive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
        chatService.executeRequest(ctx, msg)
    }
}