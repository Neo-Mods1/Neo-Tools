package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import android.widget.RelativeLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView

@Composable
fun PhotoEditorHost(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onReady: (PhotoEditor) -> Unit = {},
) {
    val context = LocalContext.current

    key(bitmap) {
        AndroidView(
            factory = { ctx ->
                val view = PhotoEditorView(ctx).apply {
                    id = android.R.id.content
                    layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                    )
                }
                view.source.setImageBitmap(bitmap)

                val editor = PhotoEditor.Builder(ctx, view)
                    .setPinchTextScalable(true)
                    .setClipSourceImage(false)
                    .build()

                onReady(editor)
                view
            },
            modifier = modifier,
        )
    }
}
