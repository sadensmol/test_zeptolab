package commands

import ATTRIBUTE_CN
import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.Channel
import Configuration
import domain.JoinChatRequest
import io.netty.channel.ChannelHandlerContext



class Join(chatService: ChatService) : AbstractCommand<JoinChatRequest>("join",chatService) {

    override fun tryParse(input: String): JoinChatRequest? {
        if (!input.startsWith("/$command")) return null

        //fixme add error check here!
        val split = input.split(" ")
        return JoinChatRequest(split[1])
    }


    // todo add ok idiom
    override suspend fun process(ctx: ChannelHandlerContext, req: JoinChatRequest): Boolean {
        if (!ctx.channel().hasAttr(ATTRIBUTE_UN)) {
            ctx.channel().writeAndFlush("Please log in first!")
            return true
        }

        val userName = ctx.channel().attr(ATTRIBUTE_UN).get()
        val user = chatService.findUserByName(userName)

        if (user == null) {
            ctx.channel().writeAndFlush("Sorry some critical error occurred!!!") //fixme
            return true
        }

        val curCh = chatService.findChannelByName(req.name) ?: chatService.addChannel(Channel(name = req.name)).also{
            ctx.channel().attr(ATTRIBUTE_CN).set(req.name)
        }
                if (curCh.getNumUsers() >= Configuration.maxUsersPerChannel) ctx.writeAndFlush(
                    "This channel is full. Please try another channel "
                )
                else {
                    user.currentChannel?.removeUser(user)
                    user.currentChannel = curCh
                    curCh.addUser(user)
                    ctx.channel()
                        .writeAndFlush("Joined *${req.name}* channel.")

                    curCh.getLastMessages(Configuration.amountOfMessages).forEach {
                        ctx.writeAndFlush(it)
                    }
                }

                return true
    }

}