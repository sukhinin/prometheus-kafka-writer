package com.github.sukhinin.prometheus.write

import com.github.sukhinin.prometheus.write.data.LabeledSample
import com.github.sukhinin.prometheus.write.data.WriteRequest
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class WriteRequestHandler(private val producer: Producer<Nothing, LabeledSample>, private val topic: String) {
    fun handle(request: WriteRequest) {
        request.timeseries.forEach { series ->
            val name = series.labels.first { it.name == "__name__" }.value
            val labels = series.labels.filter { it.name != "__name__" }
            series.samples.forEach { sample ->
                val labeledSample = LabeledSample(sample.timestamp, name, sample.value, labels)
                val record = ProducerRecord<Nothing, LabeledSample>(topic, labeledSample)
                producer.send(record)
            }
        }
    }
}
