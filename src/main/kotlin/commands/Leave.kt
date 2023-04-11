package commands

import ATTRIBUTE_CN
import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.EmptyChatRequest
import io.netty.channel.ChannelHandlerContext


class Leave(chatService: ChatService) : AbstractCommand<EmptyChatRequest>("leave", chatService) {

    override fun tryParse(input: String): EmptyChatRequest? {
        if (!input.startsWith("/$command")) return null
        return EmptyChatRequest
    }

    override suspend fun process(ctx: ChannelHandlerContext, req: EmptyChatRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_UN)) return true


        val userName = ctx.channel().attr(ATTRIBUTE_UN).get()
        val user = chatService.findUserByName(userName)

        user?.let {
            user.currentChannel = null
        }
        ctx.channel().attr(ATTRIBUTE_CN).set(null)
        ctx.channel().writeAndFlush("exited channel!")

        return true
    }

}