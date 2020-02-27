package com.github.sukhinin.prometheus.kafka.writer

import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.sukhinin.prometheus.kafka.writer.data.Label
import com.github.sukhinin.prometheus.kafka.writer.data.Sample
import com.github.sukhinin.prometheus.kafka.writer.data.TimeSeries
import com.github.sukhinin.prometheus.kafka.writer.data.WriteRequest
import io.javalin.http.Context
import io.kotlintest.specs.ShouldSpec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.xerial.snappy.Snappy

@Suppress("BlockingMethodInNonBlockingContext")
internal class WriteRequestHandlerAdapterTest : ShouldSpec({
    should("adapt write request handler") {
        val request = WriteRequest(
            listOf(
                TimeSeries(
                    listOf(Label("__name__", "metric_name_1"), Label("ln1", "lv1"), Label("ln2", "lv2")),
                    listOf(Sample(100.0, 111111), Sample(200.0, 222222))
                ),
                TimeSeries(
                    listOf(Label("__name__", "metric_name_2"), Label("ln3", "lv3"), Label("ln4", "lv4")),
                    listOf(Sample(300.0, 333333), Sample(400.0, 444444))
                )
            )
        )

        val mapper = ProtobufMapper().registerModule(KotlinModule()) as ProtobufMapper
        val schema = mapper.generateSchemaFor(WriteRequest::class.java)
        val writer = mapper.writerFor(WriteRequest::class.java).with(schema)

        val bytes = Snappy.compress(writer.writeValueAsBytes(request))
        val context = mockk<Context>(relaxed = true)
        every { context.bodyAsBytes() } returns bytes

        val handler = mockk<WriteRequestHandler>(relaxed = true)
        val adapter = WriteRequestHandlerAdapter(handler)
        adapter.handle(context)

        verify { handler.handle(request) }
        confirmVerified(handler)
    }
})
