package com.bananice.businesstracer.infrastructure.persistence.alert

import spock.lang.Specification

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager

class AlertSchemaSqlSpec extends Specification {

    Connection connection

    def setup() {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:alert-schema-spec;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "sa",
                ""
        )
        executeSchema(connection)
    }

    def cleanup() {
        connection?.close()
    }

    def "schema should contain alert tables"() {
        expect:
        tableExists("business_alert_rule")
        tableExists("business_alert_channel")
        tableExists("business_alert_event")
        tableExists("business_alert_dispatch_log")
        tableExists("business_alert_config_version")
    }

    def "schema should contain key alert indexes"() {
        when:
        def eventIndexes = readIndexes("business_alert_event")
        def dispatchIndexes = readIndexes("business_alert_dispatch_log")
        def ruleIndexes = readIndexes("business_alert_rule")

        then:
        hasIndex(eventIndexes, ["create_time", "alert_type", "status"], false)
        hasIndex(eventIndexes, ["flow_code", "node_code", "business_id", "create_time"], false)
        hasIndex(eventIndexes, ["aggregate_key", "status", "last_occur_time"], false)

        and:
        hasIndex(dispatchIndexes, ["event_id", "dispatch_time"], false)

        and:
        hasIndex(ruleIndexes, ["scope_type", "scope_code"], true)
        hasIndex(ruleIndexes, ["scope_type", "flow_code", "scope_code"], true)
    }

    private void executeSchema(Connection connection) {
        def schemaSql = this.class.classLoader.getResource("schema.sql").text
        schemaSql.split(/;\s*(\r?\n|$)/)
                .collect { it.trim() }
                .findAll { !it.isEmpty() }
                .each { connection.createStatement().execute(it) }
    }

    private boolean tableExists(String tableName) {
        def statement = connection.prepareStatement("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?
        """)
        statement.setString(1, tableName)
        def rs = statement.executeQuery()
        rs.next()
        rs.getInt(1) == 1
    }

    private List<Map<String, Object>> readIndexes(String tableName) {
        DatabaseMetaData metaData = connection.getMetaData()
        def indexRows = [:].withDefault { [unique: true, columns: [:]] }
        def rs = metaData.getIndexInfo(null, "PUBLIC", tableName, false, false)
        while (rs.next()) {
            def indexName = rs.getString("INDEX_NAME")
            if (!indexName) {
                continue
            }
            if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                continue
            }
            def columnName = rs.getString("COLUMN_NAME")
            if (!columnName) {
                continue
            }
            def ordinal = rs.getShort("ORDINAL_POSITION") as Integer
            def index = indexRows[indexName]
            index.unique = !rs.getBoolean("NON_UNIQUE")
            index.columns[ordinal] = columnName.toLowerCase()
        }

        indexRows.values().collect {
            [
                    unique : it.unique,
                    columns: it.columns.keySet().sort().collect { position -> it.columns[position] }
            ]
        }
    }

    private static boolean hasIndex(List<Map<String, Object>> indexes, List<String> columns, boolean unique) {
        indexes.any { it.unique == unique && it.columns == columns }
    }
}
