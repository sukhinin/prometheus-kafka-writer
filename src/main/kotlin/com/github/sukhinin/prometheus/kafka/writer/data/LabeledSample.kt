package com.github.sukhinin.prometheus.kafka.writer.data

data class LabeledSample(val timestamp: Long, val metric: String, val value: Double, val tags: Collection<Label>)
