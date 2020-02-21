package com.github.sukhinin.prometheus.data

data class LabeledSample(val timestamp: Long, val name: String, val value: Double, val labels: Collection<Label>)
