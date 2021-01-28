package com.tenny.tickview.widget

import android.animation.*
import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
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
    private var strokeWidth : Float = 2.dp2px
    private var tickAreaWidth : Float = 16.dp2px
    private var loadingRadius = tickAreaWidth / 3.2f

    private var showTick: Boolean = false
    private var showLoading: Boolean = false

    private var textContent: String = ""
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

    private var alpha: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 提钩的绘制路径长度
     */
    private var tickPathDistance : Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 旋转动画
     */
    private val turnAroundAnimator = ObjectAnimator.ofFloat(this, "startAngle", 180f, 539f).apply {
        duration = 700L
        interpolator = LinearInterpolator()
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
    }

    /**
     * 透明度动画
     */
    private val alphaShowAnimator = ObjectAnimator.ofInt(this, "alpha", 0, 255).apply {
        duration = 300L
        startDelay = 100L
        interpolator = LinearInterpolator()
        addListener(object : AnimatorListenerAdapter(){
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                showTick = false
                showLoading = true
            }
        })
    }

    private val alphaHideAnimator: ObjectAnimator = ObjectAnimator.ofInt(this, "alpha", 255, 0).apply {
        duration = 300L
        addListener(object : AnimatorListenerAdapter(){
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                alphaShowAnimator.cancel()
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                showTick = true
                showLoading = false

                showLoadingAnimatorSet?.pause()
               // alphaShowAnimator.end()
                turnAroundAnimator.end()
            }
        })
    }

    private var showLoadingAnimatorSet: AnimatorSet ? = null
    private var showTickAnimatorSet: AnimatorSet ? = null

    /**
     * 打钩绘制动画
     */
    private var tickDistanceAnimator: ObjectAnimator ?= null

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
      //  showLoadingView()
      //  postDelayed({showTickView()}, 2000L)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        showLoadingAnimatorSet?.end()
        showTickAnimatorSet?.end()
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
        textMeasureSpec[0] = paint.measureText(textContent) + max(paddingStart + paddingEnd, paddingLeft + paddingRight)
        textMeasureSpec[1] = paint.fontSpacing + paddingTop + paddingBottom
    }

    /**
     * 测量边距
     */
    private fun measureEdgeSize(edgeMeasureSpec: Int, textMeasureExpect: Float) : Int {
        val measureSpecSize = MeasureSpec.getSize(edgeMeasureSpec)

        return when(MeasureSpec.getMode(edgeMeasureSpec)) {
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
    private fun setTickPathTrack(){
        val edgeSize = max(min(width, height), tickAreaWidth.toInt())

        val originX :Float = (width - edgeSize) / 2f
        val originY :Float = (height - edgeSize) / 2f

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
        canvas.drawText(textContent, width  / 2f, (height - fontMetrics.descent - fontMetrics.ascent) / 2f, paint.apply {
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
        canvas.drawArc(arcRectF, startAngle, sweepAngle, false, paint.apply{
            style = Paint.Style.STROKE
            color = viewColor
            alpha = this@TickView.alpha
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
        tickDistanceAnimator = ObjectAnimator.ofFloat(this, "tickPathDistance", 0f, pathMeasure.length).apply {
            startDelay = 100L
            duration = 600L

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    Log.e("QHC", "alpha: $alpha")
                }
            })
        }
    }

    public fun showLoadingView() {
        if (showLoadingAnimatorSet == null) {
            showLoadingAnimatorSet = AnimatorSet()
            showLoadingAnimatorSet?.playTogether(turnAroundAnimator, alphaShowAnimator)
        }
        showLoadingAnimatorSet?.start()
    }

    /**
     * 显示提钩
     */
    public fun showTickView() {
        if (showTickAnimatorSet == null) {
            showTickAnimatorSet = AnimatorSet()
            showTickAnimatorSet?.playSequentially(alphaHideAnimator, tickDistanceAnimator)
        }
        showTickAnimatorSet?.start()
    }

}