package com.robert.persistance

import com.robert.AnalyticsEntry
import com.robert.DBConnector
import java.time.Instant

class AnalyticsStorage {
    private val ANALYTICS_RESULTS_COUNT = 100

    fun getAnalytics(from: Long, workerId: String?, workflowId: String?, deploymentId: String?): List<AnalyticsEntry> {
        val analytics = ArrayList<AnalyticsEntry>()
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
        )
            .use { st ->
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
                                AnalyticsEntry(
                                    rs.getLong("value"),
                                    rs.getLong("time")
                                )
                            )
                        }
                    }
            }

        return analytics
    }
}
