@file:Suppress("DEPRECATION")

package org.elixir.essence.utils

/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle
import android.os.Message
import android.view.SurfaceControlViewHost
import android.view.SurfaceView

/** Util class to generate surface view requests and parse responses */
object SurfaceViewUtils {
    private const val KEY_HOST_TOKEN = "host_token"
    private const val KEY_VIEW_WIDTH = "width"
    private const val KEY_VIEW_HEIGHT = "height"
    private const val KEY_SURFACE_PACKAGE = "surface_package"
    private const val KEY_CALLBACK = "callback"
    private const val KEY_DISPLAY_ID = "display_id"

    /** Create a surface view request. */
    fun createSurfaceViewRequest(surfaceView: SurfaceView, extras: Bundle?) =
        Bundle().apply {
            putBinder(KEY_HOST_TOKEN, surfaceView.hostToken)
            // TODO(b/305258307): Figure out why SurfaceView.getDisplay returns null in small
            //  preview
            surfaceView.display?.let { putInt(KEY_DISPLAY_ID, it.displayId) }
            putInt(KEY_VIEW_WIDTH, surfaceView.width)
            putInt(KEY_VIEW_HEIGHT, surfaceView.height)
            extras?.let { putAll(it) }
        }

    /** Return the surface package. */
    fun getSurfacePackage(bundle: Bundle): SurfaceControlViewHost.SurfacePackage? {
        return bundle.getParcelable(KEY_SURFACE_PACKAGE)
    }

    /** Return the message callback. */
    fun getCallback(bundle: Bundle): Message? {
        return bundle.getParcelable(KEY_CALLBACK)
    }

}