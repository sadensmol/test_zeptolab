package commands

import ATTRIBUTE_CN
import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.EmptyChatRequest
import domain.Error
import io.netty.channel.ChannelHandlerContext


class ListUsers(chatService: ChatService) : AbstractCommand<EmptyChatRequest>("users", chatService) {

    override fun tryParse(input: String): Pair<EmptyChatRequest?,Error?>? {
        if (!input.startsWith("/$command")) return null
        return Pair( EmptyChatRequest,null)
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: EmptyChatRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_CN)) {
            ctx.writeAndFlush("please join any channel first!")
            return false
        }

        val chName = ctx.channel().attr(ATTRIBUTE_CN).get()

        ctx.writeAndFlush("available users:")

        chatService.activeNettyChannels.mapNotNull {
            if (it.hasAttr(ATTRIBUTE_UN) && it.hasAttr(ATTRIBUTE_CN) && it.attr(ATTRIBUTE_CN).get() == chName) {
                it.attr(ATTRIBUTE_UN).get()
            }else{
                null
            }
        }.toSet().sorted().forEach {
            ctx.writeAndFlush(it)
        }
        return true
    }

}