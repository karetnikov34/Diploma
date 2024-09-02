package ru.netology.diploma.model

import android.net.Uri
import ru.netology.diploma.dto.AttachmentType
import java.io.File

data class AttachmentModel(
    val uri: Uri,
    val file: File,
    val type: AttachmentType
)