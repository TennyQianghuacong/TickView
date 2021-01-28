package com.tenny.tickview.widget

import android.animation.*
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.tenny.tickview.R
import com.tenny.tickview.utils.dp2px
import kotlin.math.max
import kotlin.math.min

/**
 * Created by TennyQ on 1/26/21
 */
class TickView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var viewColor: Int = Color.WHITE
    private var strokeWidth: Float = 2.dp2px
    private var tickAreaWidth: Float = 16.dp2px
    private var loadingRadius = tickAreaWidth / 3.2f

    private var showTick: Boolean = false
    private var showLoading: Boolean = false

    private var runningLoadingAnimate = false
    private var runningTickAnimate = false

    var textContent: String = ""
        set(value) {
            field = value
            invalidate()
        }

    private var textSize: Float = 12.dp2px
        set(value) {
            field = value
            invalidate()
        }

    private var startAngle = 180f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 扇形的角度
     */
    private val sweepAngle: Float = 300f

    private var loadingAlpha: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 提钩的绘制路径长度
     */
    private var tickPathDistance: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 旋转动画
     */
    private val turnAroundAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(this, "startAngle", 180f, 539f).apply {
            duration = 700L
            interpolator = LinearInterpolator()
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    super.onAnimationStart(animation)
                    runningLoadingAnimate = true
                }

                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    runningLoadingAnimate = false
                }

                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    runningLoadingAnimate = false
                }
            })
        }
    }

    /**
     * 渐显动画
     */
    private val alphaShowAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofInt(this, "loadingAlpha", 0, 255).apply {
            duration = 300L
            startDelay = 100L
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    super.onAnimationStart(animation)
                    showTick = false
                    showLoading = true
                }
            })
        }
    }

    /**
     * 渐隐动画
     */
    private val alphaHideAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofInt(this, "loadingAlpha", 255, 0).apply {
            duration = 300L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    super.onAnimationStart(animation)
                    alphaShowAnimator.cancel()
                }

                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    showTick = true
                    showLoading = false

                    showLoadingAnimatorSet?.cancel()
                    turnAroundAnimator.end()
                }
            })
        }
    }

    /**
     * 整View的渐隐动画
     */
    private val wholeAlphaAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
    }

    /**
     * 收缩动画
     */
    private val shrinkAnimator: ValueAnimator by lazy {
        ValueAnimator.ofInt(width, 0).apply {
            addUpdateListener { animation ->
                val lp = layoutParams
                lp.width = animation.animatedValue as Int
                layoutParams = lp
            }
        }
    }

    private var showLoadingAnimatorSet: AnimatorSet? = null
    private var showTickAnimatorSet: AnimatorSet? = null

    /**
     * 打钩绘制动画
     */
    private var tickDistanceAnimator: ObjectAnimator? = null

    private val fullTickPath = Path()
    private val drawTickPath = Path()
    private val pathMeasure = PathMeasure()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fontMetrics = Paint.FontMetrics()

    private val arcRectF: RectF = RectF()

    private val colorGreen = Color.parseColor("#00D2BB")

    init {
        initAttributes(context, attrs)
    }

    /**
     * 初始化属性
     */
    private fun initAttributes(context: Context?, attrs: AttributeSet?) {
        attrs.let {
            val typedArray = context!!.obtainStyledAttributes(attrs!!, R.styleable.TickView)

            viewColor = typedArray.getColor(R.styleable.TickView_color, Color.WHITE)
            strokeWidth = typedArray.getDimension(R.styleable.TickView_stroke_width, 2.dp2px)

            tickAreaWidth = typedArray.getDimension(R.styleable.TickView_tick_area_width, 16.dp2px)
            loadingRadius = tickAreaWidth / 3.2f

            textSize = typedArray.getDimension(R.styleable.TickView_textSize, 12.dp2px)
            val content = typedArray.getString(R.styleable.TickView_text)
            textContent = content ?: ""

            typedArray.recycle()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAllAnimator()
    }

    /**
     * 停止所有的动画
     */
    private fun stopAllAnimator() {
        showLoadingAnimatorSet?.cancel()
        showTickAnimatorSet?.cancel()
        tickDistanceAnimator?.cancel()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val textMeasureExpect = FloatArray(2)
        measureText(textMeasureExpect)

        val widthMeasureExpect = measureEdgeSize(widthMeasureSpec, textMeasureExpect[0])
        val heightMeasureExpect = measureEdgeSize(heightMeasureSpec, textMeasureExpect[1])

        setMeasuredDimension(widthMeasureExpect, heightMeasureExpect)
    }

    /**
     * 测量字符串
     */
    private fun measureText(textMeasureSpec: FloatArray) {
        paint.textSize = textSize
        paint.getFontMetrics(fontMetrics)
        textMeasureSpec[0] = paint.measureText(textContent) + max(
            paddingStart + paddingEnd,
            paddingLeft + paddingRight
        )
        textMeasureSpec[1] = paint.fontSpacing + paddingTop + paddingBottom
    }

    /**
     * 测量边距
     */
    private fun measureEdgeSize(edgeMeasureSpec: Int, textMeasureExpect: Float): Int {
        val measureSpecSize = MeasureSpec.getSize(edgeMeasureSpec)

        return when (MeasureSpec.getMode(edgeMeasureSpec)) {
            MeasureSpec.EXACTLY -> {
                measureSpecSize
            }
            else -> {
                min(measureSpecSize, max(tickAreaWidth, textMeasureExpect).toInt())
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setLoadingRect()
        setTickPathTrack()
        measureTickPath()
    }

    /**
     * 设置Loading区域的大小
     */
    private fun setLoadingRect() {
        val edgeSize = max(min(width, height), tickAreaWidth.toInt())
        val loadingSize = edgeSize / 3.2f
        arcRectF.left = width / 2f - loadingSize
        arcRectF.top = height / 2f - loadingSize
        arcRectF.right = width / 2f + loadingSize
        arcRectF.bottom = height / 2f + loadingSize
    }

    /**
     * 设置提钩的Path轨迹
     */
    private fun setTickPathTrack() {
        val edgeSize = max(min(width, height), tickAreaWidth.toInt())

        val originX: Float = (width - edgeSize) / 2f
        val originY: Float = (height - edgeSize) / 2f

        fullTickPath.moveTo(originX + edgeSize / 6f, originY + edgeSize / 2f)

        fullTickPath.lineTo(originX + edgeSize * (9 / 24f), originY + edgeSize * (3 / 4f))
        fullTickPath.lineTo(originX + edgeSize * (5 / 6f), originY + edgeSize * (7 / 24f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //       drawBackGround(canvas)
        drawText(canvas)
        drawCircle(canvas)
        drawTick(canvas)
    }

    /**
     * 绘制背景
     */
    private fun drawBackGround(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint.apply {
            style = Paint.Style.FILL
            color = colorGreen
            alpha = 255
        })
    }

    /**
     * 绘制文字
     */
    private fun drawText(canvas: Canvas) {
        if (TextUtils.isEmpty(textContent) || showTick || showLoading) {
            return
        }
        canvas.drawText(
            textContent,
            width / 2f,
            (height - fontMetrics.descent - fontMetrics.ascent) / 2f,
            paint.apply {
                style = Paint.Style.FILL
                color = viewColor
                alpha = 255
                textSize = this@TickView.textSize
                textAlign = Paint.Align.CENTER
            })


    }

    /**
     * 绘制圆圈
     */
    private fun drawCircle(canvas: Canvas) {
        if (!showLoading) {
            return
        }
        canvas.drawArc(arcRectF, startAngle, sweepAngle, false, paint.apply {
            style = Paint.Style.STROKE
            color = viewColor
            alpha = this@TickView.loadingAlpha
            strokeWidth = this@TickView.strokeWidth
        })
    }


    /**
     * 绘制提勾
     */
    private fun drawTick(canvas: Canvas) {
        if (!showTick) {
            return
        }
        drawTickPath.reset()
        pathMeasure.getSegment(0f, tickPathDistance, drawTickPath, true)
        canvas.drawPath(drawTickPath, paint.apply {
            style = Paint.Style.STROKE
            color = viewColor
            strokeWidth = this@TickView.strokeWidth
            alpha = 255
        })
    }

    /**
     * 测量提钩Path轨迹长度
     */
    private fun measureTickPath() {
        pathMeasure.setPath(fullTickPath, false)
        tickDistanceAnimator?.let {
            if (tickDistanceAnimator!!.isRunning) {
                tickDistanceAnimator!!.end()
            }
        }
        tickDistanceAnimator =
            ObjectAnimator.ofFloat(this, "tickPathDistance", 0f, pathMeasure.length).apply {
                startDelay = 100L
                duration = 600L

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        Log.e("QHC", "alpha: $loadingAlpha")
                    }
                })
            }
    }

    /**
     * 显示loading视图
     */
    fun showLoadingView() {
        if (showLoadingAnimatorSet == null) {
            showLoadingAnimatorSet = AnimatorSet().apply {
                playTogether(turnAroundAnimator, alphaShowAnimator)
            }
        }

        showLoadingAnimatorSet?.let {
            if (!it.isRunning) {
                it.start()
            }
        }
    }

    /**
     * 显示提钩
     */
    fun showTickView() {
        if (showTickAnimatorSet == null) {

            val togetherSet = AnimatorSet().apply {
                playTogether(alphaHideAnimator, tickDistanceAnimator)
            }
            showTickAnimatorSet = AnimatorSet().apply {
                playSequentially(togetherSet, wholeAlphaAnimator, shrinkAnimator)
            }
        }
        showTickAnimatorSet?.start()
    }

    /**
     * 重置View状态
     */
    fun resetTickView() {
        stopAllAnimator()

        showTick = false
        showLoading = false
        runningLoadingAnimate = false

        val lp = layoutParams
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams = lp
        loadingAlpha = 0
        tickPathDistance = 0f
        alpha = 1f
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()

        val saveState = SaveSate(superState)
        saveState.textContent = this.textContent
        saveState.showTick = this.showTick
        saveState.showLoading = this.showLoading
        saveState.runningLoadingAnimate = this.runningLoadingAnimate
        saveState.startAngle = this.startAngle
        saveState.tickPathDistance = this.tickPathDistance
        saveState.loadingAlpha = this.loadingAlpha
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            null, !is SaveSate -> {
                return super.onRestoreInstanceState(state)
            }
        }

        val saveSate = state as SaveSate
        super.onRestoreInstanceState(saveSate.superState)

        this.textContent = saveSate.textContent ?: ""
        this.showTick = saveSate.showTick
        this.showLoading = saveSate.showLoading
        this.runningLoadingAnimate = saveSate.runningLoadingAnimate
        this.tickPathDistance = saveSate.tickPathDistance
        this.startAngle = saveSate.startAngle
        this.loadingAlpha = saveSate.loadingAlpha

        if (runningLoadingAnimate) {
            showLoadingView()
        }
    }

    class SaveSate : BaseSavedState {

        var textContent: String ? = null
        var showTick: Boolean = false
        var showLoading: Boolean = false
        var runningLoadingAnimate: Boolean = false
        var startAngle: Float = 0f
        var tickPathDistance: Float = 0f
        var loadingAlpha: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            textContent = source.readString()
            showTick = source.readByte().toInt() != 0
            showLoading = source.readByte().toInt() != 0
            runningLoadingAnimate = source.readByte().toInt() != 0
            startAngle = source.readFloat()
            tickPathDistance = source.readFloat()
            loadingAlpha = source.readInt()
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeByte(if (showTick) 1.toByte() else 0.toByte())
            out?.writeByte(if (showLoading) 1.toByte() else 0.toByte())
            out?.writeByte(if (runningLoadingAnimate) 1.toByte() else 0.toByte())
            out?.writeString(textContent)
            out?.writeFloat(startAngle)
            out?.writeFloat(tickPathDistance)
            out?.writeInt(loadingAlpha)
        }

        companion object {
            val CREATOR: Parcelable.Creator<SaveSate?> = object : Parcelable.Creator<SaveSate?> {
                override fun createFromParcel(source: Parcel): SaveSate? {
                    return SaveSate(source)
                }

                override fun newArray(size: Int): Array<SaveSate?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

}