package com.runicrealms.game.items.config.jackson.deserializer

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.runicrealms.trove.generated.api.schema.v1.StatType

/**
 * Jackson databind compatible key deserializer for turning keys of stats in item YAMLs into ItemStatTypes
 */
class StatTypeKeyDeserializer: KeyDeserializer() {

    override fun deserializeKey(key: String, context: DeserializationContext): Any? {
        for (type in StatType.entries) {
            if (type.name.equals(key, ignoreCase = true)) {
                return type
            }
        }
        return null
    }

}