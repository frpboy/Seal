package com.yausername.youtubedl_android.mapper

import kotlinx.serialization.Serializable
import com.yausername.youtubedl_android.PrimitiveStringSerializer
import kotlinx.serialization.SerialName

@Serializable
class VideoFormat {
    val asr = 0
    val tbr = 0
    val abr = 0
    @Serializable(with = PrimitiveStringSerializer::class)
    val format: String? = null

    @SerialName("format_id")
    @Serializable(with = PrimitiveStringSerializer::class)
    val formatId: String? = null

    @SerialName("format_note")
    @Serializable(with = PrimitiveStringSerializer::class)
    val formatNote: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val ext: String? = null
    val preference = 0
    @Serializable(with = PrimitiveStringSerializer::class)
    val vcodec: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val acodec: String? = null
    val width = 0
    val height = 0

    @SerialName("filesize")
    val fileSize: Long = 0

    @SerialName("filesize_approx")
    val fileSizeApproximate: Long = 0
    val fps = 0
    @Serializable(with = PrimitiveStringSerializer::class)
    val url: String? = null

    @SerialName("manifest_url")
    @Serializable(with = PrimitiveStringSerializer::class)
    val manifestUrl: String? = null

    @SerialName("http_headers")
    val httpHeaders: Map<String, String>? = null
}
