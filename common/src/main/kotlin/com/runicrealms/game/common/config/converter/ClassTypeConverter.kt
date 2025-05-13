package com.runicrealms.game.common.config.converter

import com.fasterxml.jackson.databind.util.StdConverter
import com.runicrealms.trove.generated.api.schema.v1.ClassType

/**
 * Jackson databind compatible converter for turning Strings into ClassTypes
 */
class ClassTypeConverter : StdConverter<String, ClassType?>() {

    override fun convert(value: String): ClassType? {
        for (type in ClassType.entries) {
            if (type.name.equals(value, ignoreCase = true)) {
                return type
            }
        }
        return null
    }

}