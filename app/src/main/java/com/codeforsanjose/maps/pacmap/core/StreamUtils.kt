package com.codeforsanjose.maps.pacmap.core

import android.content.Context
import android.util.Log
import com.codeforsanjose.maps.pacmap.zone.ZoneManager
import com.mapbox.mapboxsdk.Mapbox
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

class StreamUtils {
    companion object {

        fun rawResourceToString(context: Context, resId: Int): String {
            val sb = StringBuilder()
            context.resources.openRawResource(resId).use { inputStream ->
                sb.append(inputStream.bufferedReader().use(BufferedReader::readText))
            }
            return sb.toString()
        }

        fun loadFileFromAsset(filename: String): String? {
            return try {
                val inputString = Mapbox.getApplicationContext().assets.open(filename)
                val size = inputString.available()
                val buffer = ByteArray(size)
                inputString.read(buffer)
                inputString.close()
                String(buffer, StandardCharsets.UTF_8)
            } catch (exception: Exception) {
                Timber.e("Exception Loading file from Assets: %s", exception.toString())
                exception.printStackTrace()
                null
            }
        }

        fun writeResponseBodyToAssets(body: ResponseBody?): Boolean {
            if (body == null) {
                Timber.w("Null response body")
                return false
            }
            try {
                val filename = ZoneManager.GEOJSON_FILENAME
                Mapbox.getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE).use { outputStream ->
                    var inputStream: InputStream? = null

                    try {
                        val fileReader = ByteArray(4096)
                        val fileSize = body.contentLength()
                        var fileSizeDownloaded: Long = 0

                        inputStream = body.byteStream()

                        while (true) {
                            val read = inputStream!!.read(fileReader)
                            if (read == -1) {
                                break
                            }

                            outputStream.write(fileReader, 0, read)
                            fileSizeDownloaded += read.toLong()

                            Timber.d("file download: $fileSizeDownloaded of $fileSize")
                        }
                        outputStream.flush()
                        return true
                    } catch (e: IOException) {
                        Timber.e(e)
                        return false
                    } finally {
                        inputStream?.close()
                        outputStream?.close()
                    }
                }
            } catch (e: IOException) {
                Timber.e(e)
                return false
            }
        }
    }
}