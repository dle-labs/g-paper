package com.symmetricalpalmtree.gpaper.onyx

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.FrameLayout
import com.symmetricalpalmtree.gpaper.core.Tool
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnyxPaperViewInputTest {
    @Test fun androidStylusRetainsItsUpWhenRawDrawingIsUnavailable() {
        listOf(Tool.PEN, Tool.ERASER).forEach { verifyContact(it, MotionEvent.ACTION_UP) }
    }

    @Test fun androidStylusRetainsItsCancellationWhenRawDrawingIsUnavailable() {
        listOf(Tool.PEN, Tool.ERASER).forEach { verifyContact(it, MotionEvent.ACTION_CANCEL) }
    }

    @Test fun ordinaryFingerStillPassesThrough() {
        val paper = OnyxPaperView(RuntimeEnvironment.getApplication())
        paper.tool = Tool.PEN
        val event = event(MotionEvent.ACTION_DOWN, MotionEvent.TOOL_TYPE_FINGER)
        try {
            assertFalse(paper.onTouchEvent(event))
            assertFalse(paper.isPenDown)
        } finally {
            event.recycle()
            paper.release()
        }
    }

    private fun verifyContact(tool: Tool, end: Int) {
        val context = RuntimeEnvironment.getApplication()
        val paper = OnyxPaperView(context).apply { this.tool = tool }
        // Unattached: no hardware pipeline. Android still owns delivery of UP/CANCEL.
        val parent = FrameLayout(context).apply {
            addView(paper, FrameLayout.LayoutParams(400, 400))
            layout(0, 0, 400, 400)
        }
        paper.layout(0, 0, 400, 400)
        val down = event(MotionEvent.ACTION_DOWN)
        val up = event(end)
        try {
            assertTrue(parent.dispatchTouchEvent(down))
            assertTrue(paper.isPenDown)
            assertTrue(parent.dispatchTouchEvent(up))
            assertFalse(paper.isPenDown)
            assertTrue(paper.getStrokes().isEmpty())
        } finally {
            down.recycle()
            up.recycle()
            paper.release()
        }
    }

    private fun event(action: Int, tool: Int = MotionEvent.TOOL_TYPE_STYLUS): MotionEvent {
        val now = SystemClock.uptimeMillis()
        val pointer = MotionEvent.PointerProperties().apply { id = 0; toolType = tool }
        val position = MotionEvent.PointerCoords().apply { x = 100f; y = 100f; pressure = 0.7f }
        return MotionEvent.obtain(now, now, action, 1, arrayOf(pointer), arrayOf(position),
            0, 0, 1f, 1f, 0, 0,
            if (tool == MotionEvent.TOOL_TYPE_STYLUS) InputDevice.SOURCE_STYLUS else InputDevice.SOURCE_TOUCHSCREEN,
            0)
    }
}
