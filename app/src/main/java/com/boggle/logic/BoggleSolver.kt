package com.boggle.logic

import kotlin.text.iterator

class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isWord = false
}

class Trie {
    val root = TrieNode()

    fun insert(word: String) {
        var current = root;
        for (char in word) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        current.isWord = true
    }

}

class BoggleSolver(private val dictionary: Trie) {

    fun findAllWords(board: String): Set<String> {
        val foundWords = mutableSetOf<String>()
        val visited = BooleanArray(16)
        for (i in 0 until 16) {
            dfs(board, i, dictionary.root, "", visited, foundWords)
        }
        return foundWords
    }

    private fun dfs(
        board: String, pos: Int,
        node: TrieNode, currentWord: String,
        visited: BooleanArray, foundWords: MutableSet<String>
    ) {
        val char = board[pos]
        val nextNode = node.children[char] ?: return
        val word = currentWord + char

        if (nextNode.isWord && word.length >= 3)
            foundWords.add(word)

        visited[pos] = true
        var shifts = arrayOf(-5, -4, -3, -1, 1, 3, 4, 5)
        if (pos % 4 == 0) shifts = arrayOf(-4, -3, 1, 4, 5)
        if (pos % 4 == 3) shifts = arrayOf(-5, -4, -1, 3, 4)

        for (newShift in shifts){
            val newPos = pos + newShift
            if (newPos in 0 until 16 && !visited[newPos])
                dfs(board, newPos, nextNode, word, visited, foundWords)
        }
        visited[pos] = false
    }
}