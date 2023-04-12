package commands

import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.EmptyChatRequest
import domain.Error
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet


class ListChannels(chatService: ChatService) : AbstractCommand<EmptyChatRequest>("list", chatService) {

    override fun tryParse(input: String): Pair<EmptyChatRequest?,Error?>? {
        if (!input.startsWith("/$command")) return null
        return Pair(EmptyChatRequest,null)
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: EmptyChatRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_UN)) {
            ctx.channel().writeAndFlush("please login first!")
            return false
        }
        ctx.channel().writeAndFlush("available channels:")
        chatService.getAllChannels().map { it.name }.toSet().sorted().forEach {
            ctx.channel().writeAndFlush(it)
        }
        return true
    }

}