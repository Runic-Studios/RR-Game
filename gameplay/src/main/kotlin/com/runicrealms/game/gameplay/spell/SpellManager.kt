package com.runicrealms.game.gameplay.spell

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.inject.Inject
import com.google.inject.Singleton
import com.runicrealms.game.common.ClassType
import com.runicrealms.game.data.UserDataRegistry
import com.runicrealms.game.gameplay.spell.api.SpellEffectAPI
import com.runicrealms.game.gameplay.spell.api.StatusEffectAPI
import com.runicrealms.game.gameplay.spell.event.SpellHealEvent
import com.runicrealms.game.gameplay.spell.event.SpellShieldEvent
import com.runicrealms.game.gameplay.spell.spells.Combat
import com.runicrealms.game.gameplay.spell.spells.Consumable
import com.runicrealms.game.gameplay.spell.spells.Potion
import com.runicrealms.game.gameplay.spell.spells.archer.*
import com.runicrealms.game.gameplay.spell.spells.cleric.*
import com.runicrealms.game.gameplay.spell.spells.mage.*
import com.runicrealms.game.gameplay.spell.spells.rogue.*
import com.runicrealms.game.gameplay.spell.spells.warrior.*
import com.runicrealms.game.gameplay.spell.spelltypes.Shield
import com.runicrealms.game.gameplay.spell.spelltypes.ShieldPayload
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellManagerBridge
import com.runicrealms.game.gameplay.spell.spelltypes.SpellSlot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gameplay")

/**
 * Central spell registry and runtime state manager. Responsibilities:
 * - Instantiates and registers all spell objects
 * - Tracks per-player cooldowns
 * - Tracks shielded players
 * - Provides [healPlayer] and [shieldPlayer] helpers
 * - Drives the action-bar cooldown display via a periodic coroutine
 * - Manages current mana (deducted/read by Spell.execute)
 *
 * Implements [SpellManagerBridge] so [Spell] instances can call back into it.
 */
