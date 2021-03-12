package org.akop.crosswordtest


import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout

class CustomKeyboard(context: Context, attr: AttributeSet) : LinearLayout(context, attr) {

    fun create(onKeyClicked: (Char) -> Unit, keys: List<String> = QWERT_KEYS) {

        keys.forEach { row ->
            val rowLayout = LinearLayout(this.context)
            rowLayout.layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
            )
            rowLayout.orientation = LinearLayout.HORIZONTAL

            row.forEach { key ->
                val button = Button(this.context)
                button.layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                        1f
                )

                button.text = key.toString()
                button.setOnClickListener {
                    onKeyClicked(key)
                }

                rowLayout.addView(button)
            }

            addView(rowLayout)
        }
    }

    companion object {
        val QWERT_KEYS = listOf(
                "qwertyuiop",
                "asdfghjkl",
                "zxcvbnm<"
        )
    }
}