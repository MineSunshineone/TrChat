package me.arasple.mc.trchat.util.proxy

import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

fun main() {
    val rooms = ChatRooms()
    val uuid = UUID.randomUUID().toString()
    fun request(action: String, player: String, argument: String = "") =
        rooms.execute(arrayOf("RoomRequest", action, player, argument, uuid)) { true }
    fun hasHint(result: List<Array<String>>, node: String) = result.any { it[0] == "SendLang" && it[2] == "Room-$node" }
    fun membership(player: String) = request("sync", player).single()[3].split(',').filter { it.isNotEmpty() }

    check(hasHint(request("accept", "stranger"), "No-Invitation"))
    request("create", "owner")
    check(hasHint(request("create", "owner"), "Already-Joined"))
    check(request("chat", "stranger", "secret").none { it[0] == "RoomMessage" })
    (1..12).forEach { request("invite", "owner", "member$it") }
    val start = CountDownLatch(1)
    val threads = (1..12).map { i -> thread { start.await(); request("accept", "member$i") } }
    start.countDown()
    threads.forEach { it.join() }
    val members = membership("owner")
    check(members.size == 8) { "Concurrent accepts exceeded room capacity: $members" }
    check(hasHint(request("invite", "owner", "extra"), "Full"))
    val chat = request("chat", "owner", "secret")
    check(chat.map { it[1] }.toSet() == members.toSet())
    check(chat.all { it[0] == "RoomMessage" && it[2] == uuid && it[3] == "secret" })

    request("create", "otherOwner")
    request("invite", "otherOwner", "outsider")
    request("accept", "outsider")
    check("outsider" !in membership("owner"))
    check(request("chat", "owner", "secret").none { it[1] == "outsider" })
    check(hasHint(request("invite", "otherOwner", "owner"), "Player-Joined"))

    request("leave", "owner")
    check(membership("owner").isEmpty())
    val successor = membership(members[1]).first()
    check(hasHint(request("invite", successor, "replacement"), "Invited"))
    request("accept", "replacement")
    check(membership(successor).size == 8)
    check(hasHint(request("leave", "owner"), "Not-Joined"))
    membership(successor).forEach { request("disconnect", it) }
    check(membership(successor).isEmpty())

    request("create", "temporary")
    request("invite", "temporary", "late")
    request("disconnect", "temporary")
    check(hasHint(request("accept", "late"), "No-Invitation"))
    check(rooms.execute(arrayOf("RoomRequest")) { true }.isEmpty())
    println("ChatRooms checks passed (concurrent capacity, invitations, isolation, switching, leave/disconnect).")
}
