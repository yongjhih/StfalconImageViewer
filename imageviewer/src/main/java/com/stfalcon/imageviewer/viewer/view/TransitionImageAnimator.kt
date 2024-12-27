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

import android.graphics.drawable.Drawable
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.stfalcon.imageviewer.common.extensions.addListener
import com.stfalcon.imageviewer.common.extensions.applyMargin
import com.stfalcon.imageviewer.common.extensions.globalVisibleRect
import com.stfalcon.imageviewer.common.extensions.isRectVisible
import com.stfalcon.imageviewer.common.extensions.localVisibleRect
import com.stfalcon.imageviewer.common.extensions.makeViewMatchParent
import com.stfalcon.imageviewer.common.extensions.postApply
import com.stfalcon.imageviewer.common.extensions.postDelayed
import com.stfalcon.imageviewer.common.extensions.requestNewSize
import com.stfalcon.imageviewer.common.extensions.systemBarsInsets
import com.stfalcon.imageviewer.viewer.builder.BuilderData
import timber.log.Timber

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
            // Timber.v("it.systemBarsInsets: ${externalImage.systemBarsInsets}")
            // Timber.v("internalImageContainer.systemBarsInsets: ${internalImageContainer.systemBarsInsets}")
            // // it.systemBarsInsets: Insets{left=0, top=128, right=0, bottom=63}
            // // internalImageContainer.systemBarsInsets: Insets{left=0, top=0, right=0, bottom=63}
            val topOffset = (externalImage.systemBarsInsets?.top ?: 0) - (internalImageContainer.systemBarsInsets?.top ?: 0)
            val bottomOffset = (externalImage.systemBarsInsets?.bottom ?: 0) - (internalImageContainer.systemBarsInsets?.bottom ?: 0)
            if (externalImage.isRectVisible) {
                with(externalImage.localVisibleRect) {
                    internalImage.requestNewSize(it.width, it.height)
                    internalImage.applyMargin(top = -top, start = -left)
                }
                with(externalImage.globalVisibleRect) {
                    internalImageContainer.requestNewSize(width(), height())
                    internalImageContainer.applyMargin(left, top - topOffset, right, bottom - bottomOffset)
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

val Drawable.size: Size get() = Size(intrinsicWidth, intrinsicHeight)

val Drawable.sizeOrNull: Size? get() = size.takeIf { it.width >= 0 && it.height >= 0 }

val View.size: Size get() = Size(width, height)