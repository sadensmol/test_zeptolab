package commands

import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.EmptyChatRequest
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map


class ListChannels(chatService: ChatService) : AbstractCommand<EmptyChatRequest>("list", chatService) {

    override fun tryParse(input: String): EmptyChatRequest? {
        if (!input.startsWith("/$command")) return null
        return EmptyChatRequest
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: EmptyChatRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_UN)) {
            ctx.writeAndFlush("You need to login first!")
            return true
        }
        ctx.writeAndFlush("Available channels:")
        chatService.getAllChannels().collect{
            ctx.writeAndFlush(it.name)}
        return true
    }

}