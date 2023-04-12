package commands

import AbstractCommand
import ChatService
import domain.EmptyChatRequest
import domain.Error
import io.netty.channel.ChannelHandlerContext



class Disconnect(chatService: ChatService) : AbstractCommand<EmptyChatRequest>("disconnect",chatService) {

    override fun tryParse(input: String): Pair<EmptyChatRequest?,Error?>? {
        if (!input.startsWith("/$command")) return null
        return Pair(EmptyChatRequest, null)
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: EmptyChatRequest): Boolean {
        ctx.disconnect()
        return true
    }

}