package dab.poao.nav.no.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.*
import javax.sql.DataSource

fun Application.configureHikariDataSource(): DataSource {
    return configureHikariDataSource(environment.config)
}
fun configureHikariDataSource(config: ApplicationConfig): DataSource {
    val env = config.property("cluster").getString()
    val host = config.property("postgres.host").getString()
    val port = config.property("postgres.port").getString()
    val databaseName = config.property("postgres.database-name").getString()
    val user = config.property("postgres.user").getString()
    val pw = config.property("postgres.password").getString()

    if (env == "dev" || env == "dev-gcp") {
        return HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://$host:$port/$databaseName"
            maximumPoolSize = 3
            isAutoCommit = true
            initializationFailTimeout = 5000
            minimumIdle = 1
            username = user
            password = pw
            validate()
        })
    } else {
        return HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = config.property("postgres.jdbc-url").getString()
            maximumPoolSize = 5
            isAutoCommit = true
            initializationFailTimeout = 5000
            minimumIdle = 1
            validate()
        })
    }
}