package dab.poao.nav.no.dokark

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

class DokarkClient {

    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
    }
}