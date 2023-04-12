package commands

import ATTRIBUTE_UN
import AbstractCommand
import ChatService
import domain.Error
import domain.LoginChatRequest
import domain.ParseError
import domain.User
import io.netty.channel.ChannelHandlerContext


class Login(chatService: ChatService) : AbstractCommand<LoginChatRequest>("login", chatService) {

    override fun tryParse(input: String): Pair<LoginChatRequest?, Error?>? {
        if (!input.startsWith("/$command")) return null

        //todo add error check here!
        val split = input.split(" ")

        if (split.size < 3) return Pair(null, ParseError("use /login <user_name> <password>"))

        return Pair(LoginChatRequest(split[1], split[2]), null)
    }


    override suspend fun process(ctx: ChannelHandlerContext, req: LoginChatRequest): Boolean {
        ctx.channel().attr(ATTRIBUTE_UN).set(req.name)

        val possibleUser = chatService.findUserByName(req.name)
        if (possibleUser != null) {
            if (possibleUser.check(req.password)) {
                ctx.channel().writeAndFlush("welcome again, ${req.name}!")
                possibleUser.currentChannel?.let {
                    chatService.executeRequest(ctx, "/join ${it.name}")
                }
            } else ctx.channel().writeAndFlush("wrong password for ${req.name}! ")
        } else {
            chatService.registerUser(User(name = req.name, password = req.password))
            ctx.channel().writeAndFlush("user ${req.name} logged in!")
        }

        return true
    }

}