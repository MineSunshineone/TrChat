package me.arasple.mc.trchat.module.display.channel

import me.arasple.mc.trchat.module.display.channel.obj.ChannelBindings
import me.arasple.mc.trchat.module.display.channel.obj.ChannelEvents
import me.arasple.mc.trchat.module.display.channel.obj.ChannelSettings
import me.arasple.mc.trchat.module.display.format.Format
import me.arasple.mc.trchat.module.internal.command.main.CommandRoom
import org.bukkit.entity.Player
import taboolib.module.chat.ComponentText

class RoomChannel(
    id: String,
    settings: ChannelSettings,
    events: ChannelEvents,
    formats: List<Format>
) : Channel(id, settings, ChannelBindings(null, null), events, formats, emptyList()) {

    override fun registerCommand() = Unit

    override fun canJoin(player: Player): Boolean = CommandRoom.isMember(player.name) && super.canJoin(player)

    override fun canListen(player: Player): Boolean = false

    override fun canSpeak(player: Player): Boolean = canJoin(player) && super.canSpeak(player)

    override fun checkLimits(player: Player, message: String): Boolean {
        return canJoin(player) && super.checkLimits(player, message)
    }

    override fun broadcast(player: Player, component: ComponentText, plain: String, toConsole: Boolean, mentioned: Set<String>) {
        CommandRoom.request(player, "chat", component.toRawMessage())
    }
}
