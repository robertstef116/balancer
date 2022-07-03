package com.robert.balancing

import org.slf4j.LoggerFactory


data class TargetWeightData(
    val target: TargetData,
    var weight: Double
) {
    companion object{
        private val log = LoggerFactory.getLogger(TargetWeightData::class.java)

        fun selectRandomTarget(targets: List<TargetWeightData>): TargetWeightData {
            var rand = kotlin.random.Random.nextDouble()
            for (target in targets) {
                val weight = target.weight
                if (rand < weight) {
                    return target
                }
                rand -= weight
            }
            log.error("Unable to randomly select the target")
            return targets.first()
        }
    }

    private var numberOfElements: Int = 0

    var averageResponseTime: Double = 0.0
        private set

    @Synchronized
    fun addResponseTime(responseTime: Long) {
        // take into consideration for the average only the last Int.MAX_VALUE requests
        if (numberOfElements != Int.MAX_VALUE) {
            numberOfElements++
        }

        averageResponseTime = ((numberOfElements - 1f) / numberOfElements) * averageResponseTime +
                (1f / numberOfElements) * responseTime
    }

    @Synchronized
    fun reset(): Pair<Int, Double> {
        val res = Pair(numberOfElements, averageResponseTime)
        numberOfElements = 0
        averageResponseTime = 0.0
        return res
    }
}
