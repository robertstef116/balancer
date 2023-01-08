package com.robert.plugins

import com.robert.exceptions.*
import com.robert.exceptions.NotFoundException
import com.robert.routes.*
import com.robert.services.*
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
    val configService = ConfigService()
    val analyticsService = AnalyticsService()

    routing {
        route("/api") {
            route("/v1") {
                login()

                authenticate("auth-jwt") {
                    config("/config", configService)
                    worker("/worker", workerService)
                    workflow("/workflow", workflowService)
                    deployment("/deployment", deploymentService)
                    analytics("/analytics", analyticsService)
                }
            }
        }

//        // Static plugin. Try to access `/static/index.html`
//        static("/ui") {
//            resources("static")
//        }

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
