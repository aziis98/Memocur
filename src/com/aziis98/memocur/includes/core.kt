package com.aziis98.memocur.includes

import com.aziis98.memocur.*

// Copyright 2016 Antonio De Lucreziis

object MemoCore {

    @Suppress("UNUSED_VARIABLE")
    val contexters = Memocur.context {

        addSignature(listOf(
            matchSymbol("do"), matchType(Type.List)
        )) {
            val (fn, list) = it

            val context = Memocur.context(this) { }

            return@addSignature list
        }

    }

    @Suppress("UNUSED_VARIABLE")
    val core = Memocur.context(contexters, MemoMath.math) {
        addSignature(listOf(
            matchSymbol("print"), matchAll()
        )) {
            val (fn, obj) = it

            print(obj.toString())

            return@addSignature obj
        }

        addSignature(listOf(
            matchSymbol("println"), matchAll()
        )) {
            val (fn, obj) = it

            println(obj.toString())

            return@addSignature obj
        }
    }

}
