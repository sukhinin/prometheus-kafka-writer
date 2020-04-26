package com.github.sukhinin.prometheus.kafka.writer.serializers

import com.fasterxml.jackson.core.JsonFactory
import com.github.sukhinin.prometheus.kafka.writer.data.LabeledSample
import com.github.sukhinin.simpleconfig.MapConfig
import com.github.sukhinin.simpleconfig.getListOrDefault
import com.github.sukhinin.simpleconfig.scoped
import org.apache.kafka.common.serialization.Serializer
import java.io.ByteArrayOutputStream

/**
 * Specialized metrics serializer with JSON output suitable for
 * importing into ClickHouse table with the following structure:
 * ```
 * CREATE TABLE test.metrics(
 *     timestamp DateTime,
 *     metric String,
 *     value Float64,
 *     tags Nested(name String, value String)
 * )
 * ENGINE = MergeTree()
 * PARTITION BY toYYYYMMDD(timestamp)
 * ORDER BY (name, timestamp)
 * ```
 */
class ClickHouseJsonSerializer : Serializer<LabeledSample> {

    private companion object {
        private const val KEY_SERIALIZER_CONFIG_SCOPE = "key.serializer"
        private const val VALUE_SERIALIZER_CONFIG_SCOPE = "value.serializer"
        private const val EXTRACT_TAGS_CONFIG_KEY = "extract.tags"
    }

    private val factory = JsonFactory()
    private val extractedTags = HashSet<String>()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {
        val scope = if (isKey) KEY_SERIALIZER_CONFIG_SCOPE else VALUE_SERIALIZER_CONFIG_SCOPE
        val config = configs.mapValues { (_, v) -> v.toString() }.let(::MapConfig).scoped(scope)
        extractedTags.addAll(config.getListOrDefault(EXTRACT_TAGS_CONFIG_KEY, emptyList()))
    }

    override fun serialize(topic: String, data: LabeledSample): ByteArray {
        val stream = ByteArrayOutputStream()
        val generator = factory.createGenerator(stream)
        generator.writeStartObject()

        generator.writeNumberField("timestamp", data.timestamp / 1000)
        generator.writeStringField("metric", data.metric)
        generator.writeNumberField("value", data.value)

        for (label in data.tags) {
            if (extractedTags.contains(label.name)) {
                generator.writeStringField(label.name, label.value)
            }
        }

        generator.writeArrayFieldStart("tags.name")
        data.tags.forEach { label -> generator.writeString(label.name) }
        generator.writeEndArray()

        generator.writeArrayFieldStart("tags.value")
        data.tags.forEach { label -> generator.writeString(label.value) }
        generator.writeEndArray()

        generator.writeEndObject()
        generator.close()
        return stream.toByteArray()
    }
}
