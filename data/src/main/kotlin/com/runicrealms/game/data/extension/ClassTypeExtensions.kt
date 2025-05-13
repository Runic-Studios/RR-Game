package com.runicrealms.game.data.extension

import com.runicrealms.trove.generated.api.schema.v1.ClassType

data class ClassTypeInfo(
    val name: String
)

val classTypeInfo = hashMapOf(
    ClassType.MAGE to ClassTypeInfo("Mage"),
    ClassType.ARCHER to ClassTypeInfo("Archer"),
    ClassType.CLERIC to ClassTypeInfo("Cleric"),
    ClassType.WARRIOR to ClassTypeInfo("Warrior"),
    ClassType.ROGUE to ClassTypeInfo("Rogue"),
    ClassType.UNRECOGNIZED to ClassTypeInfo("Unrecognized"),
)

fun ClassType.getInfo(): ClassTypeInfo {
    return classTypeInfo[this] ?: throw IllegalArgumentException("Could not get info for classType $this")
}

