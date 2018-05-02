package com.codeforsanjose.maps.pacmap

import android.content.Context
import java.io.BufferedReader

class StreamUtils {
    companion object {
        fun rawResourceToString(context: Context, resId: Int): String {
            val sb = StringBuilder()
            context.resources.openRawResource(resId).use { inputStream ->
                sb.append(inputStream.bufferedReader().use(BufferedReader::readText))
            }
            return sb.toString()
        }
    }
}