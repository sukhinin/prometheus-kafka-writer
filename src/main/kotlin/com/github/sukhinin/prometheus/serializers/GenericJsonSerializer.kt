package com.github.sukhinin.prometheus.serializers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.sukhinin.prometheus.data.LabeledSample
import org.apache.kafka.common.serialization.Serializer

/**
 * Generic metrics serializer with JSON output.
 */
class GenericJsonSerializer : Serializer<LabeledSample> {

    private val writer = ObjectMapper().writerFor(LabeledSample::class.java)

    override fun serialize(topic: String, data: LabeledSample): ByteArray {
        return writer.writeValueAsBytes(data)
    }
}
