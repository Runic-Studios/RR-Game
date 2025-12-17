package com.runicrealms.game.items.config.jackson.converter

import com.fasterxml.jackson.databind.util.StdConverter
import com.runicrealms.trove.generated.api.schema.v1.StatType

/** Jackson databind compatible converter for turning Strings into ItemStatTypes */
class StatTypeConverter : StdConverter<String, StatType?>() {

    override fun convert(value: String): StatType {
        for (type in StatType.entries) {
            if (type.name.equals(value, ignoreCase = true)) {
                return type
            }
        }
        throw IllegalArgumentException("Unknown stat type: $value")
    }
}
