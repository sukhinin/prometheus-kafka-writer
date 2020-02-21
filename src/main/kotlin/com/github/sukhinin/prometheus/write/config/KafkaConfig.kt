package com.github.sukhinin.prometheus.write.config

data class KafkaConfig(val topic: String, val props: Map<String, String>)
