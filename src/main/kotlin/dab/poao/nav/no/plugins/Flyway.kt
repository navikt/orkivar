package dab.poao.nav.no.plugins

import javax.sql.DataSource
import org.flywaydb.core.Flyway


fun configureFlyway(dataSource: DataSource) {
    Flyway.configure()
        .validateMigrationNaming(true)
        .dataSource(dataSource)
        .load()
        .migrate()
}