package com.yausername.youtubedl_android.mapper

import kotlinx.serialization.Serializable
import com.yausername.youtubedl_android.PrimitiveStringSerializer

@Serializable
class VideoThumbnail {
    @Serializable(with = PrimitiveStringSerializer::class)
    val url: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val id: String? = null
}
