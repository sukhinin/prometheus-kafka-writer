package com.github.sukhinin.prometheus.kafka.writer.config

data class KafkaConfig(val topic: String, val props: Map<String, String>)
