package com.lhadalo.oladahl.rapporteringkotlin.utilities

import android.view.View
import android.view.ViewGroup

/**
 * Extention method for ViewGroup
 */
inline fun ViewGroup.forEach(action: (View) -> Unit) {
    for (index in 0 until childCount) {
        action(getChildAt(index))
    }
}

inline fun ViewGroup.forEach(action: (View, Int) -> Unit) {
    for (index in 0 until childCount) {
        action(getChildAt(index), index)
    }
}
