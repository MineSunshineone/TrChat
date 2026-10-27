package me.arasple.mc.trchat.api

enum class ProxyMode {

    NONE, REDIS, BUNGEE, VELOCITY;

    fun forRooms(velocityEnabled: Boolean): ProxyMode =
        if (this == REDIS && velocityEnabled) VELOCITY else this
}
