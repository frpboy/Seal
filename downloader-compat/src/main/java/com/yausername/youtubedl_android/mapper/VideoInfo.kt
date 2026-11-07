package com.yausername.youtubedl_android.mapper

import kotlinx.serialization.Serializable
import com.yausername.youtubedl_android.PrimitiveStringSerializer
import kotlinx.serialization.SerialName

@Serializable
class VideoInfo {
    @Serializable(with = PrimitiveStringSerializer::class)
    val id: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val fulltitle: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val title: String? = null

    @SerialName("upload_date")
    @Serializable(with = PrimitiveStringSerializer::class)
    val uploadDate: String? = null

    @SerialName("display_id")
    @Serializable(with = PrimitiveStringSerializer::class)
    val displayId: String? = null
    val duration = 0
    @Serializable(with = PrimitiveStringSerializer::class)
    val description: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val thumbnail: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val license: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val extractor: String? = null

    @SerialName("extractor_key")
    @Serializable(with = PrimitiveStringSerializer::class)
    val extractorKey: String? = null

    @SerialName("view_count")
    @Serializable(with = PrimitiveStringSerializer::class)
    val viewCount: String? = null

    @SerialName("like_count")
    @Serializable(with = PrimitiveStringSerializer::class)
    val likeCount: String? = null

    @SerialName("dislike_count")
    @Serializable(with = PrimitiveStringSerializer::class)
    val dislikeCount: String? = null

    @SerialName("repost_count")
    @Serializable(with = PrimitiveStringSerializer::class)
    val repostCount: String? = null

    @SerialName("average_rating")
    @Serializable(with = PrimitiveStringSerializer::class)
    val averageRating: String? = null

    @SerialName("uploader_id")
    @Serializable(with = PrimitiveStringSerializer::class)
    val uploaderId: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val uploader: String? = null

    @SerialName("player_url")
    @Serializable(with = PrimitiveStringSerializer::class)
    val playerUrl: String? = null

    @SerialName("webpage_url")
    @Serializable(with = PrimitiveStringSerializer::class)
    val webpageUrl: String? = null

    @SerialName("webpage_url_basename")
    @Serializable(with = PrimitiveStringSerializer::class)
    val webpageUrlBasename: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val resolution: String? = null
    val width = 0
    val height = 0
    @Serializable(with = PrimitiveStringSerializer::class)
    val format: String? = null

    @SerialName("format_id")
    @Serializable(with = PrimitiveStringSerializer::class)
    val formatId: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val ext: String? = null

    @SerialName("filesize")
    val fileSize: Long = 0

    @SerialName("filesize_approx")
    val fileSizeApproximate: Long = 0

    @SerialName("http_headers")
    val httpHeaders: Map<String, String>? = null
    val categories: ArrayList<String>? = null
    val tags: ArrayList<String>? = null

    @SerialName("requested_formats")
    val requestedFormats: ArrayList<VideoFormat>? = null
    val formats: ArrayList<VideoFormat>? = null
    val thumbnails: ArrayList<VideoThumbnail>? = null

    //private ArrayList<VideoSubtitle> subtitles;
    @SerialName("manifest_url")
    @Serializable(with = PrimitiveStringSerializer::class)
    val manifestUrl: String? = null
    @Serializable(with = PrimitiveStringSerializer::class)
    val url: String? = null
}
