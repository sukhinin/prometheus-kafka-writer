package com.github.sukhinin.prometheus.kafka.writer.config

import com.github.sukhinin.simpleconfig.scoped

object KafkaConfigMapper {
    fun from(cfg: com.github.sukhinin.simpleconfig.Config): KafkaConfig {
        val topic = cfg.get("topic")
        val props = cfg.scoped("props").toMap()
        return KafkaConfig(topic, props)
    }
}
