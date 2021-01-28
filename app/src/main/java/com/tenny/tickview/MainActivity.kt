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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        tick_view.setOnClickListener {
            tick_view.showLoadingView()
            tick_view.postDelayed({tick_view.showTickView()}, 0)
        //    tick_view.postDelayed({tick_view.resetTickView()}, 4000)
        }
    }

}