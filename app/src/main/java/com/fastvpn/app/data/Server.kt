package com.fastvpn.app.data

/**
 * One VPN server entry (maps to one of your VPS boxes -- add as many as you want,
 * no limit).
 *
 * endpoint       -> "your.vps.ip.address:51820"
 * serverPublicKey-> WireGuard public key of the VPS (from `wg show` on the server)
 * clientAddress  -> the server-side client subnet, e.g. "10.8.0.0/24".
 *                    The actual per-device address is allocated atomically by the
 *                    backend API and returned during registration.
 * presharedKey   -> optional extra layer, can be blank
 * dns            -> DNS to use inside tunnel, e.g. "1.1.1.1"
 */
data class Server(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val countryName: String = "",
    val countryCode: String = "US",   // ISO 3166-1 alpha-2, used to render flag emoji
    val city: String = "",
    val endpointHost: String = "",
    val endpointPort: Int = 51820,
    val serverPublicKey: String = "",
    val presharedKey: String = "",
    val clientAddress: String = "10.8.0.0/24",
    val dns: String = "1.1.1.1",
    val maxRecommendedUsers: Int = 40, // rough capacity hint for a 1GB RAM VPS
    val enabled: Boolean = true,

    // runtime-only fields (not persisted, filled in at runtime)
    @Transient var pingMs: Int = -1,   // -1 = not tested yet, -2 = unreachable
    @Transient var isConnecting: Boolean = false
) {
    val endpoint: String get() = "$endpointHost:$endpointPort"

    fun flagEmoji(): String {
        if (countryCode.length != 2) return "🌐"
        val base = 0x1F1E6
        val first = Character.codePointAt(countryCode.uppercase(), 0) - 'A'.code + base
        val second = Character.codePointAt(countryCode.uppercase(), 1) - 'A'.code + base
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}
