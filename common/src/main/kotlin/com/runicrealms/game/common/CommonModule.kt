package com.runicrealms.game.common

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.spi.InjectionPoint
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CommonModule : AbstractModule() {

    @Provides
    fun provideLogger(injectionPoint: InjectionPoint): Logger {
        val clazz = injectionPoint.member.declaringClass
        val pkg = clazz.packageName

        val regex = Regex("""com\.runicrealms\.game\.([^.]+)\..+""")
        val match = regex.matchEntire(pkg)

        val loggerName =
            if (match != null) {
                val subsystem = match.groupValues[1].replaceFirstChar { it.uppercase() }
                subsystem
            } else {
                clazz.simpleName
            }

        return LoggerFactory.getLogger(loggerName)
    }
}
