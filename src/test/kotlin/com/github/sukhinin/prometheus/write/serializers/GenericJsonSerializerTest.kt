package com.github.sukhinin.prometheus.write.serializers

import com.github.sukhinin.prometheus.write.data.Label
import com.github.sukhinin.prometheus.write.data.LabeledSample
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec

internal class GenericJsonSerializerTest : ShouldSpec({
    should("serialize sample to json byte array") {
        val serializer = GenericJsonSerializer()
        val sample = LabeledSample(1234567890, "n", 100.0, setOf(Label("ln1", "lv1"), Label("ln2", "lv2")))
        val bytes = serializer.serialize("topic", sample)
        val expected = """{"timestamp":1234567890,"name":"n","value":100.0,"labels":[{"name":"ln1","value":"lv1"},{"name":"ln2","value":"lv2"}]}"""
        String(bytes) shouldBe expected
    }
})
