package dab.poao.nav.no

import configureAuthentication
import dab.poao.nav.no.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureAuthentication()
    configureMonitoring()
    configureRouting()
}
