package com.inkcraft.app

import com.inkcraft.app.ai.InkAiEngine
import com.inkcraft.app.ink.InkMath
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.Stroke
import com.inkcraft.app.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class InkMathTest {

    @Test
    fun pressureIncreasesStrokeWidthForFountainAndBrush() {
        val light = StrokePoint(0f, 0f, pressure = 0.1f)
        val heavy = StrokePoint(0f, 0f, pressure = 0.95f)

        val lightFountain = InkMath.computeWidth(light, 6f, PenStyle.FOUNTAIN)
        val heavyFountain = InkMath.computeWidth(heavy, 6f, PenStyle.FOUNTAIN)
        assertTrue(heavyFountain > lightFountain * 2f)

        val lightBrush = InkMath.computeWidth(light, 10f, PenStyle.BRUSH)
        val heavyBrush = InkMath.computeWidth(heavy, 10f, PenStyle.BRUSH)
        assertTrue(heavyBrush > lightBrush * 2f)
    }

    @Test
    fun hitTestAndSplitStrokeWorkCorrectly() {
        val pts = (0..10).map { i -> StrokePoint(x = i * 20f, y = 100f) }
        val stroke = Stroke(points = pts, colorArgb = 0xFF000000, baseWidth = 4f, penStyle = PenStyle.BALLPOINT)

        assertTrue(InkMath.hitTestStroke(stroke, 100f, 105f, 15f))
        assertFalse(InkMath.hitTestStroke(stroke, 100f, 200f, 15f))

        val split = InkMath.splitStrokeByEraser(stroke, 100f, 100f, 25f)
        assertEquals(2, split.size)
        assertTrue(split[0].points.all { it.x < 75f })
        assertTrue(split[1].points.all { it.x > 125f })
    }

    @Test
    fun lassoSelectionFindsEnclosedStroke() {
        val insideStroke = Stroke(
            points = listOf(StrokePoint(50f, 50f), StrokePoint(70f, 70f)),
            colorArgb = 0xFF000000,
            baseWidth = 4f,
            penStyle = PenStyle.FINELINER
        )
        val outsideStroke = Stroke(
            points = listOf(StrokePoint(300f, 300f), StrokePoint(350f, 350f)),
            colorArgb = 0xFF000000,
            baseWidth = 4f,
            penStyle = PenStyle.FINELINER
        )
        val lasso = listOf(
            StrokePoint(0f, 0f),
            StrokePoint(150f, 0f),
            StrokePoint(150f, 150f),
            StrokePoint(0f, 150f)
        )

        assertTrue(InkMath.isStrokeInLasso(insideStroke, lasso))
        assertFalse(InkMath.isStrokeInLasso(outsideStroke, lasso))
    }

    @Test
    fun shapeRecognitionSnapsLineAndCircle() {
        val linePoints = (0..12).map { i ->
            StrokePoint(x = 100f + i * 20f, y = 100f + (if (i % 2 == 0) 2f else -2f))
        }
        val snappedLine = InkMath.recognizeShape(linePoints)
        assertNotNull(snappedLine)
        assertEquals(2, snappedLine!!.size)

        val circlePoints = (0..36).map { i ->
            val a = (2.0 * Math.PI * i / 36).toFloat()
            StrokePoint(x = 300f + 80f * cos(a), y = 300f + 80f * sin(a))
        }
        val snappedCircle = InkMath.recognizeShape(circlePoints)
        assertNotNull(snappedCircle)
        assertTrue(snappedCircle!!.size >= 20)
    }

    @Test
    fun rulerSnapProjectsNearbyPointsOntoLine() {
        val (sx, sy) = InkMath.snapToRuler(
            x = 200f,
            y = 112f,
            rulerCx = 100f,
            rulerCy = 100f,
            rulerAngleRad = 0f,
            snapDistance = 20f
        )
        assertEquals(200f, sx, 0.1f)
        assertEquals(100f, sy, 0.1f)
    }

    @Test
    fun aiSmartInsightsExtractsActionsAndKeyPoints() {
        val sample = "Distributed systems lecture notes.\nRemember to review Raft consensus tomorrow.\nTodo: submit lab report."
        val insights = InkAiEngine.buildSmartInsights(sample)
        assertTrue(insights.wordCount >= 12)
        assertTrue(insights.keyPoints.isNotEmpty())
        assertEquals(2, insights.actionItems.size)
    }
}
