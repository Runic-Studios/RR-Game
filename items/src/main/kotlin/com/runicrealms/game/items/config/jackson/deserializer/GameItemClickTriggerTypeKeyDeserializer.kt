package com.runicrealms.game.items.config.jackson.deserializer

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.runicrealms.game.items.config.template.GameItemClickTrigger

/**
 * Jackson databind compatible key deserializer for turning keys of triggers in item YAMLs into
 * GameItemClickTrigger.Types
 */
class GameItemClickTriggerTypeKeyDeserializer : KeyDeserializer() {

    override fun deserializeKey(key: String, context: DeserializationContext): Any {
        return GameItemClickTrigger.Type.getFromIdentifier(key)
            ?: throw IllegalArgumentException("Unknown item click trigger type: $key")
    }
}
