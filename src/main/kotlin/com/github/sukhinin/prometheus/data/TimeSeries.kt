package com.github.sukhinin.prometheus.data

data class TimeSeries(val labels: Collection<Label>, val samples: Collection<Sample>)
