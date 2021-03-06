package com.github.sukhinin.prometheus.kafka.writer.serializers

import com.github.sukhinin.prometheus.kafka.writer.data.Label
import com.github.sukhinin.prometheus.kafka.writer.data.LabeledSample
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec

internal class ClickHouseJsonSerializerTest : ShouldSpec({
    should("serialize sample to json byte array") {
        val serializer = ClickHouseJsonSerializer()
        val sample = LabeledSample(1234567890, "n", 100.0, setOf(Label("ln1", "lv1"), Label("ln2", "lv2")))
        val bytes = serializer.serialize("topic", sample)
        val expected = """{"timestamp":1234567,"metric":"n","value":100.0,"tags.name":["ln1","ln2"],"tags.value":["lv1","lv2"]}"""
        String(bytes) shouldBe expected
    }

    should("extract tags to top-level json") {
        val serializer = ClickHouseJsonSerializer()
        serializer.configure(mutableMapOf("value.serializer.extract.tags" to "ln1,ln3"), false)
        val sample = LabeledSample(1234567890, "n", 100.0, setOf(Label("ln1", "lv1"), Label("ln2", "lv2"), Label("ln3", "lv3")))
        val bytes = serializer.serialize("topic", sample)
        val expected = """{"timestamp":1234567,"metric":"n","value":100.0,"ln1":"lv1","ln3":"lv3","tags.name":["ln1","ln2","ln3"],"tags.value":["lv1","lv2","lv3"]}"""
        String(bytes) shouldBe expected
    }
})
