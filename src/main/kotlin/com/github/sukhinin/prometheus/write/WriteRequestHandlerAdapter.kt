package com.github.sukhinin.prometheus.write

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.sukhinin.prometheus.write.data.WriteRequest
import io.javalin.http.Context
import io.javalin.http.Handler
import org.xerial.snappy.Snappy

class WriteRequestHandlerAdapter(private val handler: WriteRequestHandler) : Handler {

    private val reader = createObjectReader()

    private fun createObjectReader(): ObjectReader {
        val mapper = ProtobufMapper().registerModule(KotlinModule()) as ProtobufMapper
        val schema = mapper.generateSchemaFor(WriteRequest::class.java)
        return mapper.readerFor(WriteRequest::class.java).with(schema)
    }

    override fun handle(ctx: Context) {
        val data = Snappy.uncompress(ctx.bodyAsBytes())
        val request = reader.readValue<WriteRequest>(data)
        handler.handle(request)
    }
}
