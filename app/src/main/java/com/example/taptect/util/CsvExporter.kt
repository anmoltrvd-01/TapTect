package com.example.taptect.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.taptect.data.TapRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportRecords(context: Context, records: List<TapRecord>) {
        val fileName = "tap_records_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        val header = "ID,Timestamp,Material,PeakFrequency,DecayRate,Density,Note\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        try {
            FileOutputStream(file).use { out ->
                out.write(header.toByteArray())
                records.forEach { record ->
                    val line = "${record.id}," +
                            "${dateFormat.format(Date(record.timestamp))}," +
                            "${record.materialType}," +
                            "${record.peakFrequency}," +
                            "${record.decayRate}," +
                            "${record.densityScore}," +
                            "${record.userNote ?: ""}\n"
                    out.write(line.toByteArray())
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Tap History"))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
