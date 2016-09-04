package com.aziis98.test

import com.aziis98.memocur.Memocur
import java.util.*

// Copyright 2016 Antonio De Lucreziis

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)

    // while (true) {

        print("$ ")
        val expression = scanner.nextLine()

        // Compute the expression
        val result = Memocur.evaluateExpression(expression).toString()

        println(" => $result")

    // }
}