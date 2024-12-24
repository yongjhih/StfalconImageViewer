/*
 * Copyright 2018 stfalcon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stfalcon.imageviewer.viewer.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.transition.ArcMotion
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.github.chrisbanes.photoview.PhotoView
import com.stfalcon.imageviewer.R
import com.stfalcon.imageviewer.common.extensions.*
import com.stfalcon.imageviewer.common.pager.RecyclingPagerAdapter
import com.stfalcon.imageviewer.viewer.builder.BuilderData
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

internal class TransitionImageAnimator(
    private val externalImage: ImageView?,
    private val internalImage: ImageView,
    private val internalImageContainer: FrameLayout,
    private val data: BuilderData<*>,
) {

    companion object {
        private const val TRANSITION_DURATION_OPEN = 200L
        private const val TRANSITION_DURATION_CLOSE = 250L
    }

    internal var isAnimating = false

    private var isClosing = false

    private val transitionDuration: Long
        get() = if (isClosing) TRANSITION_DURATION_CLOSE else TRANSITION_DURATION_OPEN

    private val internalRoot: ViewGroup
        get() = internalImageContainer.parent as ViewGroup

    internal fun animateOpen(
        containerPadding: IntArray,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible) {
            onTransitionStart(TRANSITION_DURATION_OPEN)
            doOpenTransition(containerPadding, onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    internal fun animateClose(
        shouldDismissToBottom: Boolean,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible && !shouldDismissToBottom) {
            onTransitionStart(TRANSITION_DURATION_CLOSE)
            doCloseTransition(onTransitionEnd)
        } else {
            externalImage?.visibility = View.VISIBLE
            onTransitionEnd()
        }
    }

    private fun doOpenTransition(containerPadding: IntArray, onTransitionEnd: () -> Unit) {
        isAnimating = true
        prepareTransitionLayout()
        internalRoot.postApply {
            val oldScaleType = internalImage.scaleType
            //ain't nothing but a kludge to prevent blinking when transition is starting
            externalImage?.postDelayed(50) { visibility = View.INVISIBLE }

            internalImage.scaleType = externalImage?.scaleType ?: oldScaleType
            TransitionManager.beginDelayedTransition(internalRoot, createTransition {
                if (!isClosing) {
                    isAnimating = false
                    onTransitionEnd()
                }
            })

            internalImageContainer.makeViewMatchParent()
            internalImage.makeViewMatchParent()
            internalImage.scaleType = oldScaleType

            internalRoot.applyMargin(
                containerPadding[0],
                containerPadding[1],
                containerPadding[2],
                containerPadding[3])

            internalImageContainer.requestLayout()
        }
    }

    private fun doCloseTransition(onTransitionEnd: () -> Unit) {
        isAnimating = true
        isClosing = true
        val oldScaleType = internalImage.scaleType
        TransitionManager.beginDelayedTransition(
            internalRoot, createTransition {
                handleCloseTransitionEnd(onTransitionEnd)
                internalImage.scaleType = oldScaleType
            })
        internalImage.scaleType = externalImage?.scaleType ?: oldScaleType

        prepareTransitionLayout()
        internalImageContainer.requestLayout()
    }

    private fun prepareTransitionLayout() {
        externalImage?.let {
            if (externalImage.isRectVisible) {
                with(externalImage.localVisibleRect) {
                    internalImage.requestNewSize(it.width, it.height)
                    internalImage.applyMargin(top = -top, start = -left)
                }
                with(externalImage.globalVisibleRect) {
                    internalImageContainer.requestNewSize(width(), height())
                    internalImageContainer.applyMargin(left, top, right, bottom)
                }
            }

            resetRootTranslation()
        }
    }

    private fun handleCloseTransitionEnd(onTransitionEnd: () -> Unit) {
        externalImage?.visibility = View.VISIBLE
        internalImage.post { onTransitionEnd() }
        isAnimating = false
    }

    private fun resetRootTranslation() {
        internalRoot
            .animate()
            .translationY(0f)
            .setDuration(transitionDuration)
            .start()
    }

    private fun createTransition(onTransitionEnd: (() -> Unit)? = null): Transition =
        (data.onTransition?.invoke(isClosing) ?: TransitionSet().apply {
            ordering = TransitionSet.ORDERING_SEQUENTIAL
            addTransition(Fade(Fade.OUT))
            addTransition(TransitionSet().apply {
                addTransition(ChangeBounds())
                addTransition(ChangeImageTransform())
            })
            addTransition(Fade(Fade.IN))

            duration = transitionDuration
            interpolator = DecelerateInterpolator()
        }).apply {
            addListener(onTransitionEnd = { onTransitionEnd?.invoke() })
        }
}

/*
internal class TransitionImageAnimatorV1(
    private val externalImage: ImageView?,
    private val internalImage: ImageView,
    private val internalImageContainer: FrameLayout,
    private val data: BuilderData<*>
) {

    companion object {
        private const val TRANSITION_DURATION_OPEN = 200L
        private const val TRANSITION_DURATION_CLOSE = 250L
    }

    internal var isAnimating = false

    private var isClosing = false

    private val transitionDuration: Long
        get() = if (isClosing) TRANSITION_DURATION_CLOSE else TRANSITION_DURATION_OPEN

    private val internalRoot: ViewGroup
        get() = internalImageContainer.parent as ViewGroup

    internal fun animateOpen(
        containerPadding: IntArray,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible) {
            onTransitionStart(TRANSITION_DURATION_OPEN)
            doOpenTransition(containerPadding, onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    internal fun animateClose(
        shouldDismissToBottom: Boolean,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible && !shouldDismissToBottom) {
            onTransitionStart(TRANSITION_DURATION_CLOSE)
            doCloseTransition(onTransitionEnd)
        } else {
            externalImage?.visibility = View.VISIBLE
            onTransitionEnd()
        }
    }

    private fun doOpenTransition(containerPadding: IntArray, onTransitionEnd: () -> Unit) {
        isAnimating = true
        data.onOpenBeforeScaleType?.invoke(externalImage)?.let { internalImage.scaleType = it }
        prepareTransitionLayout()
        internalRoot.postApply {
            data.onOpenAfterScaleType?.invoke(externalImage)?.let { internalImage.scaleType = it }
            //ain't nothing but a kludge to prevent blinking when transition is starting
            externalImage?.postDelayed(50) { visibility = View.INVISIBLE }

            TransitionManager.beginDelayedTransition(internalRoot, createTransition {
                if (!isClosing) {
                    isAnimating = false
                    onTransitionEnd()
                }
            })

            internalImageContainer.makeViewMatchParent()
            internalImage.makeViewMatchParent()

            internalRoot.applyMargin(
                containerPadding[0],
                containerPadding[1],
                containerPadding[2],
                containerPadding[3])

            internalImageContainer.requestLayout()
        }
    }

    private fun doCloseTransition(onTransitionEnd: () -> Unit) {
        isAnimating = true
        isClosing = true

        TransitionManager.beginDelayedTransition(
            internalRoot, createTransition { handleCloseTransitionEnd(onTransitionEnd) })

        data.onScaleType?.invoke(externalImage)?.let { internalImage.scaleType = it }
        prepareTransitionLayout()
        internalImageContainer.requestLayout()
    }

    private fun prepareTransitionLayout() {
        externalImage?.let { externalImage ->
            val localVisibleRect = externalImage.localVisibleRect
            val globalVisibleRect = externalImage.globalVisibleRect
            val isRectVisible = localVisibleRect != globalVisibleRect
            Log.v("ImageViewer", "externalImage.size: ${externalImage.width}, ${externalImage.height}")
            Log.v("ImageViewer", "externalImage.localVisibleRect: ${localVisibleRect}")
            Log.v("ImageViewer", "externalImage.globalVisibleRect: ${globalVisibleRect}")
            val insetsRect = ViewCompat.getRootWindowInsets(externalImage)?.stableInsets?.toRect().orEmpty()
            Log.v("ImageViewer", "externalImage.stableInsets: ${insetsRect}")
            val offset = data.offset.orEmpty()

            if (isRectVisible) {
                localVisibleRect.run {
                    internalImage.requestNewSize(externalImage.width, externalImage.height)
                    internalImage.applyMargin(
                        top = -top + offset.top, start = -left + offset.left)
                }
                globalVisibleRect.run {
                    internalImageContainer.requestNewSize(width(), height())
                    internalImageContainer.applyMargin(left, top)
                }
            }

            resetRootTranslation()
        }
    }

    private fun handleCloseTransitionEnd(onTransitionEnd: () -> Unit) {
        externalImage?.visibility = View.VISIBLE
        internalImage.post { onTransitionEnd() }
        isAnimating = false
    }

    private fun resetRootTranslation() {
        internalRoot
            .animate()
            .translationY(0f)
            .setDuration(transitionDuration)
            .start()
    }

    private fun createTransition(onTransitionEnd: (() -> Unit)? = null): Transition =
        TransitionSet().apply {
            ordering = TransitionSet.ORDERING_SEQUENTIAL
            addTransition(Fade(Fade.OUT))
            addTransition(TransitionSet().apply {
                //addTransition(ChangeTransform())
                addTransition(ChangeBounds())
                //addTransition(ChangeClipBounds())
                addTransition(ChangeImageTransform())
            })
            addTransition(Fade(Fade.IN))
            duration = 250L
            interpolator = DecelerateInterpolator()
            addListener(onTransitionEnd = { onTransitionEnd?.invoke() })
        }
    /*
        (data.onTransition?.invoke(isClosing) ?: AutoTransition()
            .setDuration(transitionDuration)
            .setInterpolator(DecelerateInterpolator()))
            .addListener(onTransitionEnd = { onTransitionEnd?.invoke() })
    */
}
*/

