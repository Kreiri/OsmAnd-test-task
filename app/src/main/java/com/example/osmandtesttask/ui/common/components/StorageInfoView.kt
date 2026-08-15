package com.example.osmandtesttask.ui.common.components

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.osmandtesttask.R
import com.example.osmandtesttask.common.asFractionOf
import com.example.osmandtesttask.ui.common.extensions.dpToPx
import com.example.osmandtesttask.ui.common.extensions.resolvePaddings
import androidx.core.content.withStyledAttributes
import com.example.osmandtesttask.common.toReadableFileSize
import com.google.android.material.progressindicator.LinearProgressIndicator

class StorageInfoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val volumeLabel: TextView
    private val freeSpaceLabel: TextView
    val storageBar: LinearProgressIndicator

    init {
        LayoutInflater.from(context).inflate(R.layout.view_storage_info, this, true)
        volumeLabel = findViewById(R.id.label)
        freeSpaceLabel = findViewById(R.id.free_space)
        storageBar = findViewById(R.id.storage_bar)
        storageBar.max = 100
        applyAttrs(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun applyAttrs(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) {
        val defaultPaddingVertical = 12.dpToPx(context)
        val defaultPaddingHorizontal = 16.dpToPx(context)
        resolvePaddings(
            context, attrs, defStyleAttr, defStyleRes,
            defaultStart = defaultPaddingHorizontal,
            defaultTop = defaultPaddingVertical,
            defaultEnd = defaultPaddingHorizontal,
            defaultBottom = defaultPaddingVertical
        )
        context.withStyledAttributes(
            attrs,
            R.styleable.StorageInfoView,
            defStyleAttr,
            defStyleRes
        ) {
            applyCustomAttrs(this)
            applyTextAttrs(this)
        }

    }

    private fun applyTextAttrs(ta: TypedArray) {
        if (ta.hasValue(R.styleable.StorageInfoView_android_textSize)) {
            val textSize = ta.getDimensionPixelSize(R.styleable.StorageInfoView_android_textSize, -1)
            if (textSize != -1) {
                volumeLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                freeSpaceLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
            }
        }
        if (ta.hasValue(R.styleable.StorageInfoView_android_textColor)) {
            val textColor = ta.getColorStateList(R.styleable.StorageInfoView_android_textColor)
            if (textColor != null) {
                volumeLabel.setTextColor(textColor)
                freeSpaceLabel.setTextColor(textColor)
            }
        }

    }

    private fun applyCustomAttrs(ta: TypedArray) {
        volumeLabel.text = ta.getText(R.styleable.StorageInfoView_labelText)
        freeSpaceLabel.text = ta.getText(R.styleable.StorageInfoView_spaceText)
        if (ta.hasValue(R.styleable.StorageInfoView_barTrackColor)) {
            val color = ta.getColor(R.styleable.StorageInfoView_barTrackColor, 0)
            storageBar.trackColor = color
        }
        if (ta.hasValue(R.styleable.StorageInfoView_barColor)) {
            val color = ta.getColor(R.styleable.StorageInfoView_barColor, 0)
//            storageBar.progressTintList = ColorStateList.valueOf(color)
            storageBar.setIndicatorColor(color)
        }
        val barThickness = if (ta.hasValue(R.styleable.StorageInfoView_barThickness)) {
            ta.getDimensionPixelSize(R.styleable.StorageInfoView_barThickness, 0)
        } else {
            16.dpToPx(context)
        }
        storageBar.trackThickness = barThickness
    }

    fun setVolumeLabel(label: CharSequence) {
        volumeLabel.text = label
    }

    fun setFreeSpaceLabel(label: CharSequence) {
        freeSpaceLabel.text = label
    }

    fun updateStorageInfo(totalBytes: Long, availableBytes: Long) {
        val occupied = ((totalBytes - availableBytes).asFractionOf(totalBytes) * 100).toInt()
        storageBar.progress = occupied
        val sizeText = availableBytes.toReadableFileSize(context)
        setFreeSpaceLabel(context.getString(R.string.storage_free_space, sizeText))
    }
}