package com.robert.api.plugins

import com.robert.api.routes.*
import com.robert.api.service.AnalyticsService
import com.robert.api.service.DeploymentService
import com.robert.api.service.WorkerService
import com.robert.api.service.WorkflowService
import com.robert.exceptions.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(IgnoreTrailingSlash)

    val workerService = WorkerService()
    val workflowService = WorkflowService()
    val deploymentService = DeploymentService()
    val analyticsService = AnalyticsService()

    routing {
        route("/api") {
            route("/v1") {
                login()

                authenticate("auth-jwt") {
                    worker("/worker", workerService)
                    workflow("/workflow", workflowService)
                    deployment("/deployment", deploymentService)
                    analytics("/analytics", analyticsService)
                }
            }
        }

        singlePageApplication {
            applicationRoute = "/ui"
            useResources = true
            filesPath = "ui"
        }

        route("/") {
            get {
                call.respondRedirect("/ui")
            }
        }
    }

    install(StatusPages) {
        exception<ServerException> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError)
        }
        exception<ValidationException> { call, e ->
            call.respondText(e.message, ContentType.Text.Plain, HttpStatusCode.BadRequest)
        }
        exception<NotFoundException> { call, _ ->
            call.respond(HttpStatusCode.NotFound)
        }
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }
    }
}
