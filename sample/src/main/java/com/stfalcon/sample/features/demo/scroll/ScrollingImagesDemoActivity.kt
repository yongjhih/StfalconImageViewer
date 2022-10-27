package com.stfalcon.sample.features.demo.scroll

import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.*
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.sample.R
import com.stfalcon.sample.common.extensions.getDrawableCompat
import com.stfalcon.sample.common.extensions.loadImage
import com.stfalcon.sample.common.models.Demo
import kotlinx.android.synthetic.main.activity_demo_scrolling_images.*

class ScrollingImagesDemoActivity : AppCompatActivity() {

    private val horizontalImageViews by lazy {
        listOf(
            scrollingHorizontalFirstImage,
            scrollingHorizontalSecondImage,
            scrollingVerticalThirdImage,
            scrollingHorizontalFourthImage)
    }

    private val verticalImageViews by lazy {
        listOf(
            scrollingVerticalFirstImage,
            scrollingVerticalSecondImage,
            scrollingHorizontalThirdImage,
            scrollingVerticalFourthImage)
    }

    private lateinit var viewer: StfalconImageViewer<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_scrolling_images)

        horizontalImageViews.forEachIndexed { index, imageView ->
            loadImage(imageView, Demo.horizontalImages.getOrNull(index))
            imageView.setOnClickListener {
                openViewer(index, imageView, Demo.horizontalImages, horizontalImageViews)
            }
        }

        verticalImageViews.forEachIndexed { index, imageView ->
            loadImage(imageView, Demo.verticalImages.getOrNull(index))
            imageView.setOnClickListener {
                openViewer(index, imageView, Demo.verticalImages, verticalImageViews)
            }
        }
    }

    private fun openViewer(
        startPosition: Int,
        target: ImageView,
        images: List<String>,
        imageViews: List<ImageView>) {
        viewer = StfalconImageViewer.Builder<String>(this, images, ::loadImage)
            .withHiddenStatusBar(false)
            .withStartPosition(startPosition)
            .withTransitionFrom(target)
            .apply {
                data.onScaleType = { it?.scaleType }
                data.onOpenBeforeScaleType = { it?.scaleType }
                data.onOpenAfterScaleType = { ImageView.ScaleType.FIT_CENTER }
                //data.offset = Rect(0, -120, 0,  0)
                data.onTransition = {
                    TransitionSet().apply {
                        ordering = TransitionSet.ORDERING_SEQUENTIAL
                        addTransition(Fade(Fade.OUT))
                        addTransition(TransitionSet().apply {
                            ordering = TransitionSet.ORDERING_TOGETHER
                            //addTransition(ChangeTransform())
                            addTransition(ChangeBounds())
                            //addTransition(ChangeClipBounds())
                            addTransition(ChangeImageTransform())
                        })
                        addTransition(Fade(Fade.IN))
                    }
                }
            }
            .withImageChangeListener { viewer.updateTransitionImage(imageViews.getOrNull(it)) }
            .show()
    }

    private fun loadImage(imageView: ImageView, url: String?) {
        imageView.apply {
            background = getDrawableCompat(R.drawable.shape_placeholder)
            loadImage(url)
        }
    }
}