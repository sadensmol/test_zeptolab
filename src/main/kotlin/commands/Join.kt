package commands

import ATTRIBUTE_CN
import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import Configuration
import domain.Channel
import domain.Error
import domain.JoinChatRequest
import io.netty.channel.ChannelHandlerContext


class Join(chatService: ChatService) : AbstractCommand<JoinChatRequest>("join", chatService) {

    override fun tryParse(input: String): Pair<JoinChatRequest?,Error?>? {
        if (!input.startsWith("/$command")) return null

        //fixme add error check here!
        val split = input.split(" ")
        return Pair(JoinChatRequest(split[1]),null)
    }


    // todo add ok idiom
    override suspend fun process(ctx: ChannelHandlerContext, req: JoinChatRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_UN)) {
            ctx.channel().writeAndFlush("please log in first!")
            return false
        }

        val userName = ctx.channel().attr(ATTRIBUTE_UN).get()
        val user = chatService.findUserByName(userName)

        if (user == null) {
            ctx.channel().writeAndFlush("critical error occurred!!!") //fixme
            return false
        }

        ctx.channel().attr(ATTRIBUTE_CN).set(req.name)

        val curCh = chatService.findChannelByName(req.name) ?: chatService.addChannel(
            Channel(
                name = req.name,
                maxUsers = Configuration.maxUsersPerChannel
            )
        )
        user.currentChannel?.removeUser(user)
        user.currentChannel = curCh
        if (!curCh.addUser(user)) { //max users limit reached
            ctx.writeAndFlush("channel is full!")
            return false
        }
        ctx.channel().writeAndFlush("joined ${req.name} channel!")

        println("user $userName joined channel ${curCh.name}")

        curCh.getLastMessages(Configuration.amountOfMessages).forEach {
            ctx.writeAndFlush(it)
        }

        return true
    }

}