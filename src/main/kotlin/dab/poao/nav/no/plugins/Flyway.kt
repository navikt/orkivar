package dab.poao.nav.no.plugins

import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import javax.sql.DataSource


fun Application.configureFlyway(dataSource: DataSource) {
    Flyway.configure()
        .locations("db/migration")
        .dataSource(dataSource)
        .load()
        .migrate()
}