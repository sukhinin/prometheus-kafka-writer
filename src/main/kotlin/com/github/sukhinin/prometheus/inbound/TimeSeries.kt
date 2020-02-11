package com.github.sukhinin.prometheus.inbound

data class TimeSeries(val labels: Collection<Label>, val samples: Collection<Sample>)
