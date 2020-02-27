package com.github.sukhinin.prometheus.kafka.writer.data

data class WriteRequest(val timeseries: Collection<TimeSeries>)
