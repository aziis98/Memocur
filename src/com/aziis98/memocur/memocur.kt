package com.aziis98.memocur

import java.nio.file.*
import java.util.*
import java.util.regex.Pattern

// Copyright 2016 Antonio De Lucreziis

const val validSymbols = "$'+-*/^<>=?_."

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

object Memocur {

    val basicContext = MemocurContext().apply {

    }

    fun import(path: String) = import(Paths.get(path))

    fun import(path: Path): MemocurContext {

        val source = Files.readAllLines(path).joinToString("\n")

        val tokens = tokenize(source.toCharArray()).map { String(it) }.toMutableList()

        val memocurScript = MemocurContext()

        parseScript(tokens, memocurScript)

        return memocurScript
    }

    fun evaluateExpression(expression: String, context: MemocurContext = basicContext) : Value {
        val tokens = tokenize(expression.toCharArray())
            .map { String(it) }
            .toMutableList()

        parseExpression(tokens, context)

        return Value.Symbol("nothing")
    }

    private fun parseScript(tokens: MutableList<String>, context: MemocurContext) {

        while (tokens.isEmpty()) {
            when (tokens.peek()) {

                "[" -> parseExpression(tokens, context)

            }
        }

    }

    private fun parseExpression(tokens: MutableList<String>, context: MemocurContext) {
        
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

sealed class Type(val name: String) {
    object Symbol : Type("symbol")
    object Number : Type("number")
    object List : Type("list")
    
    object MObject : Type("object")

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other != null && other is Type) {
            return other.name == name
        }
        return false
    }

    override fun toString() = "[$name]"
}

sealed class Value(val type: Type) {
    class Symbol(val name: String) : Value(Type.Symbol) {
        companion object {
            val Nothing = Symbol("nothing")
        }

        override fun toString() = "Symbol($name)"
    }
    class Number(val value: Double) : Value(Type.Number) {
        override fun toString() = "Number($value)"
    }
    class MList(val list: List<Value>) : Value(Type.List) {
        override fun toString() = "List(${list.joinToString(", ")})"
    }
}

sealed class Matcher(val type: Type) {
    fun match(value: Value): Boolean {
        return type == value.type && matchValue(value)
    }

    protected abstract fun matchValue(value: Value): Boolean

    class MatchSymbol(val name: String) : Matcher(Type.Symbol) {
        override fun matchValue(value: Value): Boolean {
            return name == (value as Value.Symbol).name
        }

        override fun toString() = "$name"
    }

    class MatchType(type: Type) : Matcher(type) {
        override fun matchValue(value: Value) = true

        override fun toString() = "$type"
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