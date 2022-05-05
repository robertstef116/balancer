package com.robert.routes

import com.robert.Constants
import com.robert.UpdateAwareService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.changes(path: String, services: List<UpdateAwareService>) {

    route(path) {
        get {
            val changes = HashMap<String, Any>()
            for(service in services) {
                changes[service.key] = service.getVersion()
            }
            changes[Constants.HASH_KEY] = Constants.HASH
            call.respond(changes)
        }
    }
}
