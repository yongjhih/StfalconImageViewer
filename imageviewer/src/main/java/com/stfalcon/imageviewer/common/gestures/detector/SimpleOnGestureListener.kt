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

package com.stfalcon.imageviewer.common.gestures.detector

import android.view.GestureDetector
import android.view.MotionEvent

class SimpleOnGestureListener(
    private val onSingleTapConfirmed: ((MotionEvent) -> Boolean) = { false },
    private val onSingleTap: ((MotionEvent) -> Boolean) = { false },
    private val onDoubleTap: ((MotionEvent) -> Boolean) = { false },
    private val onLongPress: ((MotionEvent) -> Unit) = {}
) : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(event: MotionEvent): Boolean =
        onSingleTapConfirmed.invoke(event)

    override fun onDoubleTap(event: MotionEvent): Boolean =
        onDoubleTap.invoke(event)

    override fun onSingleTapUp(event: MotionEvent): Boolean = onSingleTap.invoke(event)

    override fun onLongPress(event: MotionEvent) = onLongPress.invoke(event)
}