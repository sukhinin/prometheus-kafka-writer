package com.github.sukhinin.prometheus.config

data class KafkaConfig(val topic: String, val props: Map<String, String>)
