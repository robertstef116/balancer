package com.robert.storage.entities

import com.robert.enums.LBAlgorithms
import org.jetbrains.exposed.sql.Table

object Workflows : Table() {
    val id = uuid("id").autoGenerate()
    val image = varchar("image", 255)
    val memoryLimit = long("memoryLimit")
    val cpuLimit = long("cpuLimit")
    val minDeployments = integer("minDeployments").nullable()
    val maxDeployments = integer("maxDeployments").nullable()
    val algorithm = enumerationByName("algorithm", 25, LBAlgorithms::class)

    override val primaryKey = PrimaryKey(id)
}