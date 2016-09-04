package com.aziis98.memocur

import java.util.*


// Copyright 2016 Antonio De Lucreziis


sealed class Type(val name: String) {
    object Symbol : Type("symbol")
    object Number : Type("number")
    object List : Type("list")

    class Function(val paramCount: Int) : Type("function") {
        override fun hashCode() = Objects.hash(name, paramCount)
        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Function) return false

            return other.name == name && other.paramCount == paramCount
        }

        override fun toString(): String {
            return "(${ (1 .. paramCount).map { "?" }.joinToString(", ") }) -> ?"
        }
    }

    object MObject : Type("object")

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other != null && other is Type) {
            return other.name == name
        }
        return false
    }

    override fun toString() = "$name"
}

fun valueSymbol(name: String) = Value.Symbol(name)
fun valueLambdaPlaceholder() = Value.LambdaPlaceholder()
fun valueNumber(number: Number) = Value.Number(number.toDouble())
fun valueListOf(list: List<Value>) = Value.MList(list)
fun valueLambda(functionType: Type.Function, function: (List<Value>) -> Value) = Value.Function(functionType, function)

sealed class Value(val type: Type) {
    class Symbol(val name: String) : Value(Type.Symbol) {
        companion object {
            val Nothing = Symbol("nothing")
        }

        override fun toString() = ":$name"
    }

    class LambdaPlaceholder : Value(Type.Symbol) {
        override fun toString() = "_"
    }

    class Number(val value: Double) : Value(Type.Number) {
        override fun toString() = "$value"
    }

    class MList(val list: List<Value>) : Value(Type.List) {
        override fun toString() = "[${list.joinToString(", ")}]"
    }

    class Function(val functionType: Type.Function, val function: (List<Value>) -> Value) : Value(functionType) {
        operator fun invoke(list: List<Value>) = function(list)

        override fun toString(): String {
            return "function $functionType"
        }
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

    class MatchFunction(val paramCount: Int = -1) : Matcher(Type.Function(paramCount)) {
        override fun matchValue(value: Value): Boolean {
            return paramCount >= 0 && (value as Value.Function).functionType.paramCount == paramCount
        }

        override fun toString() = "function[${ if (paramCount == -1) "?" else "$paramCount" }]"
    }
}

fun matchFunction(paramCount: Int = -1) = Matcher.MatchFunction(paramCount)
fun matchSymbol(name: String) = Matcher.MatchSymbol(name)
fun matchType(type: Type) = Matcher.MatchType(type)
