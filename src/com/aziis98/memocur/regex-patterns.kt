package com.aziis98.memocur

// Copyright 2016 Antonio De Lucreziis

object RegexPatterns {

    val NUMBER = """(\d*)\.?\d+""".toRegex()
    val SYMBOL = """[a-zA-Z${Regex.escape(validSymbols)}][a-zA-Z0-9${Regex.escape(validSymbols)}]*""".toRegex()

}