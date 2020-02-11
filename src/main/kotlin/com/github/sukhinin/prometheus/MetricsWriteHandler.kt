package com.github.sukhinin.prometheus

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.sukhinin.prometheus.inbound.Label
import com.github.sukhinin.prometheus.outbound.Sample
import com.github.sukhinin.prometheus.inbound.WriteRequest
import com.github.sukhinin.prometheus.outbound.LabelSet
import io.javalin.http.Context
import io.javalin.http.Handler
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.xerial.snappy.Snappy

class MetricsWriteHandler(private val producer: Producer<Nothing, ByteArray>, private val topic: String) : Handler {

    private val reader = createObjectReader()
    private val writer = createObjectWriter()

    private fun createObjectReader(): ObjectReader {
        val inboundMapper = ProtobufMapper().registerModule(KotlinModule()) as ProtobufMapper
        val schema = inboundMapper.generateSchemaFor(WriteRequest::class.java)
        return inboundMapper.readerFor(WriteRequest::class.java).with(schema)
    }

    private fun createObjectWriter(): ObjectWriter {
        val outboundMapper = ObjectMapper().registerModule(KotlinModule()) as ObjectMapper
        return outboundMapper.writerFor(Sample::class.java)
    }

    override fun handle(ctx: Context) {
        val data = Snappy.uncompress(ctx.bodyAsBytes())
        val request = reader.readValue<WriteRequest>(data)
        request.timeseries.forEach { series ->
            val name = series.labels.first { it.name == "__name__" }.value
            val labels = LabelSet(
                series.labels.map(Label::name),
                series.labels.map(Label::value)
            )
            series.samples.forEach { sample ->
                val outbound = Sample(sample.timestamp, name, sample.value, labels)
                val jsonBytes = writer.writeValueAsBytes(outbound)
                producer.send(ProducerRecord(topic, jsonBytes))
            }
        }
    }
}
