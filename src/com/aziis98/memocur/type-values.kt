package com.aziis98.memocur

import java.util.*


// Copyright 2016 Antonio De Lucreziis


sealed class Type(val name: String, val weight: Int) {

    object All : Type("all", 0) {

    }

    object Nothing : Type("nothing", 0)

    object Symbol : Type("symbol", 1)
    object Number : Type("number", 1)
    object List : Type("list", 1)

    object Function : Type("function", 1)
    class Lambda(val paramCount: Int) : Type("function", 1) {
        override fun hashCode() = name.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other == null || (other !is Lambda && other !is Function)) return false

            if (other is Lambda)
                return paramCount == other.paramCount
            else
                return true
        }

        override fun toString(): String {
            return "${(1 .. paramCount).map { "?" }.joinToString(", ", "(", ")")}->?"
        }
    }

    object MObject : Type("object", 1)

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
fun valueListOf(list: List<Value>) = Value.List(list)
fun valueLambda(functionType: Type.Lambda, function: (List<Value>) -> Value) = Value.Function(functionType, function)

sealed class Value(val type: Type) {

    object Nothing : Value(Type.Nothing)

    class Symbol(val name: String) : Value(Type.Symbol) {
        companion object {
            val Nothing = Symbol("nothing")
        }

        override fun toString() = "$name"
    }

    class LambdaPlaceholder : Value(Type.Symbol) {
        override fun toString() = "_"
    }

    class Number(val value: Double) : Value(Type.Number) {
        override fun toString() = "$value"
    }

    class List(val list: kotlin.collections.List<Value>) : Value(Type.List) {
        override fun toString() = "[${list.joinToString(", ")}]"
    }

    class Function(val functionType: Type.Lambda, val function: (kotlin.collections.List<Value>) -> Value) : Value(functionType) {
        operator fun invoke(list: kotlin.collections.List<Value>) = function(list)

        override fun toString(): String {
            return "function($functionType)"
        }
    }
}

/*
sealed class Matcher(val type: Type) {
    open fun match(value: Value): Boolean {
        return type == value.type && matchValue(value)
    }

    protected abstract fun matchValue(value: Value): Boolean

    class MatchAny() : Matcher(Type.MObject) {
        override fun match(value: Value) = true

        override fun matchValue(value: Value) = true
    }

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

    class MatchFunction(val paramCount: Int = -1) : Matcher(Type.Function) {
        override fun matchValue(value: Value): Boolean {
            return paramCount == -1 || (value as Value.Function).functionType.paramCount == paramCount
        }

        override fun toString() = "function[${ if (paramCount == -1) "?" else "$paramCount" }]"
    }
}
*/

data class Matcher(val type: Type, val predicate: (Value) -> Boolean) {
    fun match(value: Value) = predicate(value)
}

fun matchFunction(paramCount: Int = -1) = Matcher(Type.Function) {
    it is Value.Function && (paramCount == -1 || it.functionType.paramCount == paramCount)
}

fun matchSymbol(name: String) = Matcher(Type.Symbol) {
    it is Value.Symbol && it.name == name
}

fun matchType(type: Type) = Matcher(type) {
    it.type == type
}

fun matchAll() = Matcher(Type.All) { true }
