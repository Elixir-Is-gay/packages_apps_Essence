package org.elixir.essence.utils


import android.app.WallpaperColors
import android.os.Bundle
import android.os.Message
import android.os.RemoteException
import android.util.Log
import android.view.Surface
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean

/** A surface holder callback that renders user's workspace on the passed in surface view.  */
open class WorkspaceSurfaceHolderCallback private constructor(
    private val mWorkspaceSurface: SurfaceView,
    private val mPreviewUtils: PreviewUtils,
    private val mShouldUseWallpaperColors: Boolean,
    private val mExtras: Bundle?
) : SurfaceHolder.Callback {
    /**
     * Listener to be called when workspace surface is updated with a new Surface Package.
     */
    interface WorkspaceRenderListener {
        /**
         * Called on the main thread after the workspace surface is updated from the provider
         */
        fun onWorkspaceRendered()
    }

    private val mRequestPending = AtomicBoolean(false)

    private var mWallpaperColors: WallpaperColors? = null
    private var mHideBottomRow = false
    private var mIsWallpaperColorsReady = false
    private var mLastSurface: Surface? = null
    private var mCallback: Message? = null
    private var mDelayedMessage: Message? = null
    private var mListener: WorkspaceRenderListener? = null

    private var mNeedsToCleanUp = false

    private var mWidth = -1

    private var mHeight = -1

    constructor(
        workspaceSurface: SurfaceView,
        previewUtils: PreviewUtils
    ) : this(workspaceSurface, previewUtils, false, null)

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (mPreviewUtils.supportsPreview() && mLastSurface !== holder.surface) {
            mLastSurface = holder.surface
            maybeRenderPreview()
        }
    }

    /**
     * Render the preview with the current selected [.mWallpaperColors] and
     * [.mHideBottomRow].
     */
    private fun maybeRenderPreview() {
        if ((mShouldUseWallpaperColors && !mIsWallpaperColorsReady) || mLastSurface == null) {
            return
        }
        mRequestPending.set(true)
        requestPreview(mWorkspaceSurface) { result ->
            mRequestPending.set(false)
            if (result != null && mLastSurface != null) {
                val pkg: SurfaceControlViewHost.SurfacePackage? =
                    SurfaceViewUtils.getSurfacePackage(result)
                if (pkg != null) {
                    mWorkspaceSurface.setChildSurfacePackage(pkg)
                } else {
                    Log.w(
                        TAG,
                        "Result bundle from rendering preview does not contain a child "
                                + "surface package."
                    )
                }
                mCallback = SurfaceViewUtils.getCallback(result)
                if (mCallback != null && mDelayedMessage != null) {
                    try {
                        mCallback!!.replyTo.send(mDelayedMessage)
                    } catch (e: RemoteException) {
                        Log.w(TAG, "Couldn't send message to workspace preview", e)
                    }
                    mDelayedMessage = null
                }
                if (mNeedsToCleanUp) {
                    cleanUp()
                } else if (mListener != null) {
                    mListener!!.onWorkspaceRendered()
                }
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if ((mWidth != -1 || mHeight != -1) && (mWidth != width || mHeight != height)) {
            maybeRenderPreview()
        }
        mWidth = width
        mHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    private fun cleanUp() {
        if (mCallback != null) {
            try {
                mCallback!!.replyTo.send(mCallback)
                mNeedsToCleanUp = false
            } catch (e: RemoteException) {
                Log.w(TAG, "Couldn't call cleanup on workspace preview", e)
            } finally {
                mCallback = null
            }
        } else {
            if (mRequestPending.get()) {
                mNeedsToCleanUp = true
            }
        }
    }

    private fun requestPreview(
        workspaceSurface: SurfaceView,
        callback: PreviewUtils.WorkspacePreviewCallback
    ) {
        if (workspaceSurface.display == null) {
            Log.w(
                TAG,
                "No display ID, avoiding asking for workspace preview, lest WallpaperPicker "
                        + "crash"
            )
            return
        }
        val request: Bundle = SurfaceViewUtils.createSurfaceViewRequest(
            workspaceSurface,
            mExtras
        )
        if (mWallpaperColors != null) {
            request.putParcelable(KEY_WALLPAPER_COLORS, mWallpaperColors)
        }
        request.putBoolean("hide_bottom_row", mHideBottomRow)
        mPreviewUtils.renderPreview(request, callback)
    }

    companion object {
        private const val TAG = "WsSurfaceHolderCallback"
        private const val KEY_WALLPAPER_COLORS = "wallpaper_colors"
    }
}