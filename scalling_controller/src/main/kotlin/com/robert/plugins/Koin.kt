package com.robert.plugins

import com.robert.RabbitmqService
import com.robert.SchedulerService
import com.robert.controller.DeploymentService
import com.robert.controller.DeploymentsManager
import com.robert.controller.ScalingManager
import com.robert.controller.HealthChecker
import com.robert.persistance.DAORepository
import com.robert.persistance.DAORepositoryImpl
import io.ktor.server.application.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureKoin() {
    install(Koin) {
        modules(module {
            singleOf(::DeploymentService)
            singleOf(::DeploymentsManager)
            singleOf(::RabbitmqService)
            singleOf(::ScalingManager)
            singleOf(::SchedulerService)
            singleOf(::HealthChecker)
            singleOf<DAORepository>(::DAORepositoryImpl)
        })
    }
}
