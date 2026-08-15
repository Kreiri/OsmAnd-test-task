package com.example.osmandtesttask.ui.common.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.LAYOUT_DIRECTION_LTR
import android.view.View.LAYOUT_DIRECTION_RTL

@SuppressLint("ResourceType")
fun View.resolvePaddings(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0,
    defaultStart: Int = 0, defaultTop: Int = 0, defaultEnd: Int = 0, defaultBottom: Int = 0
) {
    val paddingAttrs = intArrayOf(
        android.R.attr.padding,
        android.R.attr.paddingLeft,
        android.R.attr.paddingTop,
        android.R.attr.paddingRight,
        android.R.attr.paddingBottom,
        android.R.attr.paddingStart,
        android.R.attr.paddingEnd,
    )

    val ta = context.obtainStyledAttributes(attrs, paddingAttrs, defStyleAttr, defStyleRes)
    val hasAll = ta.hasValue(0)
    val hasLeft = ta.hasValue(1)
    val hasTop = ta.hasValue(2)
    val hasRight = ta.hasValue(3)
    val hasBottom = ta.hasValue(4)
    val hasStart = ta.hasValue(5)
    val hasEnd = ta.hasValue(6)
    val all = if (hasAll) ta.getDimensionPixelSize(0, 0) else null

    val top =
        if (hasTop) ta.getDimensionPixelSize(2, 0)
        else all ?: defaultTop

    val bottom =
        if (hasBottom) ta.getDimensionPixelSize(4, 0)
        else all ?: defaultBottom
    val layoutDirection = this.layoutDirection
    val start = when {
        hasLeft && layoutDirection == LAYOUT_DIRECTION_LTR ->
            ta.getDimensionPixelSize(1, 0)

        hasRight && layoutDirection == LAYOUT_DIRECTION_RTL ->
            ta.getDimensionPixelSize(3, 0)

        hasStart ->
            ta.getDimensionPixelSize(5, 0)

        else ->
            all ?: defaultStart
    }
    val end = when {
        hasRight && layoutDirection == LAYOUT_DIRECTION_LTR ->
            ta.getDimensionPixelSize(3, 0)

        hasLeft && layoutDirection == LAYOUT_DIRECTION_RTL ->
            ta.getDimensionPixelSize(1, 0)

        hasEnd ->
            ta.getDimensionPixelSize(6, 0)

        else ->
            all ?: defaultEnd
    }
    ta.recycle()

    setPaddingRelative(start, top, end, bottom)
}