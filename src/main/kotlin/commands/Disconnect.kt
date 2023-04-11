package commands

import AbstractCommand
import ChatService
import domain.EmptyChatRequest
import io.netty.channel.ChannelHandlerContext



class Disconnect(chatService: ChatService) : AbstractCommand<EmptyChatRequest>("disconnect",chatService) {

    override fun tryParse(input: String): EmptyChatRequest? {
        if (!input.startsWith("/$command")) return null
        return EmptyChatRequest
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: EmptyChatRequest): Boolean {
        ctx.disconnect()
        return true
    }

}