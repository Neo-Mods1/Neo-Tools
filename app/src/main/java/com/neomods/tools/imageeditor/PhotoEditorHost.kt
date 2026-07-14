package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.PhotoFilter

/**
 * Compose wrapper around the PhotoEditor library's [PhotoEditorView].
 *
 * Hosts the View-based editor inside Compose and exposes a clean
 * [PhotoEditor] API that the rest of the app can drive.
 */
@Composable
fun PhotoEditorHost(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onReady: (PhotoEditor) -> Unit = {},
) {
    val host = remember { PhotoEditorState(bitmap) }

    AndroidView(
        factory = { ctx ->
            val view = PhotoEditorView(ctx).apply {
                id = android.R.id.content
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                )
            }
            view.source.setImageBitmap(host.bitmap)

            val editor = PhotoEditor.Builder(ctx, view)
                .setPinchTextScalable(true)
                .setClipSourceImage(false)
                .build()

            host.editor = editor
            onReady(editor)
            view
        },
        modifier = modifier,
    )
}

/** Simple state holder so [PhotoEditorHost] can remember across recompositions. */
class PhotoEditorState(val bitmap: Bitmap) {
    var editor: PhotoEditor? = null
    val isUndoAvailable: Boolean get() = editor?.isUndoAvailable == true
    val isRedoAvailable: Boolean get() = editor?.isRedoAvailable == true
}
