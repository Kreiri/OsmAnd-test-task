package com.example.osmandtesttask.ui.common.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginDividerItemDecoration(
    private val dividerHeight: Int,
    private val dividerColor: Int,
    private val marginStart: Int,
    private val marginEnd: Int,
    private val alsoOmitItemIf: ((adapter: RecyclerView.Adapter<*>, position: Int) -> Boolean)? = null
) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        color = dividerColor
        style = Paint.Style.FILL
    }

    private fun shouldOmit(adapter: RecyclerView.Adapter<*>?, position: Int): Boolean {
        if (adapter == null) return true
        if (position == adapter.itemCount - 1) return true
        if (alsoOmitItemIf != null) {
            if (alsoOmitItemIf.invoke(adapter, position)) return true
        }
        return false
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == parent.adapter?.itemCount?.minus(1)) {
            outRect.setEmpty()
        } else {
            outRect.bottom = dividerHeight
        }
    }

    override fun onDraw(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val isRtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val leftMargin: Int
        val rightMargin: Int
        if (isRtl) {
            leftMargin = marginEnd
            rightMargin = marginStart
        } else {
            leftMargin = marginStart
            rightMargin = marginEnd
        }

        val left = parent.paddingLeft + leftMargin
        val right = parent.right - parent.paddingRight - rightMargin

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)

            if (position == parent.adapter?.itemCount?.minus(1)) {
                continue
            }

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + dividerHeight

            c.drawRect(
                left.toFloat(),
                top.toFloat(),
                right.toFloat(),
                bottom.toFloat(),
                paint
            )
        }

        super.onDraw(c, parent, state)
    }
}