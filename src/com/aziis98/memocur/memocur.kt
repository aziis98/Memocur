package com.aziis98.memocur

import java.nio.file.*

// Copyright 2016 Antonio De Lucreziis

const val validSymbols = "$'+-*/^<>=?_"

fun Char.isValidChar() = isLetter() || validSymbols.indexOf(this) != -1

fun stdGluer(a: Char, b: Char): Boolean =
    (a.isWhitespace() && b.isWhitespace())
        || (a.isValidChar() && b.isValidChar())
        || (a.isValidChar() && b.isDigit())
        || (a.isDigit() && b.isDigit())
        || ((a.isDigit() && b == '.') || (a == '.' && b.isDigit()))

//  "abc123 123§§123  abc"
// "abc123 123§§123  abc"
//
// "abc123"," ","123","§","§","123","  ","abc"
fun tokenize(source: CharArray, gluer: (Char, Char)-> Boolean = ::stdGluer): List<CharArray> {

    val list = mutableListOf<CharArray>()

    val sourceList = source.toList()

    val front = sourceList.take(source.size - 1)
    val back = sourceList.takeLast(source.size - 1)

    val zipped = front.zip(back).toMutableList()

    val sb = StringBuilder()
    while (zipped.isNotEmpty()) {
        val entry = zipped.removeAt(0)

        sb.append(entry.first)

        val glueFlag = gluer(entry.first, entry.second)

        if (!glueFlag) {
            list.add(sb.toString().toCharArray())
            sb.setLength(0)
        }

        if (zipped.isEmpty()) {
            sb.append(entry.second)

            list.add(sb.toString().toCharArray())
        }
    }


    return list
}

fun <T> MutableList<T>.pop() = removeAt(0)
fun <T> MutableList<T>.peek(at: Int = 0) = get(at)
fun <T> MutableList<T>.peek(fromIndex: Int, toIndex: Int) = subList(fromIndex, toIndex)

sealed class Expression {
    class Symbol(val name: String) : Expression()


}


class Pattern(val pattern: List<(Expression) -> Boolean>) {
    fun match(list: List<Expression>): Boolean {
        if (pattern.size != list.size) return false

        list.forEachIndexed { i, iExpression ->
            if (!pattern[i](iExpression)) {
                return false
            }
        }
        return true
    }
}

object Memocur {

    fun import(path: String) = import(Paths.get(path))

    fun import(path: Path) {

        val source = Files.readAllLines(path).joinToString("\n")

        val tokens = tokenize(source.toCharArray()).map { String(it) }.toMutableList()

        parseScript(tokens)
    }

    private fun parseScript(tokens: MutableList<String>) {

        while (tokens.isEmpty()) {
            when (tokens.peek()) {

                "[" -> parseExpression(tokens)

            }
        }

    }

    private fun parseExpression(tokens: MutableList<String>) {

    }

}

class MemocurScript() {

    val patterns = mutableListOf<Pair<Pattern, (List<Expression>) -> Expression>>()

    init {
        registerPatternFunction(Pattern(listOf<(Expression) -> Boolean>(
            { it is Expression.Symbol && it.name == "from" },
            { true },
            { it is Expression.Symbol && it.name == "to" },
            { true }
        ))) { it: List<Expression> ->

            val a = it[1].toString().toInt()
            val b = it[3].toString().toInt()

            return null

        }
    }

    fun registerPatternFunction(pattern: Pattern, fn: (List<Expression>) -> Expression) {
        patterns.add(pattern to fn)
    }

    fun evaluateFunctionCall(arguments: List<Expression>): Expression {
        return patterns.find { it.first.match(arguments) }?.second?.invoke(arguments) ?: error("No pattern matched: $arguments")
    }
}