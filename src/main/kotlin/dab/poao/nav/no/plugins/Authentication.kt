import com.nimbusds.jose.util.DefaultResourceRetriever
import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication() {
    val config = environment.config

    install(Authentication) {
        tokenValidationSupport(
            config = config,
            resourceRetriever = DefaultResourceRetriever(),
            name = "AzureAD"
        )
    }
}