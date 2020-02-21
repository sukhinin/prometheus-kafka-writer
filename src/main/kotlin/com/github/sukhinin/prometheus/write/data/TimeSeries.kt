package com.github.sukhinin.prometheus.write.data

data class TimeSeries(val labels: Collection<Label>, val samples: Collection<Sample>)
