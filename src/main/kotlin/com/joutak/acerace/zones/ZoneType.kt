package com.joutak.acerace.zones

enum class ZoneType(val str: String) {
    CHECKPOINT("CHECKPOINT"),
    ELYTRA("ELYTRA"),
    UNDERWATER("UNDERWATER");

    override fun toString(): String {
        return str
    }
}