@Singleton
class SpellManager
@Inject
constructor(
    private val plugin: Plugin,
    private val spellEffectAPI: SpellEffectAPI,
    private val statusEffectAPI: StatusEffectAPI,
    private val userDataRegistry: UserDataRegistry,
    private val spellDependencies: SpellDependencies,
) : SpellManagerBridge {

    /** Maps spellName.lowercase() -> Spell */
    private val spellRegistry: MutableMap<String, Spell> = HashMap()

    /** player UUID -> (spell -> expiry time ms) */
    private val cooldownMap: ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> =
        ConcurrentHashMap()

    /** player UUID -> ShieldPayload */
    private val shieldedPlayers: ConcurrentHashMap<UUID, ShieldPayload> = ConcurrentHashMap()

    /** player UUID -> current mana (mirrors RegenManager but tracked here for instant read) */
    private val manaMap: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()

    init {
        registerAllSpells()
        startCooldownTask()
    }

    // --- SpellManagerBridge ---

    override fun getMana(uuid: UUID): Int = manaMap.getOrDefault(uuid, 0)

    override fun setMana(uuid: UUID, amount: Int) {
        manaMap[uuid] = amount
    }

    override fun addCooldown(player: Player, spell: Spell, cooldownSeconds: Double) {
        cooldownMap.getOrPut(player.uniqueId) { ConcurrentHashMap() }[spell.name.lowercase()] =
            System.currentTimeMillis() + (cooldownSeconds * 1000L).toLong()
    }

    override fun isOnCooldown(player: Player, spellName: String): Boolean {
        val expiry = cooldownMap[player.uniqueId]?.get(spellName.lowercase()) ?: return false
        return System.currentTimeMillis() < expiry
    }

    override fun healPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell?) {
        val event = SpellHealEvent(amount.toInt(), recipient, caster, spell)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return
        val maxHp = recipient.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        recipient.health = minOf(recipient.health + event.amount, maxHp)
        recipient.world.spawnParticle(
            Particle.HEART,
            recipient.location.add(0.0, 2.0, 0.0),
            3,
            0.3,
            0.3,
            0.3,
        )
        recipient.world.playSound(
            recipient.location,
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            0.5f,
            1.0f,
        )
    }

    override fun shieldPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell?) {
        val event = SpellShieldEvent(caster, recipient, spell, amount.toInt())
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return
        val shield = Shield(amount, System.currentTimeMillis(), caster.uniqueId)
        shieldedPlayers[recipient.uniqueId] = ShieldPayload(recipient, caster, shield)
    }

    // --- Public API (also satisfies SpellManagerBridge) ---

    override fun getSpell(name: String): Spell? = spellRegistry[name.lowercase()]

    fun getSpells(): Collection<Spell> = spellRegistry.values

    fun getShieldedPlayers(): Map<UUID, ShieldPayload> = shieldedPlayers

    fun removeShield(uuid: UUID) {
        shieldedPlayers.remove(uuid)
    }

    override fun isShielded(player: Player): Boolean = shieldedPlayers.containsKey(player.uniqueId)

    /** Returns the remaining cooldown in seconds for the given spell, or 0.0 if not on cooldown. */
    fun getRemainingCooldown(player: Player, spellName: String): Double {
        val expiry = cooldownMap[player.uniqueId]?.get(spellName.lowercase()) ?: return 0.0
        val remaining = expiry - System.currentTimeMillis()
        return if (remaining <= 0) 0.0 else remaining / 1000.0
    }

    /**
     * Reduces a spell's remaining cooldown by [reductionSeconds]. Does nothing if the spell is not
     * currently on cooldown.
     */
    override fun reduceCooldown(player: Player, spellName: String, reductionSeconds: Double) {
        val playerCooldowns = cooldownMap[player.uniqueId] ?: return
        val name = spellName.lowercase()
        val expiry = playerCooldowns[name] ?: return
        val newExpiry = expiry - (reductionSeconds * 1000L).toLong()
        if (newExpiry <= System.currentTimeMillis()) {
            playerCooldowns.remove(name)
        } else {
            playerCooldowns[name] = newExpiry
        }
    }

    /**
     * Looks up the spell assigned to [slot] from the player's skill tree data. Returns null if no
     * spell is assigned or the player has no active character.
     */
    fun getPlayerSpell(player: Player, slot: SpellSlot): Spell? {
        val slotIndex =
            when (slot) {
                SpellSlot.HOT_BAR_ONE -> 0
                SpellSlot.LEFT_CLICK -> 1
                SpellSlot.RIGHT_CLICK -> 2
                SpellSlot.SWAP_HANDS -> 3
            }
        val spellName =
            spellDependencies.skillTreeAPI.getPlayerSpellName(player.uniqueId, 0, slotIndex)
                ?: return null
        return getSpell(spellName)
    }

    /** Returns the player's current class type, or [ClassType.ANY] if not loaded. */
    fun getPlayerClassType(uuid: UUID): ClassType {
        val character = userDataRegistry.getCharacter(uuid) ?: return ClassType.ANY
        return character.withSyncCharacterData { traits.classType }
    }

    // --- Internal ---

    private fun registerAllSpells() {
        val deps = spellDependencies
        val spells: List<Spell> =
            listOf(
                // Utility spells (any class)
                Combat(deps),
                Potion(deps),
                Consumable(deps),
                // Mage (fully migrated)
                ArcaneSlash(deps),
                Blink(deps),
                Blizzard(deps),
                DragonsBreath(deps),
                Erupt(deps),
                Fireball(deps),
                Frostbite(deps),
                Glacier(deps),
                Incendiary(deps),
                Meteor(deps),
                PrimalArcanum(deps),
                Riftwalk(deps),
                Shatter(deps),
                SnapFreeze(deps),
                SpectralBlade(deps),
                Wildfire(deps),
                Ambush(deps),
                Fade(deps),
                GiftsOfTheGrove(deps),
                Jolt(deps),
                LeapingShot(deps),
                Overcharge(deps),
                PiercingArrow(deps),
                RainOfArrows(deps),
                RapidFire(deps),
                RefreshingVolley(deps),
                Remedy(deps),
                SacredGrove(deps),
                SnareTrap(deps),
                SteadyAim(deps),
                Stormborn(deps),
                Surge(deps),
                ThunderArrow(deps),
                Accelerando(deps),
                AstralBlessing(deps),
                Battlecry(deps),
                CosmicPrism(deps),
                Consecration(deps),
                Diminuendo(deps),
                GrandSymphony(deps),
                Lightwell(deps),
                Nightfall(deps),
                Powerslide(deps),
                Purify(deps),
                RadiantFire(deps),
                RadiantNova(deps),
                RayOfLight(deps),
                Rejuvenate(deps),
                SacredSpring(deps),
                Sear(deps),
                Starlight(deps),
                Tempo(deps),
                TwilightResurgence(deps),
                Backstab(deps),
                CallOfTheDeep(deps),
                Cannonfire(deps),
                Castigate(deps),
                Cocoon(deps),
                Dash(deps),
                Flay(deps),
                FromTheShadows(deps),
                Harpoon(deps),
                Hereticize(deps),
                Scurvy(deps),
                SilverBolt(deps),
                TwinFangs(deps),
                Unseen(deps),
                WardingGlyph(deps),
                Whirlpool(deps),
                Adrenaline(deps),
                AxeToss(deps),
                BlessedBlade(deps),
                Bloodbath(deps),
                Cleave(deps),
                Consecrate(deps),
                Damnation(deps),
                Devour(deps),
                Ruination(deps),
                Rupture(deps),
                SacredWings(deps),
                Salvation(deps),
                Slam(deps),
                Smite(deps),
                SoulReaper(deps),
                Taunt(deps),
                UmbralGrasp(deps),
            )
        for (spell in spells) {
            spell.setSpellManager(this)
            spell.loadConfigValues(plugin.dataFolder)
            spellRegistry[spell.name.lowercase()] = spell
        }
        logger.info("Registered ${spellRegistry.size} spells")
    }

    /** Displays active cooldowns in the action bar every half-second. */
    private fun startCooldownTask() {
        plugin.launch {
            while (true) {
                delay(500L)
                for (player in Bukkit.getOnlinePlayers()) {
                    val playerCooldowns = cooldownMap[player.uniqueId] ?: continue
                    val now = System.currentTimeMillis()
                    val activeCooldowns =
                        playerCooldowns.entries
                            .filter { (_, expiry) -> expiry > now }
                            .sortedBy { (_, expiry) -> expiry - now }
                    if (activeCooldowns.isEmpty()) continue

                    val bar = Component.text()
                    var first = true
                    for ((name, expiry) in activeCooldowns) {
                        val remaining = (expiry - now) / 1000.0
                        if (!first) bar.append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        bar.append(
                            Component.text(
                                "${spellRegistry[name]?.name ?: name}: %.1f".format(remaining),
                                NamedTextColor.YELLOW,
                            )
                        )
                        first = false
                    }
                    player.sendActionBar(bar.build())
                }
            }
        }
    }
}
