package com.robert.storage.repository.exposed

import com.robert.resources.Worker
import com.robert.enums.WorkerState
import com.robert.storage.entities.Workers
import com.robert.storage.repository.WorkerRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class WorkerRepositoryImpl : WorkerRepository {
    override fun getAll(): Collection<Worker> = transaction {
        Workers.selectAll()
            .orderBy(Workers.alias)
            .map { row ->
            Worker(
                id = row[Workers.id],
                alias = row[Workers.alias],
                state = row[Workers.status],
            )
        }
    }

    override fun getAllOnline(): Collection<Worker> = transaction {
        Workers.selectAll()
            .where(Workers.status eq WorkerState.ONLINE)
            .map { row ->
                Worker(
                    id = row[Workers.id],
                    alias = row[Workers.alias],
                    state = row[Workers.status],
                )
            }
    }

    override fun create(worker: Worker): Unit = transaction {
        Workers.insert {
            it[id] = worker.id
            it[alias] = worker.alias
            it[status] = worker.state
        }
    }

    override fun find(id: UUID): Worker? = transaction {
        val workers = Workers.selectAll()
            .where { Workers.id eq id }
            .map { row ->
                Worker(
                    id = row[Workers.id],
                    alias = row[Workers.alias],
                    state = row[Workers.status],
                )
            }
        if (workers.isEmpty()) null else workers[0]
    }

    override fun update(id: UUID, alias: String?, status: WorkerState?): Boolean = transaction {
        Workers.update({ Workers.id eq id }) {
            if (alias != null)
                it[this.alias] = alias
            if (status != null)
                it[this.status] = status
        } != 0
    }

    override fun delete(id: UUID): Boolean = transaction {
        Workers.deleteWhere { Workers.id eq id } != 0
    }
}