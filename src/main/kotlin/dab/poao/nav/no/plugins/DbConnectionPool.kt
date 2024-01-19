package dab.poao.nav.no.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import javax.sql.DataSource

fun Application.createHikariDataSource(): DataSource {

    val config = environment.config
    val host = config.property("postgres.host").getString()
    val port = config.property("postgres.port").getString()
    val databaseName = config.property("postgres.database-name").getString()
    val user = config.property("postgres.username").getString()
    val pw = config.property("postgres.password").getString()

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
}