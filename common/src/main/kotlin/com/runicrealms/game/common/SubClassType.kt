import com.runicrealms.game.common.getHead
import com.runicrealms.trove.generated.api.schema.v1.ClassType
import org.bukkit.inventory.ItemStack

enum class SubClassType(
    val text: String,
    val position: Int,
    val classType: ClassType,
    val itemStack: ItemStack,
    val description: String,
) {
    /*
    Archer
    */
    MARKSMAN(
        "Marksman",
        1,
        ClassType.ARCHER,
        marksmanItem(),
        "Marksman is a master of mobility and long-range &cphysical⚔ &7attacks!",
    ),
    STORMSHOT(
        "Stormshot",
        2,
        ClassType.ARCHER,
        stormshotItem(),
        "Stormshot is a master of lightning &3magicʔ&7, slinging area-of-effect spells!",
    ),
    WARDEN(
        "Warden",
        3,
        ClassType.ARCHER,
        wardenItem(),
        "Warden is the keeper of the forest, &ahealing✦ &7allies through the power of nature!",
    ),

    /*
    Cleric
    */
    BARD(
        "Bard",
        1,
        ClassType.CLERIC,
        bardItem(),
        "Bard is a hybrid &3magicalʔ &7and &cphysical⚔ &7fighter who controls the flow of battle with &aally buffs &7and &aenemy debuffs&7!",
    ),
    LIGHTBRINGER(
        "Lightbringer",
        2,
        ClassType.CLERIC,
        lightbringerItem(),
        "Lightbringer blasts enemies with light to &aheal✦ &7allies and keep them strong!",
    ),
    STARWEAVER(
        "Starweaver",
        3,
        ClassType.CLERIC,
        starweaverItem(),
        "Starweaver calls upon the heavens to &ashield &7allies and disable enemies!",
    ),

    /*
    Mage
    */
    CRYOMANCER(
        "Cryomancer",
        1,
        ClassType.MAGE,
        cryomancerItem(),
        "Cryomancer freezes and slows enemies with &fcrowd control&7!",
    ),
    PYROMANCER(
        "Pyromancer",
        2,
        ClassType.MAGE,
        pyromancerItem(),
        "Pyromancer deals powerful area-of-effect &3magicʔ &7damage!",
    ),
    SPELLSWORD(
        "Spellsword",
        3,
        ClassType.MAGE,
        spellswordItem(),
        "Spellsword uses magical melee attacks to &ashield &7allies!",
    ),

    /*
    Rogue
    */
    CORSAIR(
        "Corsair",
        1,
        ClassType.ROGUE,
        corsairItem(),
        "Corsair uses &cphysical⚔ " + "&7projectiles to control the flow of battle!",
    ),
    NIGHTCRAWLER(
        "Nightcrawler",
        2,
        ClassType.ROGUE,
        nightcrawlerItem(),
        "Nightcrawler emerges " +
            "from the &8shadows &7to quickly burst an opponent with &cphysical⚔ &7strikes!",
    ),
    WITCH_HUNTER(
        "Witch Hunter",
        3,
        ClassType.ROGUE,
        witchHunterItem(),
        "Witch Hunter brands a single enemy " + "for persecution!",
    ),

    /*
    Warrior
     */
    BERSERKER(
        "Berserker",
        1,
        ClassType.WARRIOR,
        berserkerItem(),
        "Berserker fights ferociously with &cphysical⚔ &7attacks that cleave enemies!",
    ),
    DREADLORD(
        "Dreadlord",
        2,
        ClassType.WARRIOR,
        dreadlordItem(),
        "Dreadlord is a &3magicalʔ &7knight that harvests the souls of enemies!",
    ),
    PALADIN(
        "Paladin",
        3,
        ClassType.WARRIOR,
        paladinItem(),
        "Paladin is a hybrid &3magicalʔ &7fighter and &fdefensive■ &7tank!",
    );

    companion object {
        val ARCHER_SUBCLASSES: MutableSet<SubClassType> = LinkedHashSet()
        val CLERIC_SUBCLASSES: MutableSet<SubClassType> = LinkedHashSet()
        val MAGE_SUBCLASSES: MutableSet<SubClassType> = LinkedHashSet()
        val ROGUE_SUBCLASSES: MutableSet<SubClassType> = LinkedHashSet()
        val WARRIOR_SUBCLASSES: MutableSet<SubClassType> = LinkedHashSet()

        init {
            for (subClass in entries) {
                when (subClass.classType) {
                    ClassType.ANY -> {}
                    ClassType.UNRECOGNIZED -> {}
                    ClassType.ARCHER -> ARCHER_SUBCLASSES.add(subClass)
                    ClassType.CLERIC -> CLERIC_SUBCLASSES.add(subClass)
                    ClassType.MAGE -> MAGE_SUBCLASSES.add(subClass)
                    ClassType.ROGUE -> ROGUE_SUBCLASSES.add(subClass)
                    ClassType.WARRIOR -> WARRIOR_SUBCLASSES.add(subClass)
                }
            }
        }
    }
}

