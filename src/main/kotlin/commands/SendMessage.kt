package commands

import ATTRIBUTE_CN
import AbstractCommand
import ChatService
import domain.Error
import domain.SendMessageRequest
import io.netty.channel.ChannelHandlerContext


class SendMessage(chatService: ChatService) : AbstractCommand<SendMessageRequest>("", chatService) {

    override fun tryParse(input: String): Pair<SendMessageRequest?, Error?>? {
        if (input.startsWith("/")) return null
        return Pair(SendMessageRequest(input), null)
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: SendMessageRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_CN)) {
            ctx.writeAndFlush("please join any channel first!")
            return false
        }

        val chName = ctx.channel().attr(ATTRIBUTE_CN).get()
        val channel = chatService.findChannelByName(chName)


        //now we need to find al
        return true
    }

}