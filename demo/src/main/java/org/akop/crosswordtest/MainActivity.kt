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

package org.akop.crosswordtest

import android.graphics.Color
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

import org.akop.ararat.core.Crossword
import org.akop.ararat.core.buildCrossword
import org.akop.ararat.core.buildWord
import org.akop.ararat.io.PuzFormatter
import org.akop.ararat.view.CrosswordView


// Crossword: Double-A's by Ben Tausig
// http://www.inkwellxwords.com/iwxpuzzles.html
class MainActivity : AppCompatActivity(),
        CrosswordView.OnLongPressListener,
        CrosswordView.OnStateChangeListener,
        CrosswordView.OnSelectionChangeListener {

    private var crosswordView: CrosswordView? = null
    private var hint: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        crosswordView = findViewById(R.id.crossword)

        //hint = findViewById(R.id.hint)

        findViewById<Button>(R.id.button1).setOnClickListener {
            Log.d("TAG", "${crosswordView?.hasMarkedCheated()}")
        }

        val crossword = readPuzzle(R.raw.puzzle)
        val customCrossword = buildCrossword {
            height = 18
            width = 20
            words.addAll(
                    listOf(
                            buildWord {
                                direction = Crossword.Word.DIR_ACROSS
                                number = 1
                                startRow = 0
                                startColumn = 4
                                "aaaaaa".toCharArray().forEach { addCell(it) }
                            },
                            buildWord {
                                direction = Crossword.Word.DIR_DOWN
                                number = 2
                                startRow = 0
                                startColumn = 6
                                "allde".toCharArray().forEach { addCell(it) }
                            },
                            buildWord {
                                direction = Crossword.Word.DIR_ACROSS
                                number = 3
                                startRow = 2
                                startColumn = 5
                                "lllllllllll".toCharArray().forEach { addCell(it) }
                            },
                            buildWord {
                                direction = Crossword.Word.DIR_DOWN
                                number = 4
                                startRow = 0
                                startColumn = 9
                                "aallbb".toCharArray().forEach { addCell(it) }
                            }
                    )
            )
        }

        crosswordView!!.let { cv ->
            cv.autoOpenKeyboard = true
            cv.crossword = customCrossword
            cv.onLongPressListener = this
            cv.onStateChangeListener = this
            cv.onSelectionChangeListener = this
            cv.onKeyPressedListener = object : CrosswordView.OnKeyPressedListener {
                override fun onPressed() {
                    playSound(R.raw.tick)
                }
            }
            cv.inputValidator = { ch -> !ch.first().isISOControl() }
            cv.undoMode = CrosswordView.UNDO_NONE
            cv.markerDisplayMode = CrosswordView.MARKER_CHEAT

            cv.roundedRectEnabled = true
            cv.roundedRectTop = 4f
            cv.roundedRectRight = 4f
            cv.selectedStrokeWidth = 2f
            cv.strokeWidth = 2f
            cv.renderNumberEnabled = false
            cv.cellPadding = 4f
            cv.backgroundColor = Color.parseColor("#FFFFFF")
            cv.markedFillFullCellEnabled = true
            cv.customMarkerForCorrectChecked = true
            cv.clearFlagsOnEditCell = true

            onSelectionChanged(cv, cv.selectedWord, cv.selectedCell)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        crosswordView!!.restoreState(savedInstanceState.getParcelable("state")!!)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable("state", crosswordView!!.state)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_restart -> {
                crosswordView?.isEditable = true
                crosswordView!!.reset()
                return true
            }
            R.id.menu_solve_cell -> {
                crosswordView!!.solveChar(crosswordView!!.selectedWord!!,
                        crosswordView!!.selectedCell)
                return true
            }
            R.id.menu_solve_word -> {
                crosswordView!!.solveWord(crosswordView!!.selectedWord!!)
                return true
            }
            R.id.menu_solve_puzzle -> {
                crosswordView!!.solveCrossword(showAnswers = false)
                Log.d("TAG", "solved on clicked = ${crosswordView!!.isSolved()}")
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCellLongPressed(view: CrosswordView,
                                   word: Crossword.Word, cell: Int) {
        Toast.makeText(this, "Show popup menu for " + word.hint!!,
                Toast.LENGTH_SHORT).show()
    }

    override fun onCrosswordChanged(view: CrosswordView) {
        Log.d("TAG", "all not empty = ${view.isAllCellsNotEmpty()}")
    }

    override fun onCrosswordSolved(view: CrosswordView) {
        crosswordView?.isEditable = false
        Toast.makeText(this, R.string.youve_solved_the_puzzle,
                Toast.LENGTH_SHORT).show()
    }

    override fun onCrosswordWordSolved(view: CrosswordView) {
        playSound(R.raw.pilim)
    }

    private fun playSound(resId: Int) {
        MediaPlayer.create(this, resId).start()
    }

    override fun onCrosswordUnsolved(view: CrosswordView) {}

    private fun readPuzzle(@RawRes resourceId: Int): Crossword =
            resources.openRawResource(resourceId).use { s ->
                buildCrossword { PuzFormatter().read(this, s) }
            }

    override fun onSelectionChanged(view: CrosswordView,
                                    word: Crossword.Word?, position: Int) {

        /*hint!!.text = when (word?.direction) {
            Crossword.Word.DIR_ACROSS -> getString(R.string.across, word.number, word.hint)
            Crossword.Word.DIR_DOWN -> getString(R.string.down, word.number, word.hint)
            else -> ""
        }*/
    }
}
