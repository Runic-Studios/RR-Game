package com.runicrealms.game.data.extension

import com.runicrealms.game.common.ClassType

data class ClassTypeInfo(val name: String)

val classTypeInfo =
    hashMapOf(
        ClassType.MAGE to ClassTypeInfo("Mage"),
        ClassType.ARCHER to ClassTypeInfo("Archer"),
        ClassType.CLERIC to ClassTypeInfo("Cleric"),
        ClassType.WARRIOR to ClassTypeInfo("Warrior"),
        ClassType.ROGUE to ClassTypeInfo("Rogue"),
        ClassType.ANY to ClassTypeInfo("Any"),
    )

fun ClassType.getInfo(): ClassTypeInfo {
    return classTypeInfo[this]
        ?: throw IllegalArgumentException("Could not get info for classType $this")
}

fun getClassTypeFromIdentifier(identifier: String): ClassType? {
    for (classType in ClassType.entries) {
        if (classType.name.equals(identifier, ignoreCase = true)) {
            return classType
        }
    }
    return null
}
