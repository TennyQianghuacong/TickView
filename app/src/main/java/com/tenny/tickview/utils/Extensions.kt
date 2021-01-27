package com.tenny.tickview.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue

private val displayMetrics = Resources.getSystem().displayMetrics

val Float.dp2px
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, displayMetrics)

val Int.dp2px
    get() = this.toFloat().dp2px

fun getAvatar(resources: Resources, resId: Int, width: Int): Bitmap{
    val option = BitmapFactory.Options()
    option.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(resources, resId, option)
    option.inJustDecodeBounds = false;
    option.inDensity = option.outWidth
    option.inTargetDensity = width
    return BitmapFactory.decodeResource(resources, resId, option)
}

fun Rect.toRectF(offset: Float): RectF {
    return RectF(
        this.left.toFloat() + offset,
        this.top.toFloat() + offset,
        this.right.toFloat() - offset,
        this.bottom.toFloat() - offset
    )
}
