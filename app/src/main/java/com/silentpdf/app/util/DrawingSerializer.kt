package com.silentpdf.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.silentpdf.app.ui.viewmodel.DrawingStroke
import org.json.JSONArray
import org.json.JSONObject

object DrawingSerializer {

    fun serializeStrokes(strokes: List<DrawingStroke>): String {
        val array = JSONArray()
        for (stroke in strokes) {
            val obj = JSONObject()
            obj.put("color", stroke.color.value.toLong())
            obj.put("width", stroke.width.toDouble())
            obj.put("isEraser", stroke.isEraser)
            val pointsArr = JSONArray()
            for (p in stroke.points) {
                val pt = JSONObject()
                pt.put("x", p.x.toDouble())
                pt.put("y", p.y.toDouble())
                pointsArr.put(pt)
            }
            obj.put("points", pointsArr)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeStrokes(json: String): List<DrawingStroke> {
        val list = mutableListOf<DrawingStroke>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val colorVal = obj.getLong("color")
                val width = obj.getDouble("width").toFloat()
                val isEraser = obj.optBoolean("isEraser", false)
                val pointsArr = obj.getJSONArray("points")
                val points = mutableListOf<Offset>()
                for (j in 0 until pointsArr.length()) {
                    val pt = pointsArr.getJSONObject(j)
                    points.add(Offset(pt.getDouble("x").toFloat(), pt.getDouble("y").toFloat()))
                }
                list.add(
                    DrawingStroke(
                        points = points,
                        color = Color(colorVal.toULong()),
                        width = width,
                        isEraser = isEraser
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
