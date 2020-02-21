package com.github.sukhinin.prometheus.write

import com.github.sukhinin.prometheus.write.data.*
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.specs.ShouldSpec
import org.apache.kafka.clients.producer.MockProducer

internal class WriteRequestHandlerTest : ShouldSpec({
    should("handle write request") {
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

        val producer = MockProducer<Nothing, LabeledSample>(true, null, null)
        val handler = WriteRequestHandler(producer, "prometheus-metrics")
        handler.handle(request)

        producer.history().map { it.topic() } shouldContainExactly MutableList(4) { "prometheus-metrics" }

        producer.history().map { it.value() } shouldContainExactly listOf(
            LabeledSample(111111, "metric_name_1", 100.0, listOf(Label("ln1", "lv1"), Label("ln2", "lv2"))),
            LabeledSample(222222, "metric_name_1", 200.0, listOf(Label("ln1", "lv1"), Label("ln2", "lv2"))),
            LabeledSample(333333, "metric_name_2", 300.0, listOf(Label("ln3", "lv3"), Label("ln4", "lv4"))),
            LabeledSample(444444, "metric_name_2", 400.0, listOf(Label("ln3", "lv3"), Label("ln4", "lv4")))
        )
    }
})
