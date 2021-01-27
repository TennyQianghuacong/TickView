package com.tenny.tickview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val animateDrawable : AnimatedVectorDrawable by lazy {
        ContextCompat.getDrawable(this, R.drawable.avd_anim) as AnimatedVectorDrawable
    }

    private var timeToReverse :Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initFollowAnimate()

        follow_layout.setOnClickListener {
            changeToVectorAnimate()
        }

    }

    private fun changeToVectorAnimate() {
        followAnimate.let {
            if (timeToReverse) {
                it.reverse()
            } else {
               it.start()
            }
            timeToReverse = !this.timeToReverse
        }
    }

    private lateinit var followAnimate: AnimatorSet

    private fun initFollowAnimate() {


        var followTvAnimate = ObjectAnimator.ofFloat(follow_tv, "alpha", 1f, 0f)
        var followIvAnimate = ObjectAnimator.ofFloat(follow_tick, "alpha", 0f, 1f)

        followAnimate = AnimatorSet().apply {
            this.playTogether(followTvAnimate, followIvAnimate)
            this.addListener(object : AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                    super.onAnimationEnd(animation, isReverse)

                    if (isReverse) {
                        follow_tick.setImageDrawable(null)
                    } else {
                        follow_tick.setImageDrawable(animateDrawable)
                        animateDrawable.start()
                    }
                }
            })
        }
    }

}