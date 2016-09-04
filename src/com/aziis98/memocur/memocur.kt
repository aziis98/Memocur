package com.aziis98.memocur

import java.nio.file.*
import java.util.*

// Copyright 2016 Antonio De Lucreziis

const val validSymbols = "$'+-*/^<>=?_.%"

fun Char.isValidChar() = isLetter() || validSymbols.indexOf(this) != -1

fun stdGluer(a: Char, b: Char): Boolean =
    (a.isWhitespace() && b.isWhitespace())
        || (a.isValidChar() && b.isValidChar())
        || (a.isValidChar() && b.isDigit())
        || (a.isDigit() && b.isDigit())
        || ((a.isDigit() && b == '.') || (a == '.' && b.isDigit()))

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

object Memocur {

    @Suppress("UNUSED_VARIABLE")
    val basicContext = MemocurContext().apply {
        addSignature(
            FunctionSignature(listOf(
                matchSymbol("from"),
                matchType(Type.Number),
                matchSymbol("to"),
                matchType(Type.Number)
            ), fnExecution {
                val (from, a, to, b) = it

                val aBound = (a as Value.Number).value.toInt()
                val bBound = (b as Value.Number).value.toInt()

                val intRange = aBound .. bBound

                return@fnExecution Value.MList(
                    intRange
                        .map { valueNumber(it) }
                        .toList()
                )
            })
        )

        addSignature(FunctionSignature(listOf(
                matchType(Type.Number),
                matchSymbol("+"),
                matchType(Type.Number)
            ), fnExecution {
                val (a, plus, b) = it

                a as Value.Number
                b as Value.Number

                return@fnExecution valueNumber(
                    a.value + b.value
                )
            })
        )

        addSignature(FunctionSignature(listOf(
                matchType(Type.Number),
                matchSymbol("-"),
                matchType(Type.Number)
            ), fnExecution {
            val (a, minus, b) = it

            a as Value.Number
            b as Value.Number

            return@fnExecution valueNumber(
                a.value - b.value
            )
        })
        )

        addSignature(FunctionSignature(listOf(
                matchType(Type.Number),
                matchSymbol("*"),
                matchType(Type.Number)
            ), fnExecution {
            val (a, times, b) = it

            a as Value.Number
            b as Value.Number

            return@fnExecution valueNumber(
                a.value * b.value
            )
        })
        )

        addSignature(FunctionSignature(listOf(
                matchType(Type.Number),
                matchSymbol("/"),
                matchType(Type.Number)
            ), fnExecution {
            val (a, divide, b) = it

            a as Value.Number
            b as Value.Number

            return@fnExecution valueNumber(
                a.value / b.value
            )
        })
        )

        addSignature(FunctionSignature(listOf(
                matchType(Type.Number),
                matchSymbol("%"),
                matchType(Type.Number)
            ), fnExecution {
            val (a, mod, b) = it

            a as Value.Number
            b as Value.Number

            return@fnExecution valueNumber(
                a.value % b.value
            )
        })
        )


        addSignature(FunctionSignature(listOf(
            matchSymbol("call"),
            matchFunction(),
            matchType(Type.List)
        ), fnExecution {
            val (call, fn, args) = it

            fn as Value.Function
            args as Value.MList

            return@fnExecution fn(args.list)
        })
        )
    }

    fun import(path: String) = import(Paths.get(path))

    fun import(path: Path): MemocurContext {

        val source = Files.readAllLines(path).joinToString("\n")

        val tokens = tokenize(source.toCharArray()).map { String(it) }.toMutableList()

        val memocurScript = MemocurContext(basicContext)

        // parseScript(tokens, memocurScript)

        return memocurScript
    }

    fun evaluateExpression(expression: String, context: MemocurContext = basicContext) : Value {
        val tokens = tokenize(expression.toCharArray())
            .map { String(it) }
            .filter { !it.isBlank() }
            .toMutableList()

        val astRoot = parseExpression(tokens)

        // println("Abstract Syntaxt Tree : $astRoot")

        return evaluateASTElement(astRoot, context)
    }

