// Copyright (c) Akop Karapetyan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.akop.ararat.io

import android.util.JsonReader
import org.akop.ararat.core.Crossword

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset


class UClickJsonFormatter : CrosswordFormatter {

    private var encoding = DEFAULT_ENCODING

    override fun setEncoding(encoding: String) {
        this.encoding = encoding
    }

    @Throws(IOException::class)
    override fun read(builder: Crossword.Builder, inputStream: InputStream) {
        val reader = JsonReader(inputStream.bufferedReader(Charset.forName(encoding)))

        var layout: Map<Int, Pair<Int, Int>>? = null
        var solution: Map<Pair<Int, Int>, String>? = null
        var acrossClues: Map<Int, String>? = null
        var downClues: Map<Int, String>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when (name) {
                "Author" -> builder.setAuthor(reader.nextString())
                "Title" -> builder.setTitle(reader.nextString())
                "Copyright" -> builder.setCopyright(reader.nextString())
                "Layout" -> layout = readLayout(reader)
                "Solution" -> solution = readSolution(reader)
                "AcrossClue" -> acrossClues = readClues(reader)
                "DownClue" -> downClues = readClues(reader)
                "Width" -> builder.width = reader.nextInt()
                "Height" -> builder.height = reader.nextInt()
//                "Date" -> user = readUser(reader)
//                "Editor" -> user = readUser(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (builder.width == 0 || builder.height == 0) {
            throw FormatException("Width (${builder.width}) or height(${builder.height}) not set")
        }

        if (layout == null) throw FormatException("Missing layout")
        if (solution == null) throw FormatException("Missing solution")
        if (acrossClues == null) throw FormatException("Missing clues for Across")
        if (downClues == null) throw FormatException("Missing clues for Down")

        acrossClues.forEach { (n, hint) ->
            val start = layout[n] ?: throw FormatException("No start position for $n Across")
            val wb = Crossword.Word.Builder()
                    .setNumber(n)
                    .setDirection(Crossword.Word.DIR_ACROSS)
                    .setHint(hint)
                    .setStartRow(start.first)
                    .setStartColumn(start.second)
            for (i in start.second until builder.width) {
                val sol = solution[Pair(start.first, i)]
                if (sol == " ") break
                wb.addCell(sol, 0)
            }
            builder.addWord(wb.build())
        }

        downClues.forEach { (n, hint) ->
            val start = layout[n] ?: throw FormatException("No start position for number $n Down")
            val wb = Crossword.Word.Builder()
                    .setNumber(n)
                    .setDirection(Crossword.Word.DIR_DOWN)
                    .setHint(hint)
                    .setStartRow(start.first)
                    .setStartColumn(start.second)
            for (i in start.first until builder.height) {
                val sol = solution[Pair(i, start.second)]
                if (sol == " ") break
                wb.addCell(sol, 0)
            }
            builder.addWord(wb.build())
        }
    }

    private fun readLayout(reader: JsonReader): Map<Int, Pair<Int, Int>> {
        val map = HashMap<Int, Pair<Int, Int>>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.matches("Line\\d+".toRegex()) -> {
                    val row = name.substring(4).toInt() - 1
                    val numbers = reader.nextString().chunked(2) { it.toString().toInt() }
                    numbers.forEachIndexed { i, n -> map[n] = Pair(row,i) }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return map
    }

    private fun readSolution(reader: JsonReader): Map<Pair<Int, Int>, String> {
        val map = HashMap<Pair<Int, Int>, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name.matches("Line\\d+".toRegex()) -> {
                    val row = name.substring(4).toInt() - 1
                    val letters = reader.nextString().chunked(1)
                    letters.forEachIndexed { i, s -> map[Pair(row,i)] = s }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return map
    }

    private fun readClues(reader: JsonReader): Map<Int, String> {
        val map = HashMap<Int, String>()
        reader.nextString().split("\n".toRegex()).forEach {
            val pair = it.split("\\|".toRegex(), limit = 2)
            if (pair.size == 2) map[pair[0].toInt()] = pair[1]
        }

        return map
    }

    @Throws(IOException::class)
    override fun write(crossword: Crossword, outputStream: OutputStream) {
        throw UnsupportedOperationException("Writing not supported")
    }

    override fun canRead(): Boolean {
        return true
    }

    override fun canWrite(): Boolean {
        return false
    }

    companion object {
        private const val DEFAULT_ENCODING = "UTF-8"
    }
}