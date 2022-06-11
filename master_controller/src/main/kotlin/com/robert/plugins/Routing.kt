package com.robert.plugins

import com.robert.exceptions.*
import com.robert.exceptions.NotFoundException
import com.robert.routes.*
import com.robert.services.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*

fun Application.configureRouting() {
    val workerService = WorkerService()
    val workflowService = WorkflowService()
    val deploymentService = DeploymentService()
    val configService = ConfigService()
    val analyticsService = AnalyticsService()

    routing {
        login()

//        authenticate("auth-jwt") {
        config("/config", configService)
        worker("/worker", workerService)
        workflow("/workflow", workflowService)
        deployment("/deployment", deploymentService)
        analytics("/analytics", analyticsService)
//        }

        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }

        install(StatusPages) {
            exception<ServerException> {
                call.respond(HttpStatusCode.InternalServerError)
            }
            exception<ValidationException> {
                call.respondText(it.message, ContentType.Text.Plain, HttpStatusCode.BadRequest)
            }
            exception<NotFoundException> {
                call.respond(HttpStatusCode.NotFound)
            }
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}
