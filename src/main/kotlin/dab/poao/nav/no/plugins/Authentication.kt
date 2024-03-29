import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication() {
    val config = environment.config
    install(Authentication) {
        tokenValidationSupport(config = config, name = "AzureAD")
    }
}