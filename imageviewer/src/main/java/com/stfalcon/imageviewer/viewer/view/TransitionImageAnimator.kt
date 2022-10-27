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

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.stfalcon.imageviewer.common.extensions.*
import com.stfalcon.imageviewer.viewer.builder.BuilderData

internal class TransitionImageAnimator(
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
        prepareTransitionLayout()
        data.onOpenBeforeScaleType?.invoke(externalImage)?.let { internalImage.scaleType = it }
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
            Log.v("ImageViewer", "externalImage.localVisibleRect: ${localVisibleRect}, ${localVisibleRect}")
            Log.v("ImageViewer", "externalImage.globalVisibleRect: ${globalVisibleRect}, ${globalVisibleRect}")
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
        (data.onTransition?.invoke(isClosing) ?: AutoTransition()
            .setDuration(transitionDuration)
            .setInterpolator(DecelerateInterpolator()))
            .addListener(onTransitionEnd = { onTransitionEnd?.invoke() })
}