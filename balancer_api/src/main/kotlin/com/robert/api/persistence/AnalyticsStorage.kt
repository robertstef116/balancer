package com.robert.api.persistence

import com.robert.analytics.UsageAnalyticsEntry
import com.robert.DBConnector
import com.robert.analytics.ImageScalingAnalyticsData
import com.robert.analytics.WorkflowAnalyticsData
import com.robert.enums.WorkflowAnalyticsEventType
import org.slf4j.LoggerFactory
import java.time.Instant

class AnalyticsStorage {
    companion object {
        private val log = LoggerFactory.getLogger(com.robert.api.persistence.AnalyticsStorage::class.java)
    }

    private val ANALYTICS_RESULTS_COUNT = 100

    fun getAnalytics(from: Long, workerId: String?, workflowId: String?, deploymentId: String?): List<UsageAnalyticsEntry> {
        val analytics = ArrayList<UsageAnalyticsEntry>()
        val to = Instant.now().epochSecond
        val step: Long = (to - from) / ANALYTICS_RESULTS_COUNT + 1

        val qs = StringBuilder()

        if (workerId != null) {
            qs.append("AND worker_id = ?")
        }

        if (workflowId != null) {
            qs.append("AND workflow_id = ?")
        }

        if (deploymentId != null) {
            qs.append("AND deployment_id = ?")
        }

        DBConnector.getConnection().prepareStatement(
            """
                WITH CTS AS (
                    SELECT generate_series(?, $to, $step) AS Series
                )
                SELECT COALESCE(count(A.timestamp), 0) AS value, CTS.Series AS time
                FROM CTS
                LEFT OUTER JOIN analytics A
                ON A.timestamp >= CTS.Series AND A.timestamp < CTS.Series + $step $qs
                GROUP BY CTS.Series
                ORDER BY CTS.Series
            """.trimIndent()
        ).use { st ->
            st.setLong(1, from)
            var parameterIdx = 2

            if (workerId != null) {
                st.setString(parameterIdx++, workerId)
            }

            if (workflowId != null) {
                st.setString(parameterIdx++, workflowId)
            }

            if (deploymentId != null) {
                st.setString(parameterIdx, deploymentId)
            }

            st.executeQuery()
                .use { rs ->
                    while (rs.next()) {
                        analytics.add(
                            UsageAnalyticsEntry(
                                rs.getLong("value"),
                                rs.getLong("time")
                            )
                        )
                    }
                }
        }

        return analytics
    }

    fun getWorkflowAnalytics(
        from: Long,
        workerId: String?,
        workflowId: String?
    ): Pair<MutableMap<String, ImageScalingAnalyticsData>, List<WorkflowAnalyticsData>> {
        val analytics = ArrayList<WorkflowAnalyticsData>()
        val workflowMapping = mutableMapOf<String, ImageScalingAnalyticsData>()

        val qs = StringBuilder()

        if (workerId != null) {
            qs.append("AND worker_id = ?")
        }

        if (workflowId != null) {
            qs.append("AND workflow_id = ?")
        }

        DBConnector.getTransactionConnection().use { conn ->
            try {
                conn.prepareStatement(
                    """
                        SELECT d.workflow_id, w.image, count('*') as no_of_deployments
                        FROM deployments d INNER JOIN workflows w on w.id = d.workflow_id ${if (workerId != null) "WHERE worker_id = ?" else ""} 
                        GROUP BY d.workflow_id, w.image
                    """.trimIndent()
                ).use { st ->
                    if (workerId != null) {
                        st.setString(1, workerId)
                    }

                    st.executeQuery().use { rs ->
                        while (rs.next()) {
                            workflowMapping[rs.getString("workflow_id")] = ImageScalingAnalyticsData(
                                rs.getString("image"), rs.getInt("no_of_deployments")
                            )
                        }
                    }
                }

                conn.prepareStatement(
                    """
                        SELECT wa.workflow_id, w.image, wa.event, wa.timestamp 
                        FROM workflow_analytics wa INNER JOIN workflows w on w.id = wa.workflow_id
                        WHERE timestamp > ? $qs
                        ORDER BY wa.timestamp ASC
                    """.trimIndent()
                ).use { st ->
                    st.setLong(1, from)

                    var parameterIdx = 2

                    if (workerId != null) {
                        st.setString(parameterIdx++, workerId)
                    }

                    if (workflowId != null) {
                        st.setString(parameterIdx, workflowId)
                    }

                    st.executeQuery().use { rs ->
                        while (rs.next()) {
                            analytics.add(
                                WorkflowAnalyticsData(
                                    rs.getString("workflow_id"),
                                    rs.getString("image"),
                                    WorkflowAnalyticsEventType.valueOf(rs.getString("event")),
                                    rs.getLong("timestamp")
                                )
                            )
                        }
                    }
                }

                conn.commit()

                return Pair(workflowMapping, analytics)
            } catch (e: Exception) {
                com.robert.api.persistence.AnalyticsStorage.Companion.log.error("error getting workflow analytics data, err = {}", e.message)
                conn.rollback()
                return Pair(mutableMapOf(), emptyList())
            }
        }
    }
}
