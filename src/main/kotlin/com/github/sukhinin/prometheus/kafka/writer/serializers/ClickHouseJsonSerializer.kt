package com.github.sukhinin.prometheus.kafka.writer.serializers

import com.fasterxml.jackson.core.JsonFactory
import com.github.sukhinin.prometheus.kafka.writer.data.LabeledSample
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

    private val factory = JsonFactory()

    override fun serialize(topic: String, data: LabeledSample): ByteArray {
        val stream = ByteArrayOutputStream()
        val generator = factory.createGenerator(stream)
        generator.writeStartObject()

        generator.writeNumberField("timestamp", data.timestamp)
        generator.writeStringField("metric", data.metric)
        generator.writeNumberField("value", data.value)

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
