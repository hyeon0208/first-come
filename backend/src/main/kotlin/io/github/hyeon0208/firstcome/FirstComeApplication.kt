package io.github.hyeon0208.firstcome

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FirstComeApplication

fun main(args: Array<String>) {
    runApplication<FirstComeApplication>(*args)
}
