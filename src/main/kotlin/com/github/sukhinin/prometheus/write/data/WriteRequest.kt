package com.github.sukhinin.prometheus.write.data

data class WriteRequest(val timeseries: Collection<TimeSeries>)
