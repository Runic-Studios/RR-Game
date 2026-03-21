package com.runicrealms.game.gameplay.spell.spelltypes

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.effect.RunicStatusEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffect
import com.runicrealms.game.gameplay.spell.effect.SpellEffectType
import com.runicrealms.game.gameplay.spell.event.AllyVerifyEvent
import com.runicrealms.game.gameplay.spell.event.EnemyVerifyEvent
import com.runicrealms.game.gameplay.spell.spelltypes.components.AttributeSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DistanceSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.HealingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.MagicDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.PhysicalDamageSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.RadiusSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.ShieldingSpell
import com.runicrealms.game.gameplay.spell.spelltypes.components.WarmupSpell
import java.io.File
import java.util.Optional
import java.util.UUID
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.util.Vector
import org.slf4j.LoggerFactory

/**
 * Abstract base for all player spells. Subclasses declare their balance values as companion object
 * constants. Each spell registers itself as a Bukkit listener on construction.
 *
 * Balance values (cooldown, mana, damage, etc.) use hardcoded companion object constants with TODO
 * markers to replace with config loading once the config system is migrated.
 *
 * ARCHITECTURAL NOTE (see SPELL_MIGRATION.md #1): execute() currently receives only (Player,
 * SpellItemType). ClassType is looked up synchronously via UserDataRegistry using
 * withSyncCharacterData. This is a temporary solution until the signature ambiguity is resolved.
 *
 * SpellManager is injected at registration time via [setSpellManager] to avoid a circular
 * dependency (SpellManager -> Spell -> SpellManager). It is guaranteed to be set before any player
 * can cast a spell.
 */
