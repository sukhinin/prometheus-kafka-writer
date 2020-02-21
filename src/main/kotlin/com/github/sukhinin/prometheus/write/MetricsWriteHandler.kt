package com.github.sukhinin.prometheus.write

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.sukhinin.prometheus.write.data.LabeledSample
import com.github.sukhinin.prometheus.write.data.WriteRequest
import io.javalin.http.Context
import io.javalin.http.Handler
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.xerial.snappy.Snappy

class MetricsWriteHandler(private val producer: Producer<Nothing, LabeledSample>, private val topic: String) : Handler {

    private val reader = createObjectReader()

    private fun createObjectReader(): ObjectReader {
        val inboundMapper = ProtobufMapper().registerModule(KotlinModule()) as ProtobufMapper
        val schema = inboundMapper.generateSchemaFor(WriteRequest::class.java)
        return inboundMapper.readerFor(WriteRequest::class.java).with(schema)
    }

    override fun handle(ctx: Context) {
        val data = Snappy.uncompress(ctx.bodyAsBytes())
        val request = reader.readValue<WriteRequest>(data)
        request.timeseries.forEach { series ->
            val name = series.labels.first { it.name == "__name__" }.value
            series.samples.forEach { sample ->
                val labeledSample = LabeledSample(sample.timestamp, name, sample.value, series.labels)
                val record = ProducerRecord<Nothing, LabeledSample>(topic, labeledSample)
                producer.send(record)
            }
        }
    }
}
