package com.runicrealms.game.items.config.jackson.deserializer

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.runicrealms.game.items.config.template.GameItemClickTrigger

/**
 * Jackson databind compatible key deserializer for turning keys of triggers in item YAMLs into
 * GameItemClickTrigger.Types
 */
class GameItemClickTriggerTypeKeyDeserializer : KeyDeserializer() {

    override fun deserializeKey(key: String, context: DeserializationContext): Any? {
        for (type in GameItemClickTrigger.Type.entries) {
            if (type.name.equals(key, ignoreCase = true)) {
                return type
            }
        }
        return null
    }
}
