package dab.poao.nav.no

import configureAuthentication
import dab.poao.nav.no.plugins.configureMonitoring
import dab.poao.nav.no.plugins.configureRouting
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(httpClientEngine: HttpClientEngine = HttpClient().engine) {
    install(ContentNegotiation) {
        json()
    }
    configureAuthentication()
    configureMonitoring()
    configureRouting(httpClientEngine)
}
