# Test task for Zeptolab

We want you to implement a simple text based chat based on Netty’s framework latest version.
Chat is a channel based communication tool, so our fancy chat implementation should support multiple channels for users communications. There is one restriction though: a user can only join one channel at a time, when they join another they leave their current channel. Moreover, the same user can auth twice from different devices, and on both them they should be able to receive messages.
ChatServer should handle the following commands:

● /login <name> <password>
If the user doesn’t exists, create profile else login, after login join to last connected channel (use join logic, if client’s limit exceeded, keep connected, but without active channel).

● /join <channel>
Try to join a channel (max 10 active clients per channel is needed). If client's limit exceeded - send error, otherwise join channel and send last N messages of activity.

● /leave
Leave current channel.

● /disconnect
Close connection to server.

● /list
Send list of channels.

● /users
Send list of unique users in current channel.

● <text message terminated with CR>
Sends message to current channel Server must send a new message to all connected to this channel.

Requirements to the implementation:

● ChatHandler - main logic handler for chat/channel communications.

● InMemory storage with no persistence implemented, but extendable to it (interface +
class implementation).

● Please pay extra attention to concurrency and thread safety.

● README.md with usage examples / commands description (can be a copy of this task
assignment).

● Use Java/Kotlin for this test assessment.

● We should be able to check this server via a simple text based telnet command.

### Code Quality:
● MVP / Proof of Concept.

● We expect you to write an explanatory letter about your solution: common thoughts,
used patterns, points of improvements or any other info you want to add to your solution but you have to cut out from implementation due to MVP/PoC restrictions.


# Performance testing
## Installing tools

```shell
go install github.com/codesenberg/bombardier@latest
```

