package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.ApplicationSendPipeline
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import no.nav.helse.spleis.HelseBuilder
import no.nav.helse.spleis.PersonMediator
import no.nav.helse.spleis.http.getJson
import no.nav.helse.spleis.person
import no.nav.helse.spleis.utbetaling
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory
import java.net.URL
import kotlin.system.exitProcess

@KtorExperimentalAPI
fun Application.helseStream(env: Map<String, String>) {
    val kafkaConfigBuilder = KafkaConfigBuilder(env)
    val dataSourceBuilder = DataSourceBuilder(env)

    val helseBuilder = HelseBuilder(
        dataSource = dataSourceBuilder.getDataSource(),
        hendelseProducer = KafkaProducer<String, String>(kafkaConfigBuilder.producerConfig(), StringSerializer(), StringSerializer())
    )

    helseBuilder.addStateListener(KafkaStreams.StateListener { newState, oldState ->
        log.info("From state={} to state={}", oldState, newState)

        if (newState == KafkaStreams.State.ERROR) {
            log.error("exiting JVM process because the kafka stream has died")
            exitProcess(1)
        }
    })

    environment.monitor.subscribe(ApplicationStarted) {
        dataSourceBuilder.migrate()
        helseBuilder.start(kafkaConfigBuilder.streamsConfig())
    }

    environment.monitor.subscribe(ApplicationStopping) {
        helseBuilder.stop()
    }

    restInterface(helseBuilder.personMediator)
}

private val httpTraceLog = LoggerFactory.getLogger("HttpTraceLog")

private val httpRequestCounter = Counter.build("http_requests_total", "Counts the http requests")
    .labelNames("method", "code")
    .register()

private val httpRequestDuration =
    Histogram.build("http_request_duration_seconds", "Distribution of http request duration")
        .register()

@KtorExperimentalAPI
private fun Application.restInterface(
    personMediator: PersonMediator,
    configurationUrl: String = environment.config.property("azure.configuration_url").getString(),
    clientId: String = environment.config.property("azure.client_id").getString(),
    requiredGroup: String = environment.config.property("azure.required_group").getString()
) {
    val idProvider = configurationUrl.getJson()
    val jwkProvider = JwkProviderBuilder(URL(idProvider["jwks_uri"].textValue())).build()

    intercept(ApplicationCallPipeline.Monitoring) {
        val timer = httpRequestDuration.startTimer()

        httpTraceLog.info("incoming ${call.request.httpMethod.value} ${call.request.uri}")

        try {
            proceed()
        } catch (err: Throwable) {
            httpTraceLog.info("exception thrown during processing: ${err.message}", err)
            throw err
        } finally {
            timer.observeDuration()
        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        val status = call.response.status() ?: (when (message) {
            is OutgoingContent -> message.status
            is HttpStatusCode -> message
            else -> null
        } ?: HttpStatusCode.OK).also { status ->
            call.response.status(status)
        }

        httpTraceLog.info("responding with ${status.value}")
        httpRequestCounter.labels(call.request.httpMethod.value, "${status.value}").inc()
    }

    install(Authentication) {
        jwt {
            verifier(jwkProvider, idProvider["issuer"].textValue())
            validate { credentials ->
                val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
                if (requiredGroup in groupsClaim && clientId in credentials.payload.audience) {
                    JWTPrincipal(credentials.payload)
                } else {
                    log.info(
                        "${credentials.payload.subject} with audience ${credentials.payload.audience} " +
                            "is not authorized to use this app, denying access"
                    )
                    null
                }
            }
        }
    }

    routing {
        authenticate {
            utbetaling(personMediator)
            person(personMediator)
        }
    }

}
