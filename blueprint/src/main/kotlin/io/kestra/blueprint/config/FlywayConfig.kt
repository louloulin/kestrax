package io.kestra.blueprint.config

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.event.ApplicationStartupEvent
import io.micronaut.transaction.annotation.Transactional
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Flyway配置类
 * 确保在应用启动时执行数据库迁移
 */
@Context
@Requires(property = "flyway.datasources.default.enabled", value = "true")
open class FlywayConfig(
    private val dataSource: DataSource
) : ApplicationEventListener<ApplicationStartupEvent> {

    private val logger = LoggerFactory.getLogger(FlywayConfig::class.java)

    @Transactional
    override fun onApplicationEvent(event: ApplicationStartupEvent) {
        logger.info("🔄 Starting Flyway database migration...")

        try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .cleanDisabled(false)
                .baselineVersion("0")
                .baselineDescription("Initial baseline")
                .load()

            val migrationInfo = flyway.info()
            logger.info("📊 Current database version: ${migrationInfo.current()?.version ?: "empty"}")
            logger.info("📋 Pending migrations: ${migrationInfo.pending().size}")

            val result = flyway.migrate()
            logger.info("✅ Flyway migration completed successfully!")
            logger.info("📈 Migrations executed: ${result.migrationsExecuted}")
            logger.info("🎯 Target schema version: ${result.targetSchemaVersion}")

        } catch (e: Exception) {
            logger.error("❌ Flyway migration failed: ${e.message}", e)
            throw e
        }
    }
}
