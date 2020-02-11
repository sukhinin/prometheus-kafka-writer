package com.github.sukhinin.prometheus.config

import com.github.sukhinin.simpleconfig.scoped

object ConfigMapper {
    fun from(cfg: com.github.sukhinin.simpleconfig.Config): Config {
        val serverConfig = ServerConfigMapper.from(cfg.scoped("server"))
        val kafkaConfig = KafkaConfigMapper.from(cfg.scoped("kafka"))
        return Config(serverConfig, kafkaConfig)
    }
}
