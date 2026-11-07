package com.yausername.youtubedl_android

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

object PrimitiveStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("PrimitiveString", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String {
        val value = (decoder as JsonDecoder).decodeJsonElement() as? JsonPrimitive
            ?: throw SerializationException("Expected primitive metadata string")
        return value.content
    }
}
