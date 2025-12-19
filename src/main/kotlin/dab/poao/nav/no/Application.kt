package dab.poao.nav.no

import configureAuthentication
import dab.poao.nav.no.database.JournalføringerRepository
import dab.poao.nav.no.plugins.*
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
    val journalføringerRepository = JournalføringerRepository(datasource)
    configureAuthentication()
    configureMonitoring()
    configureRouting(httpClientEngine = httpClientEngine, lagreJournalføring = journalføringerRepository::lagreJournalfoering, hentJournalføringer = journalføringerRepository::hentJournalposter)
    configureFlyway(datasource)
    configureErrorHandling()
}
