package com.stfalcon.sample.features.demo.grid

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.squareup.contour.ContourLayout
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.listeners.OnImageChangeListener
import com.stfalcon.sample.R
import com.stfalcon.sample.common.extensions.getDrawableCompat
import com.stfalcon.sample.common.extensions.loadImage
import com.stfalcon.sample.common.models.Demo
import com.stfalcon.sample.common.models.Poster
import kotlinx.android.synthetic.main.activity_demo_posters_grid.*

class PostersGridDemoActivity : AppCompatActivity() {

    private lateinit var viewer: StfalconImageViewer<Poster>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_posters_grid)

        postersGridView.apply {
            imageLoader = ::loadPosterImage
            onPosterClick = ::openViewer
        }
    }

    private fun openViewer(startPosition: Int, target: ImageView) {
        val context = target.context
        viewer = StfalconImageViewer.Builder<Poster>(this, Demo.posters, ::loadPosterImage)
            .withHiddenStatusBar(false)
            .withStartPosition(startPosition)
            .withTransitionFrom(target)
            .withImageChangeListener {
                viewer.updateTransitionImage(postersGridView.imageViews[it])
            }
            .run {
                val onChanger = object : OnImageChangeListener {
                    var onChange: (Int) -> Unit = {}
                    override fun onImageChange(position: Int) { onChange(position) }
                }
                withImageChangeListener(onChanger)
                //val backView = LayoutInflater.from(context).inflate(R.layout.overlay, null)
                val backView = BackOverlayView(context)
                withOverlayView(backView)

                build().let { viewer ->
                    //val tab = backView.findViewById<TabLayout>(R.id.tab)
                    //tab.setupWithViewPager(viewer.viewPager)
                    backView.viewPager = viewer.viewPager
                    backView.onBack = { viewer.close() }
                    viewer.builderData.scaleType = ImageView.ScaleType.CENTER_CROP
                    viewer.show()
                    viewer
                }
            }
    }

    private fun loadPosterImage(imageView: ImageView, poster: Poster?) {
        imageView.apply {
            background = getDrawableCompat(R.drawable.shape_placeholder)
            loadImage(poster?.url)
        }
    }
}

class BackOverlayView(context: Context) : ContourLayout(context) {
    var onBack: (View) -> Unit = {}

    private val backButton = ImageView(context).apply {
        setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        setColorFilter(Color.WHITE)
        setOnClickListener { onBack(it) }
    }
    var viewPager: ViewPager? = null
        set(value) {
            tabLayout.setupWithViewPager(value)
        }

    private val tabLayout = TabLayout(ContextThemeWrapper(context, R.style.DotTabLayout)).apply {
    }

    init {
        backButton.layoutBy(
            x = leftTo { parent.left() + 16.xdip }.widthOf { 24.xdip },
            y = topTo { parent.top() + 25.ydip + 16.ydip }.heightOf { 24.ydip },
        )
        tabLayout.layoutBy(
            x = leftTo { parent.left() }.rightTo { parent.right() },
            y = topTo { backButton.bottom() + 16.ydip }.heightOf { 32.ydip },
        )
        contourHeightWrapContent()
    }
}