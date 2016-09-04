package com.aziis98.test

import com.aziis98.memocur.*
import org.junit.Test
import java.nio.file.Paths

// Copyright 2016 Antonio De Lucreziis

class MemocurTest {

    @Test
    fun test() {

        val script = Memocur.import("workspace/script.memc")

    }

    @Test
    fun testTokn() {
        val str = "abc123 abc§§  abc§"

        println(tokenize(str.toCharArray()).map { "\"" + String(it) + "\"" })
    }

}