package com.axios.focusguard.domain.model

data class TimerPreset(
    val id: String,
    val name: String,
    val focusTimeMin: Int,
    val breakTimeMin: Int,
    val rounds: Int,
    val isDefault: Boolean = false,
    val isInfo: Boolean = false,
    val infoDescription: String? = null
)
