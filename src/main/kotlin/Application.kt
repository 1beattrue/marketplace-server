package ru.mirea

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureDatabases()
    configureRouting()
}
