package com.aziis98.memocur.includes

import com.aziis98.memocur.*

// Copyright 2016 Antonio De Lucreziis

object MemoMath {

    inline fun MemocurContext.addOperatorSignature(symbol: String, crossinline fn: (Value, Value) -> Value) {
        addSignature(listOf(
            matchType(Type.Number), matchSymbol(symbol), matchType(Type.Number)
        )) {
            val (a, op, b) = it

            return@addSignature fn(a, b)
        }
    }

    val math = MemocurContext().apply {
        addOperatorSignature("+") { a, b ->
            a as Value.Number
            b as Value.Number
            return@addOperatorSignature valueNumber(a.value + b.value)
        }
        addOperatorSignature("-") { a, b ->
            a as Value.Number
            b as Value.Number
            return@addOperatorSignature valueNumber(a.value - b.value)
        }
        addOperatorSignature("*") { a, b ->
            a as Value.Number
            b as Value.Number
            return@addOperatorSignature valueNumber(a.value * b.value)
        }
        addOperatorSignature("/") { a, b ->
            a as Value.Number
            b as Value.Number
            return@addOperatorSignature valueNumber(a.value / b.value)
        }
        addOperatorSignature("%") { a, b ->
            a as Value.Number
            b as Value.Number
            return@addOperatorSignature valueNumber(a.value % b.value)
        }
    }

}