    fun evaluateASTElement(element: ASTElement, context: MemocurContext): Value {
        return when (element) {

            is ASTElement.Leaf.Number -> valueNumber(element.source.toDouble())

            is ASTElement.Leaf.Symbol -> valueSymbol(element.source)

            is ASTElement.Leaf.LambdaPlaceholder -> valueLambdaPlaceholder()

            is ASTElement.List -> valueListOf(element.list.map { evaluateASTElement(it, context) })

            is ASTElement.Call -> context.evaluatePattern(element.list.map { evaluateASTElement(it, context) })

            is ASTElement.Lambda -> evaluateLambda(element, context)

            else -> error("Unable to evaluate: $element")
        }
    }

    fun evaluateLambda(element: ASTElement.Lambda, context: MemocurContext) : Value.Function {
        val paramCount = element.list.count { it is ASTElement.Leaf.LambdaPlaceholder }

        val precomp = element.list.map {
            evaluateASTElement(it, context)
        }

        return Value.Function(Type.Function(paramCount)) { arguments ->
            if (arguments.size != paramCount) error("Called $precomp with ${arguments.size} arguments instead of $paramCount")

            val actualParams = mutableListOf<Value>()

            var i = 0

            for (value in precomp) {
                if (value is Value.LambdaPlaceholder) {
                    actualParams.add(arguments[i])
                    i++
                }
                else {
                    actualParams.add(value)
                }
            }

            context.evaluatePattern(actualParams)
        }
    }

    private fun parseExpression(tokens: MutableList<String>) : ASTElement {
        val next = tokens.peek()

        return when {

            next == "{" ->
                parseFunctionCall(tokens)

            next == "[" ->
                parseList(tokens)

            next == "_" ->
                parseLambdaPlaceholder(tokens)

            RegexPatterns.NUMBER.matches(next) ->
                parseNumber(tokens)

            RegexPatterns.SYMBOL.matches(next) ->
                parseSymbol(tokens)

            else -> error("Unable to parse: \"$next\"")
        }
    }

    private fun parseFunctionCall(tokens: MutableList<String>) : ASTElement {
        val list = mutableListOf<ASTElement>()
        var isLambda = false

        tokens.pop()

        while (tokens.peek() != "}") {
            val expression = parseExpression(tokens)

            if (expression is ASTElement.Leaf.LambdaPlaceholder) isLambda = true

            list.add(expression)
        }

        tokens.pop()

        if (isLambda) {
            return ASTElement.Lambda(list)
        }
        else {
            return ASTElement.Call(list)
        }
    }

    private fun parseList(tokens: MutableList<String>): ASTElement.List {
        val list = mutableListOf<ASTElement>()

        tokens.pop()

        while (tokens.peek() != "]") {
            list.add(parseExpression(tokens))
        }

        tokens.pop()

        return ASTElement.List(list)
    }

    private fun parseLambdaPlaceholder(tokens: MutableList<String>): ASTElement.Leaf.LambdaPlaceholder {
        tokens.pop()
        return ASTElement.Leaf.LambdaPlaceholder()
    }

    private fun parseNumber(tokens: MutableList<String>) : ASTElement.Leaf.Number {
        return ASTElement.Leaf.Number(tokens.pop())
    }

    private fun parseSymbol(tokens: MutableList<String>) : ASTElement.Leaf.Symbol {
        return ASTElement.Leaf.Symbol(tokens.pop())
    }

}

sealed class ASTElement(val list: kotlin.collections.List<ASTElement>) {
    sealed class Leaf(val source: String) : ASTElement(emptyList()) {
        class Number(source: String) : Leaf(source)
        class Symbol(source: String) : Leaf(source)
        class LambdaPlaceholder : Leaf("_")

