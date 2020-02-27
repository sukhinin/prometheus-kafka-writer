package com.github.sukhinin.prometheus.kafka.writer.data

data class TimeSeries(val labels: Collection<Label>, val samples: Collection<Sample>)
