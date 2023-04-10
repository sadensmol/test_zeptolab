import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ChatHandler(
    private val channelProcessingService: MessageProcessor
) : SimpleChannelInboundHandler<String>() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Channel ${ctx.channel().id()} connected!")
        ctx.writeAndFlush("Connected!")
        runBlocking {
            launch {
                channelProcessingService.onChannelActive(context = ctx)
            }
        }
        super.channelActive(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        println("Channel ${ctx.channel().id()} disconnected!")
        runBlocking {
            launch {
                channelProcessingService.onChannelInactive(context = ctx)
            }
        }
        super.channelInactive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: String) {
        println("server: $msg")
        runBlocking {
            launch {
                channelProcessingService.processIncomingMessage(context = ctx, message = msg)
            }
        }
    }
}