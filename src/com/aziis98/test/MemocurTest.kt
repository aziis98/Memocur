package com.aziis98.test

import com.aziis98.memocur.*
import org.junit.Test
import printfRec

// Copyright 2016 Antonio De Lucreziis

class MemocurTest {

    @Test
    fun test() {

        val script = Memocur.import("workspace/script.memc")



        script.patternDefs.addSignature(
            FunctionSignature(listOf(
                Matcher.MatchSymbol("from"),
                Matcher.MatchType(Type.Number),
                Matcher.MatchSymbol("to"),
                Matcher.MatchSymbol("infinity")
            ), fnExecution { Value.Symbol.Nothing })
        )

        script.patternDefs.addSignature(
            FunctionSignature(listOf(
                Matcher.MatchSymbol("is"),
                Matcher.MatchType(Type.Number),
                Matcher.MatchSymbol("divisible"),
                Matcher.MatchSymbol("by"),
                Matcher.MatchType(Type.Number)
            ), fnExecution { Value.Symbol.Nothing })
        )

        script.patternDefs.heads.forEach { type, struct ->
            printfRec(struct) { fsb, struct, rec ->
                fsb.appendln("- ${struct.type}")
                fsb.indented {
                    struct.matchingDefs.forEach {
                        fsb.appendln("+ ${it.signature.map { it.toString() }.joinToString(" ")}")
                    }
                    struct.nexts.forEach { type, struct ->
                        rec(struct)
                    }
                }
            }
        }

        val arguments = listOf(
            Value.Symbol("from"),
            Value.Number(1.0),
            Value.Symbol("to"),
            Value.Number(10.0)
        )

        println("Found: ${script.evaluatePattern(arguments)}")
    }

    @Test
    fun testTokn() {
        val str = "abc123 abc§§  abc§"

        println(tokenize(str.toCharArray()).map { "\"" + String(it) + "\"" })
    }

}