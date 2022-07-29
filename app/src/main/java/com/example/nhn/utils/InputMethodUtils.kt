package com.example.nhn.utils

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

object InputMethodUtils {
    fun hideInputMethod(context: Context?, editText: EditText) {
        context?.let { c ->
            val imm = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
            editText.clearFocus()
        }
    }
}