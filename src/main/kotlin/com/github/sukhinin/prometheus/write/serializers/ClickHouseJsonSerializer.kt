package com.github.sukhinin.prometheus.write.serializers

import com.fasterxml.jackson.core.JsonFactory
import com.github.sukhinin.prometheus.write.data.LabeledSample
import org.apache.kafka.common.serialization.Serializer
import java.io.ByteArrayOutputStream

/**
 * Specialized metrics serializer with JSON output suitable for
 * importing into ClickHouse table with the following structure:
 * ```
 * CREATE TABLE test.metrics(
 *     timestamp DateTime,
 *     name String,
 *     value Float64,
 *     labels Nested(name String, value String)
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
        generator.writeStringField("name", data.name)
        generator.writeNumberField("value", data.value)

        generator.writeArrayFieldStart("labels.name")
        data.labels.forEach { label -> generator.writeString(label.name) }
        generator.writeEndArray()

        generator.writeArrayFieldStart("labels.value")
        data.labels.forEach { label -> generator.writeString(label.value) }
        generator.writeEndArray()

        generator.writeEndObject()
        generator.close()
        return stream.toByteArray()
    }
}
