package commands

import ATTRIBUTE_CN
import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.Error
import domain.Message
import domain.SendMessageRequest
import io.netty.channel.ChannelHandlerContext


class SendMessage(chatService: ChatService) : AbstractCommand<SendMessageRequest>("", chatService) {

    override fun tryParse(input: String): Pair<SendMessageRequest?, Error?>? {
        if (input.startsWith("/")) return null

        if (input.trim().isEmpty()) return null

        return Pair(SendMessageRequest(input), null)
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: SendMessageRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_UN)) {
            ctx.channel().writeAndFlush("please login first!")
            return false
        }

        if (!ctx.channel().hasAttr(ATTRIBUTE_CN)) {
            ctx.channel().writeAndFlush("please join any channel first!")
            return false
        }

        val uName = ctx.channel().attr(ATTRIBUTE_UN).get()
        val chName = ctx.channel().attr(ATTRIBUTE_CN).get()
        val user = chatService.findUserByName(uName)
        val channel = chatService.findChannelByName(chName)


        if (user == null || channel == null) {
            ctx.channel().writeAndFlush("critical error occurred!!!")
            return false
        }

        val msg = Message(sentBy = user, content = req.message, )
        channel.addMessage(msg)

        chatService.activeNettyChannels.forEach {
            if (it == ctx.channel()) return@forEach
            if (it.hasAttr(ATTRIBUTE_CN) && it.attr(ATTRIBUTE_CN).get() == chName) {
                it.writeAndFlush(msg.toString())
            }
        }

        return true
    }

}