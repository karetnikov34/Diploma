package ru.netology.diploma.util

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.netology.diploma.R
import ru.netology.diploma.dto.AttachmentType
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

fun numberRepresentation(number: Int): String {

    val suffix = charArrayOf(' ', 'K', 'M', 'B', 'T', 'P', 'E')
    val value = log10(number.toDouble()).toInt()
    val base = value / 3
    return if (value == 4 || value == 5) {
        (number / 10.0.pow(base * 3).toLong()).toString() + suffix[base]
    } else if (value >= 3) {
        DecimalFormat("#.#").format(
            floor(number / (10.0.pow(base * 3)) * 10) / 10
        ) + suffix[base]
    } else {
        number.toString()
    }
}

fun ImageView.loadCircle(url: String) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_download_24)
        .error(R.drawable.ic_error_24)
        .timeout(10_000)
        .circleCrop()
        .into(this)
}

fun ImageView.load(url: String) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_download_24)
        .error(R.drawable.ic_error_24)
        .timeout(10_000)
        .into(this)
}

fun formatDateTime(date: String): String {

    return try {
        val serverDateTime = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        serverDateTime.format(formatter)
    } catch (_: Exception) {
        ""
    }

}

fun published(): String {
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    return currentDateTime.format(formatter)
}

fun publishedEvent(): String {
    val currentDateTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    return currentDateTime.format(formatter)
}

fun formatDateTimeEvent (inputDateTime: String): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")
    val dateTime = LocalDateTime.parse(inputDateTime, formatter)
    val date =  dateTime.format(DateTimeFormatter.ISO_DATE_TIME)
    val originalDateTime = LocalDateTime.parse(date)
    val newDateTime = originalDateTime.withSecond(46).withNano(668000000).atOffset(ZoneOffset.UTC)
    return newDateTime.toString()
}

fun formatDateTimeJob (inputDate: String): String {
    val inputTime = "00:00:00"
    val inputDateString = "$inputDate $inputTime"
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    val localDateTime = LocalDateTime.parse(inputDateString, formatter)
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    return localDateTime.format(outputFormatter)
}

fun formatDateTimeJobBinding(dateTimeString: String): String {
    val serverDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return serverDateTime.format(formatter)
}

fun checkMediaType(input: String): AttachmentType {
    return when (File(input).extension.lowercase(Locale.ROOT)) {
        "mp3", "wav", "ogg" -> AttachmentType.AUDIO
        "mp4", "avi", "mov" -> AttachmentType.VIDEO
        "jpg", "jpeg", "png", "gif" -> AttachmentType.IMAGE
        else -> AttachmentType.Unknown
    }
}

fun getInputStreamFromUri(context: Context?, uri: Uri): InputStream? {
    return context?.contentResolver?.openInputStream(uri)
}

fun createFileFromInputStream(inputStream: InputStream): File {
    val tempFile = File.createTempFile("temp", ".tmp")
    tempFile.outputStream().use { fileOut ->
        inputStream.copyTo(fileOut)
    }
    return tempFile
}