package com.github.sukhinin.prometheus.outbound

data class Sample(val timestamp: Long, val name: String, val value: Double, val labels: LabelSet)

