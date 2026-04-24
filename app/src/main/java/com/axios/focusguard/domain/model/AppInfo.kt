package com.axios.focusguard.domain.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSelected: Boolean = false
)
