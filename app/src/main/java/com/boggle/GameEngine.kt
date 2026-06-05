package com.boggle

import android.graphics.RenderNode
import kotlin.random.Random

class GameEngine {
    private var allPossibleWords = setOf<String>()
    val foundWords = mutableSetOf<String>()
    val opponentWords = mutableSetOf<String>()
    var roundTimeSecond = 30
    val opponentScore = opponentWords.sumOf { calculateScore(it) }
    var board = String()

    var currentScore: Int = 0
        private set

    fun getAllWords() : Set<String> {
        return allPossibleWords
    }
    fun setAllPossibleWords(words: Set<String>) {
        allPossibleWords = words
    }

    fun setNewGame(){
        resetGame()
        board = makeBoard()
    }

    fun checkWord(word: String) : Boolean {
        if (allPossibleWords.contains(word) && !foundWords.contains(word)) {
            foundWords.add(word)
            currentScore += calculateScore(word)
            return true
        }
        return false
    }

    fun calculateScore(word: String) : Int {
        // TODO: przełączanie trybu punktacji
        return word.length - 2
    }


    fun resetGame(){
        foundWords.clear()
        currentScore = 0
    }

    // funkcje i zmienne do generowania planszy
    private val vowels = mapOf(
        'A' to 89, 'I' to 82, 'O' to 77, 'E' to 76, 'Y' to 37, 'U' to 25, 'Ę' to 11, 'Ą' to 10, 'Ó' to 8
    )

    private val consonants = mapOf(
        'Z' to 56, 'N' to 55, 'R' to 46, 'W' to 46, 'S' to 43, 'T' to 39, 'C' to 39,
        'K' to 35, 'D' to 32, 'P' to 31, 'M' to 28, 'J' to 22, 'L' to 21, 'B' to 14,
        'G' to 14, 'H' to 10, 'F' to 3, 'Ł' to 18, 'Ś' to 8, 'Ż' to 7, 'Ć' to 4, 'Ń' to 2, 'Ź' to 1
    )

    private fun getRandomLetter(pool: Map<Char, Int>, randomGen: Random): Char {
        val totalWeight = pool.values.sum()
        var random = randomGen.nextInt(totalWeight)
        for ((letter, weight) in pool) {
            random -= weight
            if (random < 0) {
                return letter
            }
        }
        return 'X' // ERROR
    }

    fun makeBoard() : String{
        val randomGen = Random.Default
//        val randomGen = Random(1000000007)
        val board = mutableListOf<Char>()

        val vowelCount = randomGen.nextInt(5, 8)
        val consonantCount = 16 - vowelCount

        repeat(vowelCount) {
            board.add(getRandomLetter(vowels, randomGen))
        }

        repeat(consonantCount) {
            board.add(getRandomLetter(consonants, randomGen))
        }

        board.shuffle(randomGen)

        return board.toCharArray().concatToString()

//        return  "EFOĆ" +
//                "APIG" +
//                "DAWO" +
//                "INTB"
    }
//
//    private val dices = listOf(
//        "AEINRT",
//        "AEINST",
//        "AEIORW",
//        "AEOPSZ",
//        "ACDKLM",
//        "BCGHNP",
//        "DGHMOU",
//        "IŁPRSY",
//        "EĘKNRW",
//        "CĆLŚTZ",
//        "AIOWYZ",
//        "NÓPRTY",
//        "AEJŁRW",
//        "IUMPZB",
//        "FHJŻŹA",
//        "ABCDEŚ",
//    )
//
//    fun makeBoard() : String{
//        val randomGen = Random.Default
////        val randomGen = Random(1000000007)
//        val board = mutableListOf<Char>()
//
//        // 1. Mieszamy kostki
//        val shuffledDice = dices.shuffled(randomGen)
//
//        // 2. Rzucamy każdą z 16 kostek
//        for (die in shuffledDice) {
//            val faceIndex = randomGen.nextInt(6)
//            board.add(die[faceIndex])
//        }
//
//        return board.toCharArray().concatToString()
//    }

}