abstract class Spell(
    override val name: String,
    override val reqClass: ClassType,
    protected val deps: SpellDependencies,
) : ISpell, Listener {

    private val logger = LoggerFactory.getLogger("gameplay")
    private lateinit var spellManagerRef: SpellManagerBridge

    // Mutable so loadConfigValues() can overwrite them from YAML.
    // Subclasses initialize via `override var cooldown = COOLDOWN` (companion object constant).
    override var cooldown: Double = 0.0
    override var manaCost: Int = 0

    override var description: String = ""
    var isPassive: Boolean = false
    var displayCastMessage: Boolean = true

    init {
        Bukkit.getPluginManager().registerEvents(this, deps.plugin)
    }

    /** Called by [SpellRegistry] after construction so the spell can call back into the manager. */
    internal fun setSpellManager(manager: SpellManagerBridge) {
        spellManagerRef = manager
    }

    /** Exposes the manager bridge to subclasses for advanced operations (e.g. reduceCooldown). */
    protected val spellManager: SpellManagerBridge
        get() = spellManagerRef

    // --- Core execute pipeline ---

    override fun execute(player: Player, type: SpellItemType): Boolean {
        if (isOnCooldown(player)) return false

        // Look up player class synchronously. This blocks briefly on the data lock.
        // TODO (SPELL_MIGRATION.md #1): Replace with GameCharacter parameter once decided.
        val playerClass = getPlayerClassType(player.uniqueId)
        val canCast = reqClass == ClassType.ANY || reqClass == playerClass
        if (!canCast) {
            player.playSound(player.location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f)
            player.sendActionBar(
                Component.text("Your class cannot cast this spell!", NamedTextColor.RED)
            )
            return false
        }

        if (!attemptToExecute(player)) return false

        // Deduct mana
        val currentMana = spellManagerRef.getMana(player.uniqueId)
        if (currentMana < manaCost) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 1.0f)
            player.sendActionBar(Component.text("Not enough mana!", NamedTextColor.RED))
            return false
        }
        spellManagerRef.setMana(player.uniqueId, currentMana - manaCost)

        if (displayCastMessage) {
            player.sendActionBar(
                Component.text()
                    .append(Component.text("You cast ", NamedTextColor.GREEN))
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(Component.text("!", NamedTextColor.GREEN))
                    .build()
            )
        }

        spellManagerRef.addCooldown(player, this, cooldown)
        executeSpell(player, type)
        return true
    }

    /**
     * Override to add additional pre-cast conditions (e.g. must be on ground, must be holding bow).
     */
    open fun attemptToExecute(player: Player): Boolean = true

    /** Override to implement the spell's actual gameplay logic. */
    open fun executeSpell(player: Player, type: SpellItemType) {}

    // --- ISpell delegation helpers ---

    override fun hasSpellEffect(uuid: UUID, identifier: SpellEffectType): Boolean =
        deps.spellEffectAPI.hasSpellEffect(uuid, identifier)

    override fun getSpellEffect(
        casterUuid: UUID,
        recipientUuid: UUID,
        identifier: SpellEffectType,
    ): Optional<SpellEffect> =
        deps.spellEffectAPI.getSpellEffect(casterUuid, recipientUuid, identifier)

    override fun getSpellEffects(
        recipientId: UUID,
        identifier: SpellEffectType,
    ): List<SpellEffect> = deps.spellEffectAPI.getSpellEffects(recipientId, identifier)

    override fun determineHighestStacks(recipientId: UUID, identifier: SpellEffectType): Int =
        deps.spellEffectAPI.determineHighestStacks(recipientId, identifier)

    override fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
        applier: LivingEntity?,
    ) =
        deps.statusEffectAPI.addStatusEffect(
            entity,
            effect,
            durationInSeconds,
            displayMessage,
            applier,
        )

    override fun addStatusEffect(
        entity: LivingEntity,
        effect: RunicStatusEffect,
        durationInSeconds: Double,
        displayMessage: Boolean,
    ) = deps.statusEffectAPI.addStatusEffect(entity, effect, durationInSeconds, displayMessage)

    override fun hasPassive(uuid: UUID, passive: String): Boolean =
        deps.skillTreeAPI.hasPassiveFromSkillTree(uuid, passive)

    override fun hasStatusEffect(uuid: UUID, effect: RunicStatusEffect): Boolean =
        deps.statusEffectAPI.hasStatusEffect(uuid, effect)

    override fun healPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell?) =
        spellManagerRef.healPlayer(caster, recipient, amount, spell)

    override fun isOnCooldown(player: Player): Boolean = spellManagerRef.isOnCooldown(player, name)

    override fun isValidAlly(caster: Player, recipient: Entity): Boolean {
        val event = AllyVerifyEvent(caster, recipient)
        Bukkit.getPluginManager().callEvent(event)
        return !event.isCancelled
    }

    override fun isValidEnemy(caster: Player, victim: Entity): Boolean {
        val event = EnemyVerifyEvent(caster, victim)
        Bukkit.getPluginManager().callEvent(event)
        return !event.isCancelled
    }

    override fun percentMaxHealth(entity: LivingEntity, percent: Double): Int {
        val max = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        return (max * percent).toInt()
    }

    override fun percentMissingHealth(entity: LivingEntity, percent: Double, cap: Int): Int {
        val max = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        val missing = max - entity.health
        return minOf(cap, (missing * percent).toInt())
    }

    override fun removeStatusEffect(entity: Entity, effect: RunicStatusEffect): Boolean =
        deps.statusEffectAPI.removeStatusEffect(entity.uniqueId, effect)

    override fun shieldPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell?) =
        spellManagerRef.shieldPlayer(caster, recipient, amount, spell)

    // --- Internal helpers ---

    private fun getPlayerClassType(uuid: UUID): ClassType {
        val character = deps.userDataRegistry.getCharacter(uuid) ?: return ClassType.ANY
        return character.withSyncCharacterData { traits.classType }
    }

    // --- Config loading ---

    /**
     * Reads this spell's YAML config file from [dataFolder]/spells/{ClassName}.yml and applies the
     * values to this spell's fields. If the file is missing, logs an error and keeps the current
     * values (companion object constants serve as defaults).
     *
     * Called by [com.runicrealms.game.gameplay.spell.SpellManager] after construction and
     * [setSpellManager], so [spellManager] is available if needed.
     */
    internal fun loadConfigValues(dataFolder: File) {
        val file = dataFolder.resolve("spells/${javaClass.simpleName}.yml")
        if (!file.exists()) {
            logger.error("Missing spell data file for $name")
            return
        }
        val config = YamlConfiguration.loadConfiguration(file)
        cooldown = loadDouble(config, "cooldown", cooldown)
        manaCost = loadInt(config, "mana", manaCost)
        loadSpellSpecificData(config)
    }

    /**
     * Loads component fields and any spell-specific custom keys from the YAML config. Spells with
     * custom keys override this, call super, then load their own fields using [loadDouble],
     * [loadInt], or [loadString].
     */
    protected open fun loadSpellSpecificData(config: FileConfiguration) {
        if (this is AttributeSpell) {
            attribute = loadString(config, "attribute", attribute)
            attributeBaseValue = loadDouble(config, "attribute-base-value", attributeBaseValue)
            attributeMultiplier = loadDouble(config, "attribute-multiplier", attributeMultiplier)
        }
        if (this is DistanceSpell) distance = loadDouble(config, "distance", distance)
        if (this is DurationSpell) duration = loadDouble(config, "duration", duration)
        if (this is HealingSpell) {
            healAmount = loadDouble(config, "heal", healAmount)
            healPerLevel = loadDouble(config, "heal-per-level", healPerLevel)
        }
        if (this is MagicDamageSpell) {
            magicDamage = loadDouble(config, "magic-damage", magicDamage)
            magicDamagePerLevel = loadDouble(config, "magic-damage-per-level", magicDamagePerLevel)
        }
        if (this is PhysicalDamageSpell) {
            physicalDamage = loadDouble(config, "physical-damage", physicalDamage)
            physicalDamagePerLevel =
                loadDouble(config, "physical-damage-per-level", physicalDamagePerLevel)
        }
        if (this is RadiusSpell) radius = loadDouble(config, "radius", radius)
        if (this is ShieldingSpell) {
            shieldAmount = loadDouble(config, "shield", shieldAmount)
            shieldPerLevel = loadDouble(config, "shield-per-level", shieldPerLevel)
        }
        if (this is WarmupSpell) warmupSeconds = loadDouble(config, "warmup", warmupSeconds)
    }

    // --- Config load helpers ---

    /** Returns the Double value for [key], or [default] with a warning if the key is absent. */
    protected fun loadDouble(config: FileConfiguration, key: String, default: Double): Double =
        if (config.contains(key)) config.getDouble(key)
        else {
            logger.warn("[$name] Config key '$key' not found, using default: $default")
            default
        }

    /** Returns the Int value for [key], or [default] with a warning if the key is absent. */
    protected fun loadInt(config: FileConfiguration, key: String, default: Int): Int =
        if (config.contains(key)) config.getInt(key)
        else {
            logger.warn("[$name] Config key '$key' not found, using default: $default")
            default
        }

    /** Returns the String value for [key], or [default] with a warning if the key is absent. */
    protected fun loadString(config: FileConfiguration, key: String, default: String): String =
        if (config.contains(key)) config.getString(key) ?: default
        else {
            logger.warn("[$name] Config key '$key' not found, using default: $default")
            default
        }

    /**
     * Rotates a vector around the Y axis by [degrees]. Kept as a protected helper matching the old
     * Spell.rotateVectorAroundY().
     */
    protected fun rotateVectorAroundY(vector: Vector, degrees: Double): Vector {
        val clone = vector.clone()
        val rad = Math.toRadians(degrees)
        val cos = Math.cos(rad)
        val sin = Math.sin(rad)
        val x = vector.x
        val z = vector.z
        clone.x = cos * x - sin * z
        clone.z = sin * x + cos * z
        return clone
    }
}

/**
 * Minimal bridge interface that [Spell] uses to call back into [SpellManager]. Defined separately
 * to break the circular dependency Spell <-> SpellManager.
 */
interface SpellManagerBridge {
    fun getMana(uuid: UUID): Int

    fun setMana(uuid: UUID, amount: Int)

    fun addCooldown(player: Player, spell: Spell, cooldownSeconds: Double)

    fun isOnCooldown(player: Player, spellName: String): Boolean

    fun reduceCooldown(player: Player, spellName: String, reductionSeconds: Double)

    fun healPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell?)

    fun shieldPlayer(caster: Player, recipient: Player, amount: Double, spell: Spell?)

    fun getSpell(name: String): Spell?

    fun isShielded(player: Player): Boolean
}
