package com.runicrealms.game.data.model

import com.mongodb.MongoClientSettings
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.ProfessionType
import com.runicrealms.game.common.StatType
import com.runicrealms.game.common.WorldType
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider

/**
 * Central BSON codec registry for all PlayerDocument model types.
 *
 * Design notes:
 * 1. UUID: stored as BSON binary subtype 4 (UUID_STANDARD). MongoClientSettings must be configured
 *    with UuidRepresentation.STANDARD to match.
 * 2. Instant: stored as a BSON Date (milliseconds since epoch). The POJO codec maps
 *    java.time.Instant automatically when the KotlinSerialization or POJO provider is used
 *    alongside the JavaTimeCodecProvider.
 * 3. Duration: we store play-time as Long milliseconds (playTimeMillis) directly in the data class,
 *    so no custom codec is needed. If you need java.time.Duration elsewhere, add a DurationCodec
 *    here that reads/writes as a Long.
 * 4. sealed class ItemTypeData: the POJO codec needs a discriminator to distinguish ArmorData /
 *    WeaponData / GemData / OffhandData at deserialisation time. Each subclass is annotated
 *    with @BsonDiscriminator("armor") etc. (see the model classes). The discriminator key used in
 *    BSON is "_type".
 */
object BsonCodecs {

    /**
     * Builds the full codec registry to pass to MongoClientSettings or getCollection().
     *
     * Call order matters: custom codecs are checked before the POJO provider, which is checked
     * before the Mongo defaults.
     */
    fun buildRegistry(): CodecRegistry {
        val pojoProvider =
            PojoCodecProvider.builder()
                .automatic(true)
                // Register all packages that contain model classes
                .register("com.runicrealms.game.data.model")
                .register("com.runicrealms.game.common")
                .build()

        return CodecRegistries.fromRegistries(
            // Custom codecs for types the POJO provider cannot handle automatically
            CodecRegistries.fromCodecs(
                EnumCodec(ClassType::class.java),
                EnumCodec(StatType::class.java),
                EnumCodec(ProfessionType::class.java),
                EnumCodec(WorldType::class.java),
            ),
            // Mongo defaults MUST come before the POJO provider so that primitive/built-in types
            // like UUID and Instant are handled by their proper binary codecs rather than being
            // serialised as plain POJO sub-documents (e.g. UUID -> {leastSignificantBits, ...}).
            MongoClientSettings.getDefaultCodecRegistry(),
            // POJO-based mapping for all data classes
            CodecRegistries.fromProviders(pojoProvider),
        )
    }

    /**
     * Generic codec that serialises Kotlin enum values as their [name] string and deserialises by
     * matching against [enumType].entries. This is more robust than relying on ordinal values,
     * which change when entries are reordered.
     */
    class EnumCodec<T : Enum<T>>(private val enumType: Class<T>) : Codec<T> {
        override fun getEncoderClass(): Class<T> = enumType

        override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
            writer.writeString(value.name)
        }

        override fun decode(reader: BsonReader, decoderContext: DecoderContext): T {
            val name = reader.readString()
            return enumType.enumConstants.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("Unknown ${enumType.simpleName} value: '$name'")
        }
    }
}
