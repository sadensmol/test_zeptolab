package commands

import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import Configuration
import domain.JoinChatRequest
import domain.User
import domain.LoginChatRequest
import io.netty.channel.ChannelHandlerContext



class Login(chatService: ChatService) : AbstractCommand<LoginChatRequest>("login", chatService) {

    override fun tryParse(input: String): LoginChatRequest? {
        if (!input.startsWith("/$command")) return null

        //todo add error check here!
        val split = input.split(" ")



        return LoginChatRequest(split[1], split[2])
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: LoginChatRequest): Boolean {
        val possibleUser = chatService.findUserByName (req.name)
        ctx.channel().attr(ATTRIBUTE_UN).set(req.name)

        if (possibleUser != null) {
            if (possibleUser.check(req.password)) {
                ctx.channel().writeAndFlush("Welcome again, ${req.name}! ")
                possibleUser.currentChannel?.let {
                    chatService.executeRequest(ctx, "/join ${it.name}")
                }
            } else ctx.channel().writeAndFlush("Wrong password for ${req.name}! ")
        } else {
            chatService.addUser(
                User(
                    name = req.name,
                    password = req.password,
                )
            )

            ctx.channel().writeAndFlush("user ${req.name} logged in!")
        }


        return true
    }

}