/*
internal class TransitionImageAnimator(
    private val externalImage: ImageView?,
    private val internalImage: ImageView,
    private val internalImageContainer: FrameLayout,
    private val data: BuilderData<*>
) {

    companion object {
        private const val TRANSITION_DURATION = 250L
    }

    private val internalRoot: ViewGroup
        get() = internalImageContainer.parent as ViewGroup

    internal var isAnimating = false
    private var isClosing = false
    private var scaleNumber: Float = 1f
    private var resetToXValue: Float = 0f
    private var resetToYValue: Float = 0f

    internal fun animateOpen(
        containerPadding: IntArray,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        updateTransitionView()
        if (externalImage.isRectVisible) {
            onTransitionStart(TRANSITION_DURATION)
            doOpenTransition(containerPadding, onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    internal fun animateClose(
        shouldDismissToBottom: Boolean,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        updateTransitionView()
        if (externalImage.isRectVisible && !shouldDismissToBottom) {
            onTransitionStart(TRANSITION_DURATION)
            doCloseTransition(onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    private fun doOpenTransition(containerPadding: IntArray, onTransitionEnd: () -> Unit) {
        isAnimating = true
        internalRoot.applyMargin(
            containerPadding[0],
            containerPadding[1],
            containerPadding[2],
            containerPadding[3])
        startAnimation(internalImage, externalImage, onTransitionEnd, true)
    }


    private fun doCloseTransition(onTransitionEnd: () -> Unit) {
        isAnimating = true
        isClosing = true
        startAnimation(internalImage, externalImage, onTransitionEnd, false)
    }

    fun updateTransitionView() {
        if (externalImage == null) {
            scaleNumber = 1f
            resetToXValue = 0f
            resetToYValue = 0f
            return
        }
        val itemView = internalImage
        val scaleX = externalImage.width * 1f / itemView.width
        val scaleY = externalImage.height * 1f / itemView.height
        scaleNumber = max(scaleX, scaleY)

        // target center location
        val location = IntArray(2)
        externalImage.getLocationOnScreen(location)

        val externalCenterX = (location[0] + externalImage.width / 2)
        val externalCenterY = (location[1] + externalImage.height / 2)

        // center of itemView
        val itemViewLocation = IntArray(2)
        itemView.getLocationOnScreen(itemViewLocation)

        val centerX = itemViewLocation[0] + itemView.width / 2
        val centerY = itemViewLocation[1] + itemView.height / 2

        val toXValue = (externalCenterX - centerX) * 1f
        val toYValue = (externalCenterY - centerY) * 1f

        resetToXValue = toXValue
        resetToYValue = toYValue
    }

    private fun startAnimation(
        itemView: View?,
        externalImage: View?,
        onTransitionEnd: (() -> Unit)? = null,
        isOpen: Boolean
    ) {
        if (itemView == null || externalImage == null) {
            return
        }
        val scaleX = externalImage.width * 1f / itemView.width
        val scaleY = externalImage.height * 1f / itemView.height
        val toX = max(scaleX, scaleY)

        scaleNumber = toX
        // scale itself by center
        val scaleAnimation: ScaleAnimation = if (isOpen) {
            ScaleAnimation(
                toX,
                1f,
                toX,
                1f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
        } else {
            ScaleAnimation(
                1f,
                toX,
                1f,
                toX,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
        }

        // target center location
        val location = IntArray(2)
        externalImage.getLocationOnScreen(location)

        val externalCenterX = (location[0] + externalImage.width / 2)
        val externalCenterY = (location[1] + externalImage.height / 2)

        // center of itemView
        val itemViewLocation = IntArray(2)
        itemView.getLocationOnScreen(itemViewLocation)

        val centerX = itemViewLocation[0] + itemView.width / 2
        val centerY = itemViewLocation[1] + itemView.height / 2

        val toXValue = (externalCenterX - centerX) * 1f
        val toYValue = (externalCenterY - centerY) * 1f

        resetToXValue = toXValue
        resetToYValue = toYValue

        val translateAnimation: TranslateAnimation = if (isOpen) {
            TranslateAnimation(
                Animation.ABSOLUTE, toXValue,
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, toYValue,
                Animation.ABSOLUTE, 0f
            )
        } else {
            TranslateAnimation(
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, toXValue,
                Animation.ABSOLUTE, 0f,
                Animation.ABSOLUTE, toYValue
            )
        }

        val fromWidth = externalImage.width
        val fromHeight = externalImage.height
        val toWidth = itemView.width
        val toHeight = itemView.height
        (itemView as? ImageView)?.scaleType = ImageView.ScaleType.CENTER_CROP
        ValueAnimator().apply {
            duration = TRANSITION_DURATION
            addUpdateListener {
                val value = (it.animatedValue as? Float) ?: 1f
                itemView.updateLayoutParams {
                    width = ((toWidth - fromWidth) * value).toInt() + fromWidth
                    height = ((toHeight - fromHeight) * value).toInt() + fromHeight
                }
                itemView.invalidate()
            }
            if (isOpen) {
                setFloatValues(0f, 1f)
            } else {
                setFloatValues(1f, 0f)
            }
        }.start()

        /*
        val animationSet = AnimationSet(true)
        //animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(translateAnimation)
        animationSet.duration = TRANSITION_DURATION
        animationSet.fillAfter = true
        itemView.startAnimation(animationSet)
        */

        /*
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {

            }

            override fun onAnimationEnd(p0: Animation?) {
                externalImage.visibility = View.VISIBLE
                if (!isClosing) {
                    isAnimating = false
                }
                onTransitionEnd?.invoke()
            }

            override fun onAnimationRepeat(p0: Animation?) {

            }
        })
        */
    }

}
*/

