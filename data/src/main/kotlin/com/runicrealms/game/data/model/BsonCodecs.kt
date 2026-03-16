package com.runicrealms.game.data.model

import com.mongodb.MongoClientSettings
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.common.ProfessionType
import com.runicrealms.game.common.StatType
import com.runicrealms.game.common.WorldType
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.BsonReader
import org.bson.BsonString
import org.bson.BsonWriter
import org.bson.UuidRepresentation
import org.bson.codecs.BsonDocumentCodec
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.UuidCodec
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.jsr310.InstantCodec
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
        lateinit var fullRegistry: CodecRegistry
        val itemTypeDataCodec = ItemTypeDataCodec { fullRegistry }
        val pojoProvider =
            PojoCodecProvider.builder()
                .automatic(true)
                // Register all packages that contain model classes
                .register("com.runicrealms.game.data.model")
                .register("com.runicrealms.game.common")
                // Explicitly register sealed hierarchy so abstract base ItemTypeData can be
                // resolved when encountered as a declared field type.
                .register(
                    ItemTypeData::class.java,
                    ArmorData::class.java,
                    WeaponData::class.java,
                    GemData::class.java,
                    OffhandData::class.java,
                )
                .build()

        fullRegistry =
            CodecRegistries.fromRegistries(
                // Custom codecs for types the POJO provider cannot handle automatically
                CodecRegistries.fromCodecs(
                    itemTypeDataCodec,
                    UuidCodec(UuidRepresentation.STANDARD),
                    InstantCodec(),
                    EnumCodec(ClassType::class.java),
                    EnumCodec(StatType::class.java),
                    EnumCodec(ProfessionType::class.java),
                    EnumCodec(WorldType::class.java),
                ),
                MongoClientSettings.getDefaultCodecRegistry(),
                // POJO mapping comes after defaults so core scalar types (String, Double, UUID,
                // etc.)
                // continue to use Mongo's built-in codecs instead of being treated as POJOs.
                CodecRegistries.fromProviders(pojoProvider),
            )
        return fullRegistry
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

    /**
     * Custom polymorphic codec for [ItemTypeData].
     *
     * Mongo's Kotlin DataClass codec cannot infer a codec for sealed base classes declared as field
     * types. We store a discriminator (`_type`) and delegate the payload to the concrete subtype
     * codec from the same registry.
     */
    class ItemTypeDataCodec(private val registryProvider: () -> CodecRegistry) :
        Codec<ItemTypeData> {
        private val documentCodec = BsonDocumentCodec()

        override fun getEncoderClass(): Class<ItemTypeData> = ItemTypeData::class.java

        override fun encode(
            writer: BsonWriter,
            value: ItemTypeData,
            encoderContext: EncoderContext,
        ) {
            val (typeName, subtypeClass) =
                when (value) {
                    is ArmorData -> "armor" to ArmorData::class.java
                    is WeaponData -> "weapon" to WeaponData::class.java
                    is GemData -> "gem" to GemData::class.java
                    is OffhandData -> "offhand" to OffhandData::class.java
                }

            val payload = BsonDocument()
            @Suppress("UNCHECKED_CAST")
            val payloadCodec = registryProvider().get(subtypeClass) as Codec<ItemTypeData>
            payloadCodec.encode(
                BsonDocumentWriter(payload),
                value,
                EncoderContext.builder().isEncodingCollectibleDocument(false).build(),
            )

            val out = BsonDocument("_type", BsonString(typeName))
            for ((key, bsonValue) in payload) {
                out[key] = bsonValue
            }
            documentCodec.encode(writer, out, encoderContext)
        }

        override fun decode(reader: BsonReader, decoderContext: DecoderContext): ItemTypeData {
            val raw = documentCodec.decode(reader, decoderContext)

            // Support both {_type, ...payload} and legacy {_t, ...payload} layouts.
            val typeName =
                raw.getString("_type").value
                    ?: throw IllegalArgumentException(
                        "Missing ItemTypeData discriminator (_type/_t)"
                    )
            val dataValue = raw["data"]
            val payload =
                if (dataValue != null && dataValue.isDocument) {
                    raw.getDocument("data")
                } else {
                    BsonDocument().also { copy ->
                        for ((key, bsonValue) in raw) {
                            if (key != "_type") {
                                copy[key] = bsonValue
                            }
                        }
                    }
                }

            return when (typeName) {
                "armor" ->
                    registryProvider()
                        .get(ArmorData::class.java)
                        .decode(BsonDocumentReader(payload), decoderContext)
                "weapon" ->
                    registryProvider()
                        .get(WeaponData::class.java)
                        .decode(BsonDocumentReader(payload), decoderContext)
                "gem" ->
                    registryProvider()
                        .get(GemData::class.java)
                        .decode(BsonDocumentReader(payload), decoderContext)
                "offhand" ->
                    registryProvider()
                        .get(OffhandData::class.java)
                        .decode(BsonDocumentReader(payload), decoderContext)
                else ->
                    throw IllegalArgumentException(
                        "Unknown ItemTypeData discriminator: '$typeName'"
                    )
            }
        }
    }
}
