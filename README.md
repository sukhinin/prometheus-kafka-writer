# Kafka remote write backend for Prometheus

[![Build Status](https://travis-ci.com/sukhinin/prometheus-kafka-bridge.svg?branch=master)](https://travis-ci.com/sukhinin/prometheus-kafka-bridge)
[![codebeat badge](https://codebeat.co/badges/8973464f-6218-4339-8cf1-9963fb6b2b04)](https://codebeat.co/projects/github-com-sukhinin-prometheus-kafka-bridge-master)

Prometheus includes a local on-disk time series database, but also optionally integrates with remote storage systems.
A limitation of the local storage is that it is not clustered or replicated. Kafka remote write backend utilizes 
Prometheus remote write API and allows metrics to be pushed into Apache Kafka.

## Requirements
Kafka remote write backend for Prometheus requires at least Java 1.8. It was tested with Apache Kafka 2.4.0 
but should also work with any recent version.

## Downloading and running
Kafka remote write backend for Prometheus is distributed as a self-containing JAR (also known as fat JAR).
Simply download `prometheus-kafka-bridge-VERSION-all.jar` from 
[GitHub releases page](https://github.com/sukhinin/prometheus-kafka-bridge/releases) and run the following
(assuming java is on your PATH):
```
java -jar prometheus-kafka-bridge-VERSION-all.jar
```

You can also build the project yourself (see [Building](#building) section)

## Configuration
By default application accepts remote connections on port 8080 and pushes metrics to local Kafka 
broker `localhost:9092` to topic `prometheus-metrics`. This behavior can be customized by overriding 
configuration properties.

Configuration values are resolved from multiple sources with the following precedence, from highest to lowest:
1. Java system properties, usually passed as one or more `-Dname=value` command line arguments,
2. custom `.properties` file specified with `-c` or `--config` command line argument,
3. [default configuration values](https://github.com/sukhinin/prometheus-kafka-bridge/blob/master/src/main/resources/reference.properties).

### Important properties
| Property | Description |
| --- | --- |
| `server.port` | Port number to listen for Prometheus remote write requests |
| `kafka.topic` | Kafka topic to write metrics to |
| `kafka.props.bootstrap.servers` | Kafka brokers to setup initial connection with |
| `kafka.props.value.serializer` | Class used to serialize metric samples |
| `kafka.props.*` | Various Kafka producer configuration properties (see [producer docs](https://kafka.apache.org/documentation/#producerconfigs)) |

## Output formats
Kafka allows publish and subscribe to streams of records of any type. Internally Kafka sees these records as byte
arrays. Conversion of metric samples to binary representation is done by an object of a class specified 
in `kafka.props.value.serializer` configuration parameter. This class must implement 
`org.apache.kafka.common.serialization.Serializer` interface. 

Application ships with two serializer implementations: `GenericJsonSerializer` and `ClickHouseJsonSerializer`.
To use your own implementation add it to runtime classpath and set `kafka.props.value.serializer` to fully qualified
class name.

### `GenericJsonSerializer`
`com.github.sukhinin.prometheus.write.serializers.GenericJsonSerializer` is the implementation used by default. 
It converts metric samples to a JSON string having the following structure:
```
{
  "timestamp": UNIX_TIMESTAMP,
  "name": "METRIC_NAME",
  "value": METRIC_VALUE,
  "labels": [
    { "name": "LABEL_NAME_1", "value": "LABEL_VALUE_1" }, 
    { "name": "LABEL_NAME_N", "value": "LABEL_VALUE_N" }
  ]
}
```

### `ClickHouseJsonSerializer`
`com.github.sukhinin.prometheus.write.serializers.ClickHouseJsonSerializer` also converts metric samples to a JSON 
string, but it differs from the default `GenericJsonSerializer` in the way tags are serialized:
```
{
  "timestamp": UNIX_TIMESTAMP,
  "name": "METRIC_NAME",
  "value": METRIC_VALUE,
  "labels.name": [ "LABEL_NAME_1", "LABEL_NAME_N" ],
  "labels.value": [ "LABEL_VALUE_1", "LABEL_VALUE_N" ]
}
```

It corresponds to JSONEachRow ClickHouse format and allows to directly consume Kafka topic from ClickHouse
given the table has the following format:
```
CREATE TABLE test.metrics(
    timestamp DateTime,
    name String,
    value Float64,
    labels Nested(name String, value String)
) 
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(timestamp)
ORDER BY (name, timestamp)
```

## Building
Kafka remote write backend for Prometheus is built with Gradle.

- `./gradlew build` builds the project,
- `./gradlew test` runs the test suite.

Fat JAR is produced in `build/libs/prometheus-kafka-bridge-VERSION-all.jar`. The build also assembles redistributable 
application archives in `build/distributions` folder.

## Affiliation
Kafka remote write backend for Prometheus is neither affiliated with nor endorsed by Apache Kafka or Prometheus,
though it heavily relies on code developed by the aforementioned projects.