/*
internal class TransitionImageAnimatorV3(
    private val externalImage: View?,
    private var internalImage: View,
    //private val imageSize: Size? = (externalImage as? ImageView)?.drawable?.sizeOrNull,
    private val imageSize: Size? = (internalImage.getTag(R.id.transition_current_scene) as? Size) ?: externalImage?.getTag(R.id.transition_current_scene) as? Size,
) {

    companion object {
        private const val TRANSITION_DURATION = 250L
        private const val TRANSITION_DURATION_OPEN = 200L
        private const val TRANSITION_DURATION_CLOSE = 250L
    }

    internal var isAnimating = false
    private var isClosing = false
    private var scaleNumber: Float = 0f
    private var resetToXValue: Float = 0f
    private var resetToYValue: Float = 0f
    var viewType = RecyclingPagerAdapter.VIEW_TYPE_IMAGE
    var scaleSize = 1.0f

    internal fun animateOpen(
        containerPadding: IntArray,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible) {
            onTransitionStart(TRANSITION_DURATION_OPEN)
            doOpenTransition(containerPadding, onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    internal fun transitionAnimateClose(
        translationX: Float,
        translationY: Float,
        scaleTemp: Float,
        shouldDismissToBottom: Boolean,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible && !shouldDismissToBottom) {
            onTransitionStart(TRANSITION_DURATION)
            doCloseTransition(translationX, translationY, scaleTemp, onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    internal fun animateClose(
        shouldDismissToBottom: Boolean,
        onTransitionStart: (Long) -> Unit,
        onTransitionEnd: () -> Unit
    ) {
        if (externalImage.isRectVisible && !shouldDismissToBottom) {
            onTransitionStart(TRANSITION_DURATION)
            doCloseTransition(onTransitionEnd)
        } else {
            onTransitionEnd()
        }
    }

    private fun doOpenTransition(
        containerPadding: IntArray,
        onTransitionEnd: () -> Unit) {
        isAnimating = true
        /*
        internalRoot.applyMargin(
            containerPadding[0],
            containerPadding[1],
            containerPadding[2],
            containerPadding[3])
        */
        startAnimation(internalImage, externalImage, onTransitionEnd, true)

    }


    private fun doCloseTransition(
        translationX: Float,
        translationY: Float,
        scaleTemp: Float,
        onTransitionEnd: () -> Unit
    ) {
        isAnimating = true
        isClosing = true
        val p1: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat("translationX", translationX, resetToXValue)
        val p2: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat("translationY", translationY, resetToYValue)
        val p3: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat("scaleX", scaleTemp, scaleNumber)
        val p4: PropertyValuesHolder =
            PropertyValuesHolder.ofFloat("scaleY", scaleTemp, scaleNumber)
        val animator: ObjectAnimator =
            ObjectAnimator.ofPropertyValuesHolder(internalImage, p1, p2, p3, p4)
        animator.duration = TRANSITION_DURATION
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {

            }

            override fun onAnimationEnd(p0: Animator) {
                if (!isClosing) {
                    isAnimating = false
                }
                onTransitionEnd.invoke()
            }

            override fun onAnimationCancel(p0: Animator) {

            }

            override fun onAnimationRepeat(p0: Animator) {

            }
        })
        animator.start()
    }


    private fun doCloseTransition(onTransitionEnd: () -> Unit) {
        isAnimating = true
        isClosing = true
        startAnimation(internalImage, externalImage, onTransitionEnd, false)

    }

    fun updateTransitionView(itemView: View?, externalImage: View?) {
        this.internalImage = itemView!!
        // 缩放动画
        val scale = imageSize?.let { size ->
            min(size.width * 1F / externalImage!!.width, size.height * 1F / externalImage.height)
        }
        val transitionWidth = (imageSize?.let { size ->
            size.width / (scale ?: 1f)
        } ?: externalImage!!.height).toFloat()
        val transitionHeight = (imageSize?.let { size ->
            size.height / (scale ?: 1f)
        } ?: externalImage!!.height).toFloat()
        /*
        val transitionWidth = (externalImage!!.width).toFloat()
        val transitionHeight = (externalImage!!.height).toFloat()
        */
        val scaleX = transitionWidth / itemView.width
        val scaleY = transitionHeight / itemView.height
        val toX = if (scaleX > scaleY) {  // 哪个缩放的比例小就用哪个(例如: 0.9 收缩比例 比0.3要小)
            scaleX
        } else {
            scaleY
        }
        // 保存缩放比例,拖动缩小后恢复到原图大小需用到比例
        scaleNumber = toX

        //平移到外部imageView的中心点
        val location = IntArray(2)
        externalImage!!.getLocationOnScreen(location)

        val externalCenterX = (location[0] + externalImage.width / 2)
        val externalCenterY = (location[1] + externalImage.height / 2)

        //获取itemView中心点
        val itemViewLocation = IntArray(2)
        itemView.getLocationOnScreen(itemViewLocation)


        val centerX = itemViewLocation[0] + itemView.width / 2
        val centerY = itemViewLocation[1] + itemView.height / 2

        val toXValue = (externalCenterX - centerX) * 1f
        val toYValue = (externalCenterY - centerY) * 1f

        resetToXValue = toXValue
        resetToYValue = toYValue
    }

    private fun startAnimation(
        itemView: View?,
        externalImage: View?,
        onTransitionEnd: (() -> Unit)? = null,
        isOpen: Boolean
    ) {
        Timber.v("imageSize: $imageSize, externalSize: ${externalImage?.size}")
        val scale = imageSize?.let { size ->
            min(size.width * 1F / externalImage!!.width, size.height * 1F / externalImage.height)
        }
        val transitionWidth = (imageSize?.let { size ->
            size.width / (scale ?: 1f)
        } ?: externalImage!!.height).toFloat()
        val transitionHeight = (imageSize?.let { size ->
            size.height / (scale ?: 1f)
        } ?: externalImage!!.height).toFloat()
        /*
        val transitionWidth = (externalImage!!.width).toFloat()
        val transitionHeight = (externalImage!!.height).toFloat()
        */

        // 缩放动画
        val scaleX = transitionWidth / itemView!!.width
        val scaleY = transitionHeight / itemView.height
        val toX = if (scaleX > scaleY) { //那个缩放的比例小就用哪个(例如: 0.9 收缩比例 比0.3要小)
            scaleX
        } else {
            scaleY
        }

        if (!isOpen && viewType == RecyclingPagerAdapter.VIEW_TYPE_IMAGE) {
            fun animResetScale(view: View?) {
                if (view is PhotoView) {
                    view.setScale(1F, true)
                    return
                }
                if (view is ViewGroup) {
                    val count = view.childCount
                    for (index in 0..count) {
                        val childView = view.getChildAt(index)
                        animResetScale(childView)
                    }
                }
            }
            /*
            // itemView 是 dismissContainer, FrameLayout 包裹一个 ViewPager2, @see image_viewer_mage_viewer
            val viewPager2 =
                itemView.findViewById<ViewPager2>(com.stfalcon.imageviewer.R.id.imagesPager)
            val childView =
                (viewPager2.getChildAt(0) as RecyclerView).layoutManager?.findViewByPosition(viewPager2.currentItem)
            animResetScale(childView)
            */
        }

        scaleNumber = toX
        //以自己为中心进行缩放
        val scaleAnimation: ScaleAnimation = if (isOpen) {
            ScaleAnimation(
                toX,
                1f,
                toX,
                1f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
        } else {
            ScaleAnimation(
                1f,
                toX,
                1f,
                toX,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
        }

        //平移到外部imageView的中心点
        val location = IntArray(2)
        externalImage!!.getLocationOnScreen(location)

        val externalCenterX = (location[0] + externalImage.width / 2)
        val externalCenterY = (location[1] + externalImage.height / 2)

        //获取itemView中心点
        val itemViewLocation = IntArray(2)
        itemView.getLocationOnScreen(itemViewLocation)


        val centerX = itemViewLocation[0] + itemView.width / 2
        val centerY = itemViewLocation[1] + itemView.height / 2

        val toXValue = (externalCenterX - centerX) * 1f
        val toYValue = (externalCenterY - centerY) * 1f

        resetToXValue = toXValue
        resetToYValue = toYValue

        val translateAnimation: TranslateAnimation = if (isOpen) {
            TranslateAnimation(
                Animation.ABSOLUTE,
                toXValue,
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                toYValue,
                Animation.ABSOLUTE,
                0f
            )
        } else {
            TranslateAnimation(
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                toXValue,
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                toYValue
            )
        }

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(translateAnimation)
        animationSet.duration = TRANSITION_DURATION
        animationSet.fillAfter = true
        itemView.startAnimation(animationSet)

        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {

            }

            override fun onAnimationEnd(p0: Animation?) {
                //结束以后关闭dialog即可
                if (!isClosing) {
                    isAnimating = false
                }
                onTransitionEnd?.invoke()
            }

            override fun onAnimationRepeat(p0: Animation?) {

            }
        })
    }

}
*/

val Drawable.size: Size get() = Size(intrinsicWidth, intrinsicHeight)

val Drawable.sizeOrNull: Size? get() = size.takeIf { it.width >= 0 && it.height >= 0 }

val View.size: Size get() = Size(width, height)