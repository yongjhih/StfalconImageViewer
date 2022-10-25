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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.transition.*
import com.stfalcon.imageviewer.common.extensions.*

internal class TransitionImageAnimator(
    private val externalImage: ImageView?,
    private val internalImage: ImageView,
    private val internalImageContainer: FrameLayout,
    private val scaleType: ImageView.ScaleType? = null
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
        /**
         * For a landscape image in a square center-crop imageView
         *
         * If we don't make it with same scale-type,
         * the transition starts with inappropriate non-fulfill landscape shape from a fulfill center-crop ImageView
         */
        internalImage.scaleType = scaleType ?: externalImage?.scaleType ?: ImageView.ScaleType.CENTER_CROP
        prepareTransitionLayout()

        internalRoot.postApply {
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

            /**
             * Since the transition may start with non fit-center scaleType,
             * we should make it back to fit-center as target scaleType
             */
            internalImage.scaleType = ImageView.ScaleType.FIT_CENTER
            internalImageContainer.requestLayout()
        }
    }

    private fun doCloseTransition(onTransitionEnd: () -> Unit) {
        isAnimating = true
        isClosing = true

        TransitionManager.beginDelayedTransition(
            internalRoot, createTransition { handleCloseTransitionEnd(onTransitionEnd) })

        /**
         * For a landscape image in a square center-crop imageView
         *
         * If we don't make it with same scale-type,
         * the transition starts with inappropriate non-fulfill landscape shape from a fulfill center-crop ImageView
         */
        internalImage.scaleType = scaleType ?: externalImage?.scaleType ?: ImageView.ScaleType.CENTER_CROP
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
                    internalImageContainer.requestNewSize(first.width(), first.height())
                    internalImageContainer.applyMargin(first.left, first.top, first.right, first.bottom)
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

    /**
     * ```
     * AutoTransition()
     *     .setDuration(transitionDuration)
     *     .setInterpolator(DecelerateInterpolator())
     *     .addListener(onTransitionEnd = { onTransitionEnd?.invoke() })
     * ```
     */
    private fun createTransition(onTransitionEnd: ((Transition) -> Unit) = {}): Transition =
        TransitionSet().apply {
            ordering = TransitionSet.ORDERING_SEQUENTIAL
            addTransition(Fade(Fade.OUT)).apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                //addTransition(ChangeClipBounds())
                //addTransition(ChangeTransform())
                addTransition(ChangeBounds())
                addTransition(ChangeImageTransform())
            }.addTransition(Fade(Fade.IN))
        }.
        setDuration(transitionDuration).
        setInterpolator(DecelerateInterpolator()).
        addListener(onTransitionEnd = onTransitionEnd)
}

/*
internal class CenterTransitionImageAnimator(
    private val externalImage: ImageView?,
    private val internalImage: ImageView,
    private val internalImageContainer: FrameLayout
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
                externalImage.visibility = View.VISIBLE
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