package me.arasple.mc.trchat

import me.arasple.mc.trchat.api.ProxyMode
import me.arasple.mc.trchat.module.internal.proxy.acceptsVelocityMessage
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.channel.PrivateChannel
import me.arasple.mc.trchat.module.display.channel.obj.ChannelBindings
import me.arasple.mc.trchat.module.display.channel.obj.ChannelEvents
import me.arasple.mc.trchat.module.display.channel.obj.ChannelSettings
import me.arasple.mc.trchat.module.internal.listener.ListenerCommand
import me.arasple.mc.trchat.util.color.CustomColor
import me.arasple.mc.trchat.util.color.MessageColors
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionAttachmentInfo
import java.lang.reflect.Proxy

fun main() {
    check(ProxyMode.REDIS.forRooms(true) == ProxyMode.VELOCITY)
    check(ProxyMode.REDIS.forRooms(false) == ProxyMode.REDIS)
    listOf(ProxyMode.NONE, ProxyMode.BUNGEE, ProxyMode.VELOCITY).forEach { mode ->
        check(mode.forRooms(true) == mode)
        check(mode.forRooms(false) == mode)
    }
    listOf("RoomState", "RoomMessage").forEach { type ->
        check(acceptsVelocityMessage(ProxyMode.REDIS, arrayOf(type)))
    }
    check(acceptsVelocityMessage(ProxyMode.REDIS, arrayOf("SendLang", "owner", "Room-Created")))
    listOf("UpdateAllNames", "BroadcastRaw", "SendPrivateRaw", "GlobalMute").forEach { type ->
        check(!acceptsVelocityMessage(ProxyMode.REDIS, arrayOf(type)))
        check(acceptsVelocityMessage(ProxyMode.VELOCITY, arrayOf(type)))
    }
    check(!acceptsVelocityMessage(ProxyMode.REDIS, emptyArray()))
    check(!acceptsVelocityMessage(ProxyMode.REDIS, arrayOf("SendLang")))
    check(!acceptsVelocityMessage(ProxyMode.REDIS, arrayOf("SendLang", "owner", "Other-Message")))
    val permissions = linkedMapOf(
        "trchat.color.c" to false,
        "trchat.color.b" to true,
        "trchat.color.rainbow" to false,
        "trchat.color.force-defaultcolor.c" to false,
        "trchat.color.force-defaultcolor.b" to true
    )
    lateinit var sender: CommandSender
    sender = Proxy.newProxyInstance(CommandSender::class.java.classLoader, arrayOf(CommandSender::class.java)) { _, method, args ->
        when (method.name) {
            "hasPermission" -> permissions[args!![0]] == true
            "getEffectivePermissions" -> permissions.map { (node, value) -> PermissionAttachmentInfo(sender, node, null, value) }.toSet()
            "recalculatePermissions" -> null
            else -> error("Unexpected CommandSender method: ${method.name}")
        }
    } as CommandSender
    check("&c" !in MessageColors.getColors(sender))
    check(MessageColors.getForceColors(sender) == listOf("&b"))
    check(MessageColors.replaceWithPermission(sender, "&cred §cred <rainbow>text") == "red red text")
    check(MessageColors.replaceWithPermission(sender, "&bblue") == "§bblue")
    check(CustomColor(CustomColor.ColorType.NORMAL, "§e").colored(sender, "&cred") == "§ered")
    check(CustomColor.get("7").colored(sender, "§cred") == "§7red")
    check(CustomColor(CustomColor.ColorType.SPECIAL, "prefix ").colored(sender, "<rainbow>text") == "prefix text")
    check(MessageColors.stripColorCodes("§cA &lB &#ff0000C <rainbow>D <g:#000000:#ffffff>E") == "A B C D E")

    permissions.clear()
    val itemColors = mapOf(
        MessageColors.Type.ANVIL to "trchat.color.anvil.*",
        MessageColors.Type.SIGN to "trchat.color.sign.*",
        MessageColors.Type.BOOK to "trchat.color.book.*"
    )
    itemColors.forEach { (type, permission) ->
        permissions.clear()
        permissions[permission] = true
        check(MessageColors.replaceWithPermission(sender, "&cred §cred", type) == "§cred §cred")
        check(MessageColors.replaceWithPermission(sender, "&cred §cred") == "red red")
        itemColors.keys.filter { it != type }.forEach { other ->
            check(MessageColors.replaceWithPermission(sender, "&cred", other) == "red")
        }
        permissions[permission] = false
        check(MessageColors.replaceWithPermission(sender, "&cred", type) == "red")
    }
    permissions.clear()
    itemColors.values.forEach { permissions[it] = true }
    check(MessageColors.replaceWithPermission(sender, listOf("&cone", "§ctwo"), MessageColors.Type.BOOK) == listOf("§cone", "§ctwo"))
    check(MessageColors.replaceWithPermission(sender, "&cchat") == "chat")
    permissions["trchat.color.*"] = true
    MessageColors.Type.values().forEach { type ->
        check(MessageColors.replaceWithPermission(sender, "&cred §cred", type) == "§cred §cred")
    }

    val private = PrivateChannel("Private", ChannelSettings(), ChannelBindings(null, listOf("msg", "tell", "w")),
        ChannelEvents(null, null, null, null), emptyList(), emptyList(), emptyList())
    Channel.channels[private.id] = private
    check(ListenerCommand.redirectPrivateCommand("/tell Alex hello world", "trchat") == "/trchat:msg Alex hello world")
    check(ListenerCommand.redirectPrivateCommand("/TELL Alex  hello", "trchat") == "/trchat:msg Alex  hello")
    check(ListenerCommand.redirectPrivateCommand("/tell", "trchat") == "/trchat:msg")
    check(ListenerCommand.redirectPrivateCommand("/minecraft:tell Alex hello", "trchat") == "/minecraft:tell Alex hello")
    Channel.channels.clear()
    check(ListenerCommand.redirectPrivateCommand("/tell Alex hello", "trchat") == "/tell Alex hello")
    listOf("r", "R", "reply", "trreply").forEach { alias ->
        check(ListenerCommand.redirectPrivateCommand("/$alias hello  world", "trchat") == "/trchat:r hello  world")
        check(ListenerCommand.redirectPrivateCommand("/$alias", "trchat") == "/trchat:r")
        check(ListenerCommand.redirectPrivateCommand("/$alias hello", "trchat", false) == "/$alias hello")
    }
    check(ListenerCommand.redirectPrivateCommand("/r 你好 世界", "trchat") == "/trchat:r 你好 世界")
    check(ListenerCommand.redirectPrivateCommand("/essentials:r hello", "trchat") == "/essentials:r hello")
    check(ListenerCommand.redirectPrivateCommand("/trchat:r hello", "trchat") == "/trchat:r hello")
    println("Chat checks passed (denied colors, item/chat permission isolation, admin colors, raw section codes, default colors, private-command routing).")
}
