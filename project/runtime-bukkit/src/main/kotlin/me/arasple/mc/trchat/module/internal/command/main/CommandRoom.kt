package me.arasple.mc.trchat.module.internal.command.main

import me.arasple.mc.trchat.api.ProxyMode
import me.arasple.mc.trchat.api.impl.BukkitProxyManager
import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.channel.RoomChannel
import me.arasple.mc.trchat.util.sendComponent
import me.arasple.mc.trchat.util.session
import me.arasple.mc.trchat.util.proxy.ChatRooms
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.command.suggest
import taboolib.common.platform.function.submit
import taboolib.module.chat.Components
import taboolib.module.lang.sendLang
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@PlatformSide(Platform.BUKKIT)
object CommandRoom {

    private val localRooms = ChatRooms()
    private val members = ConcurrentHashMap<String, List<String>>()
    private val revisions = ConcurrentHashMap<String, Long>()
    private val synchronizing = ConcurrentHashMap.newKeySet<String>()

    private fun channel() = Channel.channels.values.filterIsInstance<RoomChannel>().firstOrNull()

    fun isMember(name: String): Boolean = members.containsKey(name)

    fun beginSync(player: Player) {
        if (channel() != null && BukkitProxyManager.mode == ProxyMode.VELOCITY) synchronizing += player.name
    }

    fun isSynchronizing(name: String): Boolean = name in synchronizing

    @Awake(LifeCycle.ENABLE)
    fun register() {
        if ("room" in Settings.conf.getStringList("Options.Disabled-Commands")) return
        command("room", listOf("trroom"), "TrChat private room", permission = "trchat.private.room", permissionDefault = PermissionDefault.TRUE) {
            listOf("create", "accept", "leave", "list").forEach { action ->
                literal(action) {
                    execute<Player> { sender, _, _ -> request(sender, action) }
                }
            }
            literal("invite") {
                dynamic("player") {
                    suggest { BukkitProxyManager.getPlayerNames().keys.toList() }
                    execute<Player> { sender, _, argument ->
                        val target = BukkitProxyManager.getExactName(argument)
                        if (target == null || target !in BukkitProxyManager.getPlayerNames()) sender.sendLang("Command-Player-Not-Exist")
                        else request(sender, "invite", target)
                    }
                }
            }
            literal("chat") {
                dynamic("message") {
                    execute<Player> { sender, _, argument ->
                        if (!isMember(sender.name)) sender.sendLang("Room-Not-Joined")
                        else channel()?.execute(sender, argument)
                    }
                }
            }
            execute<Player> { sender, _, _ ->
                val room = channel()
                if (room != null && isMember(sender.name)) Channel.join(sender, room)
                else sender.sendLang("Room-Help")
            }
            incorrectSender { sender, _ -> sender.sendLang("Command-Not-Player") }
            incorrectCommand { sender, _, _, _ -> sender.sendLang("Room-Help") }
        }
    }

    fun request(player: Player, action: String, argument: String = "") {
        if (channel() == null) return
        val data = arrayOf("RoomRequest", action, player.name, argument, player.uniqueId.toString())
        if ((BukkitProxyManager.mode == ProxyMode.NONE || BukkitProxyManager.mode == ProxyMode.VELOCITY)
            && (action == "create" || action == "accept" || action == "sync")) {
            synchronizing += player.name
        }
        when (BukkitProxyManager.mode) {
            ProxyMode.NONE -> localRooms.execute(data) { Bukkit.getPlayerExact(it)?.isOnline == true }.forEach { receive(it) }
            ProxyMode.VELOCITY -> BukkitProxyManager.sendMessage(player, data)
            else -> if (action != "sync") player.sendLang("Room-Unsupported-Proxy")
        }
    }

    fun forget(player: Player) {
        members.remove(player.name)
        revisions.remove(player.name)
        synchronizing.remove(player.name)
        if (BukkitProxyManager.mode == ProxyMode.NONE) request(player, "disconnect")
    }

    fun receive(data: Array<String>) {
        submit {
            val player = data.getOrNull(1)?.let { Bukkit.getPlayerExact(it) } ?: return@submit
            when (data[0]) {
                "RoomState" -> {
                    if (data.size != 5) return@submit
                    val revision = data[2].toLongOrNull() ?: return@submit
                    if (revision <= (revisions[player.name] ?: 0L)) return@submit
                    revisions[player.name] = revision
                    val wasSynchronizing = synchronizing.remove(player.name)
                    val wasMember = isMember(player.name)
                    val names = data[3].split(',').filter { it.isNotEmpty() }
                    if (player.name in names) members[player.name] = names else members.remove(player.name)
                    if (player.name in names && (data[4].toBoolean() || !wasMember || wasSynchronizing)) {
                        channel()?.let { Channel.join(player, it, hint = false) }
                    } else if (player.name !in names && player.session.getChannel() is RoomChannel) {
                        Channel.quit(player, setDefault = true, hint = false)
                    }
                }
                "RoomMessage" -> {
                    if (data.size == 4) player.sendComponent(UUID.fromString(data[2]), Components.parseRaw(data[3]))
                }
                "SendLang" -> player.sendLang(data[2], *data.drop(3).toTypedArray())
            }
        }
    }
}
