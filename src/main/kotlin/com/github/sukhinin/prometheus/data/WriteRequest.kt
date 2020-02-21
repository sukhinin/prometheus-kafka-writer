package com.github.sukhinin.prometheus.data

data class WriteRequest(val timeseries: Collection<TimeSeries>)
