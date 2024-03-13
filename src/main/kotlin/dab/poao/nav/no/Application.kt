package dab.poao.nav.no

import configureAuthentication
import dab.poao.nav.no.database.Repository
import dab.poao.nav.no.plugins.configureHikariDataSource
import dab.poao.nav.no.plugins.configureFlyway
import dab.poao.nav.no.plugins.configureMonitoring
import dab.poao.nav.no.plugins.configureRouting
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
    val logger = LoggerFactory.getLogger(Application::class.java)
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        logger.error("Uncaught exception i thread: ${thread.name}", exception)
    }
}

fun Application.module(httpClientEngine: HttpClientEngine = HttpClient().engine) {
    install(ContentNegotiation) {
        json()
    }
    val datasource = configureHikariDataSource()
    val repository = Repository(datasource)
    configureAuthentication()
    configureMonitoring()
    configureRouting(httpClientEngine = httpClientEngine, lagreJournalføring = repository::lagreJournalfoering, hentJournalføringer = repository::hentJournalposter)
    configureFlyway(datasource)
}
