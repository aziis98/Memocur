package com.aziis98.memocur

import java.util.*

// Copyright 2016 Antonio De Lucreziis

class PatternDefinitions {

    val heads = LinkedList<FnDefPatternStruct>()

    fun addSignature(list: List<Matcher>, fn: (List<Value>) -> Value) {
        addSignature(FunctionSignature(list, fn))
    }

    fun addSignature(functionSignature: FunctionSignature) {

        fun addRecursive(fnDefPatternStruct: FnDefPatternStruct,
                         index: Int) {

            if (functionSignature.signature.lastIndex == index) {
                val present = fnDefPatternStruct.matchingDefs.find { it.signature.map { it.type } == functionSignature.signature.map { it.type } }

                if (present != null) {
                    println("[UPDATEING] $functionSignature")
                    fnDefPatternStruct.matchingDefs.remove(present)
                }

                fnDefPatternStruct.matchingDefs.add(functionSignature)
                return
            }
            else {
                val cType = functionSignature.signature[index + 1].type

                var nextBranch = fnDefPatternStruct.nexts.find { it.type == cType }

                if (nextBranch == null) {
                    nextBranch = FnDefPatternStruct(cType)
                    fnDefPatternStruct.nexts.add(nextBranch)
                }

                addRecursive(nextBranch, index + 1)
            }

        }

        val firstSignatureType = functionSignature.signature[0].type
        var startBranch = heads.find { it.type == firstSignatureType }

        if (startBranch == null) {
            startBranch = FnDefPatternStruct(firstSignatureType)
            heads.add(startBranch)
        }

        addRecursive(startBranch, 0)

    }

    fun getFunctionSignature(arguments: List<Value>): FunctionSignature? {

        // val errorFn = { error("No pattern found matching: ${arguments.joinToString(" ", "{", "}")}") }

        fun getRecursive(defPatternStruct: FnDefPatternStruct, index: Int): FunctionSignature? {
            if (arguments.lastIndex == index) {
                return defPatternStruct.matchingDefs.find { it.test(arguments) } ?: null
            }
            else {
                val defPatternStruct1 = defPatternStruct.nexts.getBestMatch(arguments[index + 1].type) ?: return null

                return getRecursive(defPatternStruct1, index + 1)
            }
        }

        val startBranch = heads.getBestMatch(arguments[0].type) ?: return null
        return getRecursive(startBranch, 0)

    }

    fun List<FnDefPatternStruct>.getBestMatch(type: Type): FnDefPatternStruct? {
        return sortedByDescending { it.type.weight }
            .find { it.type == type || type.weight > it.type.weight }
    }

}

class FnDefPatternStruct(val type: Type) {
    val nexts = LinkedList<FnDefPatternStruct>()
    val matchingDefs = LinkedList<FunctionSignature>()
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