        override fun toString() = "Leaf($source)"
    }
    class Lambda(list: kotlin.collections.List<ASTElement>) : ASTElement(list) {
        override fun toString() = list.joinToString(", ", "Lambda(", ")")
    }
    class Call(list: kotlin.collections.List<ASTElement>) : ASTElement(list) {
        override fun toString() = list.joinToString(", ", "FunctionCall(", ")")
    }
    class List(list: kotlin.collections.List<ASTElement>) : ASTElement(list) {
        override fun toString() = list.joinToString(", ", "List(", ")")
    }
}


class MemocurContext(val parent: MemocurContext? = null) {

    val patternDefs = PatternDefinitions()

    fun addSignature(functionSignature: FunctionSignature) {
        patternDefs.addSignature(functionSignature)
    }

    fun getFunctionSignature(arguments: List<Value>): FunctionSignature {
        return patternDefs.getFunctionSignature(arguments)
    }

    fun evaluatePattern(arguments: List<Value>): Value {
        return getFunctionSignature(arguments).functionExecution.execute(arguments)
    }
    
}

class FunctionSignature(val signature: List<Matcher>, val functionExecution: FunctionExecution) {
    fun test(expression: List<Value>): Boolean {
        expression.forEachIndexed { i, value ->
            if (!signature[i].match(value))
                return false
        }
        return true
    }
}

inline fun fnExecution(crossinline fn: (List<Value>) -> Value): FunctionExecution {
    return object : FunctionExecution() {
        override fun execute(expression: List<Value>): Value {
            return fn(expression)
        }
    }
}

abstract class FunctionExecution() {
    abstract fun execute(expression: List<Value>) : Value
}

/*

1 - { from #number to #number }
2 - { from #number to #number stepping by #number }
3 - { from #number to infinity }
4 - { from #number to infinity stepping by #number }

- Symbol
  - Number
    - Symbol
      - Number
        + 1
        - Symbol
          -Symbol
            - Number
              + 2
      - Symbol
        + 3
        - Symbol
          -Symbol
            - Number
              + 4
            

 */

class PatternDefinitions {

    val heads = HashMap<Type, FnDefPatternStruct>()

    fun addSignature(functionSignature: FunctionSignature) {

        fun addRecursive(fnDefPatternStruct: FnDefPatternStruct,
                         index: Int) {

            if (functionSignature.signature.lastIndex == index) {
                fnDefPatternStruct.matchingDefs.add(functionSignature)
                return
            }
            else {
                val cType = functionSignature.signature[index + 1].type
                var nextBranch = fnDefPatternStruct.nexts[cType]

                if (nextBranch == null) {
                    nextBranch = FnDefPatternStruct(cType)
                    fnDefPatternStruct.nexts.put(cType, nextBranch)
                }

                addRecursive(nextBranch, index + 1)
            }

        }

        val firstSignatureType = functionSignature.signature[0].type
        var startBranch = heads[firstSignatureType]

        if (startBranch == null) {
            startBranch = FnDefPatternStruct(firstSignatureType)
            heads.put(firstSignatureType, startBranch)
        }

        addRecursive(startBranch, 0)

    }

    fun getFunctionSignature(arguments: List<Value>): FunctionSignature {

        val errorFn = { error("No pattern found matching: $arguments") }

        fun getRecursive(defPatternStruct: FnDefPatternStruct, index: Int) : FunctionSignature {
            if (arguments.lastIndex == index) {
                return defPatternStruct.matchingDefs.find { it.test(arguments) } ?: errorFn()
            }
            else {
                return getRecursive(defPatternStruct.nexts[arguments[index + 1].type] ?: errorFn(), index + 1)
            }
        }

        val startBranch = heads[arguments[0].type] ?: errorFn()
        return getRecursive(startBranch, 0)

    }

}

class FnDefPatternStruct(val type: Type) {
    val nexts = HashMap<Type, FnDefPatternStruct>()
    val matchingDefs = LinkedList<FunctionSignature>()
}