private fun marksmanItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQ0ZGE3Zjc0Mzg1MTliZTIwMmU1OGM5MWYyOWI4NjllYmQ5ZGUyZWZiMWJiZjQ0NWY3NDVkOWZiMTIyODcifX19"
    )
}

private fun stormshotItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmViMTBjYjg3NTNkYTgwMDMwMzIwYmUyMzg5MWExM2ZmYzI4MmQ4NWU2ZDJiNzg2YmNlZjRlYmYyMzFhZDJlYSJ9fX0="
    )
}

private fun wardenItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTU4ZjE2YmZmNjQ5ODhjNDRlN2JjYzJjYTc4NTJlYjM5YjI0ZTYwZWRhYWQ1ZmU0ODgzZjY3OWUwZjNjOTYyIn19fQ=="
    )
}

private fun bardItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3" +
            "RleHR1cmUvZWM1NmY4Zjk2ZDE0MWUyYWI0MmE1ODkzMjZjNmFiZjYzNTc4NmZhMmM4NzA5ZWZkNDZmZGYyOWY3YTJjOTI3NCJ9fX0="
    )
}

private fun starweaverItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTM5YTA0YTJlMTZkMDliYzgzNjY1Zjc2Yzc4MDA4YzNkNzRjYmRhMTgxOGU4MDFlMTNiZTZlN2M0YmMyZjgyNCJ9fX0="
    )
}

private fun lightbringerItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2EyNDM1ZDI3YWZlZGM1YTZjNDAwMzEwYzhkYmFhZDZjZjYwMmMwZDdmYWRlNGExNzVjZWU2NjllY2NmNTUwNyJ9fX0="
    )
}

private fun cryomancerItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzE2OGIzYjg4MGU4MGVmY2JmY2M3MDQ3YWE3MGMxNjViNTM3MjFkNTM4ZWRiNmNiYWQ1YzM2MDhlOGQzMTFmZSJ9fX0="
    )
}

private fun pyromancerItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3R" +
            "leHR1cmUvNDA4MGJiZWZjYTg3ZGMwZjM2NTM2YjY1MDg0MjVjZmM0Yjk1YmE2ZThmNWU2YTQ2ZmY5ZTljYjQ4OGE5ZWQifX19"
    )
}

private fun spellswordItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmUwMzIzNzczYzBkYTViNDE3NzE4NTAwYTQ0OGFlM2RiZjg3ZDQ5YWMwNjhjZDUzZjAxNTAyZjRjMDMxNjE1MyJ9fX0="
    )
}

private fun nightcrawlerItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjg1YzFlZTYxZjJiZDQ0M2MwYTllNjE3ZjM3MjAzY2RmZjQ0MGJmYTJkMDBiNmRkMzZmZjgzNGNkODcwMmQ5In19fQ=="
    )
}

private fun witchHunterItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjI3NGUxNjA1MjMzNDI1MDkxZjdiMjgzN2E0YmI4ZjRjODA0ZGFjODBkYjllNGY1OTlmNTM1YzAzYWZhYjBmOCJ9fX0="
    )
}

private fun corsairItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmExNGE0ZDJkMDJkY2QyNmExNzU1ZWI4NTYxYjcxYjM0ZDU5ZjQ1MThkOTk0NGExNDJjMmUxMTU2ODg2NzdkMSJ9fX0="
    )
}

private fun berserkerItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjZmNDNjYWFkYjQwMzNjNDFjN2YzNDcwMmM0N2ZmM2IyMWNlOTc3MTBjOGQ2NTMwN2Y1ODc2ZWU0NWMzZjRlNSJ9fX0="
    )
}

private fun dreadlordItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQ3YjFkNGVhYmYzNTM1MDM4MmI0NjU2NDk5NjRhNGY1YWQ4MWZiYzBjOWY0MTQ5NjM0ODI5ZGI4M2Q2OWEzIn19fQ=="
    )
}

private fun paladinItem(): ItemStack {
    return getHead(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3R" +
            "leHR1cmUvZWY2M2FhOWEzZjk4MzIzNTNmZDc4ZmU2OTc5NjM5YzcwOWMxMDU2YzdhODExNjNkMjllZjk0ZDA5OTI1YzMifX19"
    )
}
