import com.runicrealms.game.common.getHead
import com.runicrealms.trove.generated.api.schema.v1.ClassType

enum class SubClassType(
    val text: String,
    val position: Int,
    val classType: ClassType,
    headURL: String,
    val description: String,
) {
    /*
    Archer
    */
    MARKSMAN(
        "Marksman",
        1,
        ClassType.ARCHER,
        "http://textures.minecraft.net/texture/5d4da7f7438519be202e58c91f29b869ebd9de2efb1bbf445f745d9fb12287",
        "Marksman is a master of mobility and long-range &cphysical⚔ &7attacks!",
    ),
    STORMSHOT(
        "Stormshot",
        2,
        ClassType.ARCHER,
        "http://textures.minecraft.net/texture/feb10cb8753da80030320be23891a13ffc282d85e6d2b786bcef4ebf231ad2ea",
        "Stormshot is a master of lightning &3magicʔ&7, slinging area-of-effect spells!",
    ),
    WARDEN(
        "Warden",
        3,
        ClassType.ARCHER,
        "http://textures.minecraft.net/texture/558f16bff64988c44e7bcc2ca7852eb39b24e60edaad5fe4883f679e0f3c962",
        "Warden is the keeper of the forest, &ahealing✦ &7allies through the power of nature!",
    ),

    /*
    Cleric
    */
    BARD(
        "Bard",
        1,
        ClassType.CLERIC,
        "http://textures.minecraft.net/texture/ec56f8f96d141e2ab42a589326c6abf635786fa2c8709efd46fdf29f7a2c9274",
        "Bard is a hybrid &3magicalʔ &7and &cphysical⚔ &7fighter who controls the flow of battle with &aally buffs &7and &aenemy debuffs&7!",
    ),
    LIGHTBRINGER(
        "Lightbringer",
        2,
        ClassType.CLERIC,
        "http://textures.minecraft.net/texture/a39a04a2e16d09bc83665f76c78008c3d74cbda1818e801e13be6e7c4bc2f824",
        "Lightbringer blasts enemies with light to &aheal✦ &7allies and keep them strong!",
    ),
    STARWEAVER(
        "Starweaver",
        3,
        ClassType.CLERIC,
        "http://textures.minecraft.net/texture/ca2435d27afedc5a6c400310c8dbaad6cf602c0d7fade4a175cee669eccf5507",
        "Starweaver calls upon the heavens to &ashield &7allies and disable enemies!",
    ),

    /*
    Mage
    */
    CRYOMANCER(
        "Cryomancer",
        1,
        ClassType.MAGE,
        "http://textures.minecraft.net/texture/3168b3b880e80efcbfcc7047aa70c165b53721d538edb6cbad5c3608e8d311fe",
        "Cryomancer freezes and slows enemies with &fcrowd control&7!",
    ),
    PYROMANCER(
        "Pyromancer",
        2,
        ClassType.MAGE,
        "http://textures.minecraft.net/texture/4080bbefca87dc0f36536b6508425cfc4b95ba6e8f5e6a46ff9e9cb488a9ed",
        "Pyromancer deals powerful area-of-effect &3magicʔ &7damage!",
    ),
    SPELLSWORD(
        "Spellsword",
        3,
        ClassType.MAGE,
        "http://textures.minecraft.net/texture/fe0323773c0da5b417718500a448ae3dbf87d49ac068cd53f01502f4c0316153",
        "Spellsword uses magical melee attacks to &ashield &7allies!",
    ),

    /*
    Rogue
    */
    CORSAIR(
        "Corsair",
        1,
        ClassType.ROGUE,
        "http://textures.minecraft.net/texture/b85c1ee61f2bd443c0a9e617f37203cdff440bfa2d00b6dd36ff834cd8702d9",
        "Corsair uses &cphysical⚔ " + "&7projectiles to control the flow of battle!",
    ),
    NIGHTCRAWLER(
        "Nightcrawler",
        2,
        ClassType.ROGUE,
        "http://textures.minecraft.net/texture/6274e1605233425091f7b2837a4bb8f4c804dac80db9e4f599f535c03afab0f8",
        "Nightcrawler emerges " +
            "from the &8shadows &7to quickly burst an opponent with &cphysical⚔ &7strikes!",
    ),
    WITCH_HUNTER(
        "Witch Hunter",
        3,
        ClassType.ROGUE,
        "http://textures.minecraft.net/texture/6a14a4d2d02dcd26a1755eb8561b71b34d59f4518d9944a142c2e115688677d1",
        "Witch Hunter brands a single enemy " + "for persecution!",
    ),

    /*
    Warrior
     */
    BERSERKER(
        "Berserker",
        1,
        ClassType.WARRIOR,
        "http://textures.minecraft.net/texture/26f43caadb4033c41c7f34702c47ff3b21ce97710c8d65307f5876ee45c3f4e5",
        "Berserker fights ferociously with &cphysical⚔ &7attacks that cleave enemies!",
    ),
    DREADLORD(
        "Dreadlord",
        2,
        ClassType.WARRIOR,
        "http://textures.minecraft.net/texture/6d7b1d4eabf35350382b465649964a4f5ad81fbc0c9f4149634829db83d69a3",
        "Dreadlord is a &3magicalʔ &7knight that harvests the souls of enemies!",
    ),
    PALADIN(
        "Paladin",
        3,
        ClassType.WARRIOR,
        "http://textures.minecraft.net/texture/ef63aa9a3f9832353fd78fe6979639c709c1056c7a81163d29ef94d09925c3",
        "Paladin is a hybrid &3magicalʔ &7fighter and &fdefensive■ &7tank!",
    );

    val item = getHead(headURL)

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
