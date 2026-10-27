package me.arasple.mc.trchat.util.proxy

import java.util.UUID

/** One authority handles joins so simultaneous accepts cannot exceed eight members. */
class ChatRooms {

    private val rooms = linkedMapOf<String, MutableSet<String>>()
    private val invitations = mutableMapOf<String, Invitation>()
    private var revision = 0L

    private class Invitation(val room: String, val expires: Long)

    @Synchronized
    fun execute(data: Array<String>, online: (String) -> Boolean): List<Array<String>> {
        if (data.size < 3 || data[0] != "RoomRequest") return emptyList()
        val action = data[1]
        val player = data[2]
        val argument = data.getOrElse(3) { "" }
        val result = mutableListOf<Array<String>>()
        revision = maxOf(revision + 1, System.currentTimeMillis())
        invitations.entries.removeAll { it.value.expires <= System.currentTimeMillis() || it.value.room !in rooms }
        fun roomOf(name: String) = rooms.entries.firstOrNull { name in it.value }?.key
        fun hint(target: String, node: String, vararg args: String) {
            result += arrayOf("SendLang", target, "Room-$node", *args)
        }
        fun state(target: String, activate: Boolean = false) {
            val members = roomOf(target)?.let { rooms[it] }?.joinToString(",") ?: ""
            result += arrayOf("RoomState", target, revision.toString(), members, activate.toString())
        }
        val room = roomOf(player)
        when (action) {
            "create" -> {
                if (room != null) hint(player, "Already-Joined") else {
                    rooms[UUID.randomUUID().toString()] = linkedSetOf(player)
                    state(player, true)
                    hint(player, "Created")
                }
            }
            "invite" -> {
                when {
                    room == null -> hint(player, "Not-Joined")
                    !online(argument) || argument == player -> hint(player, "Player-Unavailable")
                    roomOf(argument) != null -> hint(player, "Player-Joined")
                    rooms.getValue(room).size >= 8 -> hint(player, "Full")
                    else -> {
                        invitations[argument] = Invitation(room, System.currentTimeMillis() + 60_000)
                        hint(argument, "Invitation", player)
                        hint(player, "Invited", argument)
                    }
                }
            }
            "accept" -> {
                val invite = invitations[player]
                when {
                    room != null -> hint(player, "Already-Joined")
                    invite == null -> hint(player, "No-Invitation")
                    rooms.getValue(invite.room).size >= 8 -> hint(player, "Full")
                    else -> {
                        val members = rooms.getValue(invite.room)
                        members += player
                        invitations.remove(player)
                        members.forEach { state(it, it == player) }
                        members.forEach { hint(it, "Joined", player) }
                    }
                }
            }
            "leave", "disconnect" -> {
                invitations.remove(player)
                if (room == null) {
                    if (action == "leave") hint(player, "Not-Joined")
                } else {
                    val members = rooms.getValue(room)
                    members -= player
                    if (members.isEmpty()) {
                        rooms.remove(room)
                        invitations.entries.removeAll { it.value.room == room }
                    }
                    state(player, true)
                    members.forEach { state(it) }
                    members.forEach { hint(it, "Left", player) }
                    if (action == "leave") hint(player, "Left", player)
                }
            }
            "sync", "list" -> {
                state(player, action == "sync")
                if (action == "list") {
                    if (room == null) hint(player, "Not-Joined")
                    else hint(player, "Members", rooms.getValue(room).joinToString(", "))
                }
            }
            "chat" -> {
                if (room == null) {
                    state(player, true)
                    hint(player, "Not-Joined")
                } else if (data.size == 5 && argument.length <= 65536 && runCatching { UUID.fromString(data[4]) }.isSuccess) {
                    rooms.getValue(room).forEach { result += arrayOf("RoomMessage", it, data[4], argument) }
                }
            }
        }
        if ((action == "create" || action == "accept") && result.none { it[0] == "RoomState" && it[1] == player }) {
            state(player)
        }
        return result
    }
}
