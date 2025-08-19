package com.careercoach

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class CareerCoachApplication

fun main(args: Array<String>) {
    runApplication<CareerCoachApplication>(*args)
}