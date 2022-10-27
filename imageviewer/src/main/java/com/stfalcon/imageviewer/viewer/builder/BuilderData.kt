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

package com.stfalcon.imageviewer.viewer.builder

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.annotation.StyleRes
import androidx.transition.Transition
import androidx.viewpager.widget.ViewPager
import com.stfalcon.imageviewer.listeners.OnDismissListener
import com.stfalcon.imageviewer.listeners.OnImageChangeListener
import com.stfalcon.imageviewer.loader.ImageLoader

class BuilderData<T>(
    val images: List<T>,
    val imageLoader: ImageLoader<T>
) {
    var backgroundColor = Color.BLACK
    var startPosition: Int = 0
    var imageChangeListener: OnImageChangeListener? = null
    var onDismissListener: OnDismissListener? = null
    var overlayView: View? = null
    var imageMarginPixels: Int = 0
    var containerPaddingPixels = IntArray(4)
    var shouldStatusBarHide = true
    @StyleRes
    var style: Int? = null
    var isZoomingAllowed = true
    var isSwipeToDismissAllowed = true
    var transitionView: ImageView? = null
    var scaleType: ImageView.ScaleType? = null
    val onOpenBeforeScaleType: ImageView.ScaleType? = null
    val onOpenAfterScaleType: ImageView.ScaleType? = null
    var onPageChangeListener: ViewPager.OnPageChangeListener? = null
    var onSingleTapConfirmed: ((MotionEvent) -> Boolean) = { false }
    var onSingleTap: ((MotionEvent) -> Boolean) = { false }
    var onDoubleTap: ((MotionEvent) -> Boolean) = { false }
    var onLongPress: ((MotionEvent) -> Unit) = {}
    var onTransition: () -> Transition? = { null }
}