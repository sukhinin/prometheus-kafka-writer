package com.github.sukhinin.prometheus.kafka.writer

import com.github.sukhinin.prometheus.kafka.writer.config.Config
import com.github.sukhinin.prometheus.kafka.writer.config.ConfigMapper
import com.github.sukhinin.prometheus.kafka.writer.config.KafkaConfig
import com.github.sukhinin.prometheus.kafka.writer.data.LabeledSample
import com.github.sukhinin.simpleconfig.*
import io.javalin.Javalin
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.jetty.InstrumentedQueuedThreadPool
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.eclipse.jetty.server.Server
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.Duration
import kotlin.system.exitProcess

object RemoteWriteServer {

    private val logger = LoggerFactory.getLogger(RemoteWriteServer::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val ns = parseCommandLineArgs(args)
        val config = getApplicationConfig(ns)

        val meterRegistry = createPrometheusMeterRegistry()
        setupCommonMeterBindings()

        val producer = createKafkaProducer(config.kafka)

        val server = createJavalinServer()
        val metricsHandler = WriteRequestHandler(producer, config.kafka.topic)
        server.post("/", WriteRequestHandlerAdapter(metricsHandler))
        server.get("/metrics") { ctx -> ctx.result(meterRegistry.scrape()) }
        server.start(config.server.port)
    }

    private fun parseCommandLineArgs(args: Array<String>): Namespace {
        val parser = ArgumentParsers.newFor("prometheus-kafka-writer").build()
            .defaultHelp(true)
            .description("Prometheus remote write backend with Kafka export.")
        parser.addArgument("-c", "--config")
            .metavar("PATH")
            .help("path to configuration file")

        return try {
            parser.parseArgs(args)
        } catch (e: ArgumentParserException) {
            parser.handleError(e)
            exitProcess(1)
        }
    }

    private fun getApplicationConfig(ns: Namespace): Config {
        val systemPropertiesConfig = System.getProperties().stringPropertyNames()
            .filter { name -> name.startsWith("app.") }
            .associateWith { name -> System.getProperties().getProperty(name) }
            .mapKeys { (name, _) -> name.removePrefix("app.") }
            .let(::MapConfig)
        val applicationConfig = ns.getString("config")
            ?.let { s -> Paths.get(s) }
            ?.let { path -> ConfigLoader.getConfigFromPath(path) }
            ?: MapConfig(emptyMap())
        val referenceConfig = ConfigLoader.getConfigFromSystemResource("reference.properties")

        val config = systemPropertiesConfig
            .withFallback(applicationConfig)
            .withFallback(referenceConfig)
            .resolved()
        logger.info("Loaded configuration:\n\t" + config.masked().dump().replace("\n", "\n\t"))

        return ConfigMapper.from(config)
    }

    private fun createPrometheusMeterRegistry(): PrometheusMeterRegistry {
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        Metrics.addRegistry(meterRegistry)
        return meterRegistry
    }

    private fun setupCommonMeterBindings() {
        listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            JvmThreadMetrics(),
            ProcessorMetrics(),
            LogbackMetrics()
        ).forEach { binder -> binder.bindTo(Metrics.globalRegistry) }
    }

    private fun createKafkaProducer(config: KafkaConfig): Producer<Nothing, LabeledSample> {
        val producer = KafkaProducer<Nothing, LabeledSample>(config.props)
        Runtime.getRuntime().addShutdownHook(Thread { producer.close(Duration.ofSeconds(10)) })
        return producer
    }

    private fun createJavalinServer(): Javalin {
        val server = Javalin.create { javalinConfig ->
            javalinConfig.showJavalinBanner = false
            javalinConfig.server {
                Server(InstrumentedQueuedThreadPool(Metrics.globalRegistry, emptyList()))
            }
        }
        Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
        return server
    }
}
