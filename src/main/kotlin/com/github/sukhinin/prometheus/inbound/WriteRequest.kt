package com.github.sukhinin.prometheus.inbound

data class WriteRequest(val timeseries: Collection<TimeSeries>)
