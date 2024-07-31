package com.robert.storage.repository.exposed

import com.robert.analytics.AnalyticsData
import com.robert.balancing.LoadBalancerResponseType
import com.robert.storage.repository.AnalyticsRepository
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class AnalyticsRepositoryImpl : AnalyticsRepository {
    companion object {
        private const val ANALYTICS_SAMPLING_COUNT = 200

        private val GET_SCALING_ANALYTICS_DATA_TEMPL = """
            WITH CTS AS (
                SELECT generate_series(?, ?, ?) AS Series
            )
            SELECT a.workflow_id, COALESCE(%s, 0) AS data_value, CTS.Series AS data_time
            FROM CTS
            LEFT OUTER JOIN scaling_analytics a
            ON a.timestamp_ms >= CTS.Series AND a.timestamp_ms < CTS.Series + ? %s
            GROUP BY CTS.Series, a.workflow_id
            ORDER BY CTS.Series
        """.trimIndent()

        private val GET_LOAD_BALANCING_ANALYTICS_DATA_TEMPL = """
            WITH CTS AS (
                SELECT generate_series(?, ?, ?) AS Series
            )
            SELECT l.workflow_id, l.path, COALESCE(CEIL(AVG(l.response_time_ms)), 0) AS data_value, CTS.Series AS data_time
            FROM CTS
            LEFT OUTER JOIN load_balancer_analytics l
            ON l.timestamp_ms >= CTS.Series AND l.timestamp_ms < CTS.Series + ? AND l.response_type = ?%s
            GROUP BY CTS.Series, l.workflow_id, l.path
            ORDER BY CTS.Series
        """.trimIndent()
    }

    override fun getScalingAnalyticsData(workflowId: UUID?, metric: String, durationMs: Long): List<AnalyticsData> = transaction {
        val (from, to, step) = getSeriesParametersFromDuration(durationMs)

        val params = mutableListOf<Pair<IColumnType, Any>>(
            LongColumnType() to from,
            LongColumnType() to to + step,
            LongColumnType() to step,
            LongColumnType() to step
        )

        val query: String
        val metricColumn = when (metric) {
            "avg_cpu" -> "AVG(a.avg_cpu)"
            "avg_memory" -> "AVG(a.avg_memory)"
            else -> "CEIL(AVG(a.replicas))"
        }
        if (workflowId == null) {
            query = String.format(GET_SCALING_ANALYTICS_DATA_TEMPL, metricColumn, "")
        } else {
            query = String.format(GET_SCALING_ANALYTICS_DATA_TEMPL, metricColumn, "AND workflow_id = ?")
            params.add(VarCharColumnType() to workflowId)
        }

        exec(query, params, StatementType.EXEC) {
            val data = mutableListOf<AnalyticsData>()
            while (it.next()) {
                data.add(AnalyticsData(it.getString("workflow_id") ?: "", it.getDouble("data_value"), it.getLong("data_time")))
            }
            data
        } as List<AnalyticsData>
    }

    override fun getLoadBalancingAnalyticsData(workflowId: UUID?, path: String?, responseType: LoadBalancerResponseType, durationMs: Long): List<AnalyticsData> = transaction {
        val (from, to, step) = getSeriesParametersFromDuration(durationMs)

        val params = mutableListOf<Pair<IColumnType, Any>>(
            LongColumnType() to from,
            LongColumnType() to to + step,
            LongColumnType() to step,
            LongColumnType() to step,
            VarCharColumnType() to responseType.toString()
        )

        val queryConditions = StringBuilder("")
        if (workflowId != null) {
            queryConditions.append(" AND l.workflow_id = ?")
            params.add(VarCharColumnType() to workflowId)
        }
        if (path != null) {
            queryConditions.append(" AND l.path = ?")
            params.add(VarCharColumnType() to path)
        }
        val query = String.format(GET_LOAD_BALANCING_ANALYTICS_DATA_TEMPL, queryConditions)

        exec(query, params, StatementType.EXEC) {
            val data = mutableListOf<AnalyticsData>()
            while (it.next()) {
                data.add(AnalyticsData(String.format("%s%s", it.getString("workflow_id") ?: "", it.getString("path") ?: ""), it.getDouble("data_value"), it.getLong("data_time")))
            }
            data
        } as List<AnalyticsData>
    }

    private fun getSeriesParametersFromDuration(durationMs: Long): Triple<Long, Long, Long> {
        val to = Instant.now().toEpochMilli()
        val from = to - durationMs
        val sampling = when {
            durationMs < 600_000 -> 10
            durationMs < 1_800_000 -> 8
            durationMs < 3_600_000 -> 6
            durationMs < 10_800_000 -> 5
            durationMs < 21_600_000 -> 4
            durationMs < 43_200_000 -> 3
            durationMs < 86_400_000 -> 2
            else -> 1
        }
        val step: Long = durationMs / ANALYTICS_SAMPLING_COUNT / sampling + 1
        return Triple(from, to, step)
    }
}