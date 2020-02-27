package com.github.sukhinin.prometheus.kafka.writer.config

object ServerConfigMapper {
    fun from(cfg: com.github.sukhinin.simpleconfig.Config): ServerConfig {
        val port = cfg.getInteger("port")
        return ServerConfig(port)
    }
}
