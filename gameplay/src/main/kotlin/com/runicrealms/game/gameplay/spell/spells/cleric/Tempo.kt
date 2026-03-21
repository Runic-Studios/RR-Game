package com.runicrealms.game.gameplay.spell.spells.cleric

import com.runicrealms.game.common.ClassType
import com.runicrealms.game.gameplay.spell.event.BasicAttackEvent
import com.runicrealms.game.gameplay.spell.event.SpellCastEvent
import com.runicrealms.game.gameplay.spell.spelltypes.ISpell
import com.runicrealms.game.gameplay.spell.spelltypes.Spell
import com.runicrealms.game.gameplay.spell.spelltypes.SpellDependencies
import com.runicrealms.game.gameplay.spell.spelltypes.SpellItemType
import com.runicrealms.game.gameplay.spell.spelltypes.components.DurationSpell
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent

class Tempo(deps: SpellDependencies) : Spell(SPELL_NAME, ClassType.CLERIC, deps) {
    var restore = BASE_RESTORE
    var radius = BASE_RADIUS
    var duration = BASE_DURATION

    override var cooldown = COOLDOWN
    override var manaCost = MANA_COST
    override var description =
        "Whenever you cast a spell, your next basic attack restores $BASE_RESTORE mana to yourself and allies within $BASE_RADIUS blocks."

    private val enhancedAttacks: MutableSet<UUID> = mutableSetOf()

    init {
        isPassive = true
        displayCastMessage = false
    }

    override fun executeSpell(player: Player, type: SpellItemType) {
        // Passive - handled through event listeners.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpellCast(event: SpellCastEvent) {
        val caster = event.caster
        if (!hasPassive(caster.uniqueId, name)) return
        if (event.spell.isPassive || !event.spell.displayCastMessage) return

        enhancedAttacks.add(caster.uniqueId)
        restoreMana(caster, restore)

        for (entity in caster.getNearbyEntities(radius, radius, radius)) {
            val ally = entity as? Player ?: continue
            if (!isValidAlly(caster, ally)) continue
            restoreMana(ally, restore)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBasicAttack(event: BasicAttackEvent) {
        val player = event.player
        if (!enhancedAttacks.remove(player.uniqueId)) return

        (spellManager.getSpell(Battlecry.SPELL_NAME) as? Influenced)?.increaseExtraDuration(
            player,
            duration,
        )
        (spellManager.getSpell(Accelerando.SPELL_NAME) as? Influenced)?.increaseExtraDuration(
            player,
            duration,
        )
        (spellManager.getSpell(GrandSymphony.SPELL_NAME) as? Influenced)?.increaseExtraDuration(
            player,
            duration,
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        enhancedAttacks.remove(uuid)
        extensions.remove(uuid)
    }

    private fun restoreMana(player: Player, amount: Int) {
        val mana = spellManager.getMana(player.uniqueId)
        spellManager.setMana(player.uniqueId, mana + amount)
    }

    // Tempo's fields are not component-based, so we load them all manually.
    override fun loadSpellSpecificData(config: FileConfiguration) {
        super.loadSpellSpecificData(config)
        restore = loadInt(config, "restore", restore)
        radius = loadDouble(config, "radius", radius)
        duration = loadDouble(config, "duration", duration)
    }

    companion object {
        const val SPELL_NAME = "Tempo"
        private const val COOLDOWN = 0.0
        private const val MANA_COST = 0
        private const val BASE_RESTORE = 10
        private const val BASE_RADIUS = 8.0
        private const val BASE_DURATION = 1.0

        val extensions: ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>> =
            ConcurrentHashMap()
    }

    interface Influenced : ISpell, DurationSpell {
        fun getExtraSeconds(player: Player): Double {
            if (!hasPassive(player.uniqueId, SPELL_NAME)) return 0.0
            return extensions[player.uniqueId]?.get(name) ?: 0.0
        }

        fun effectiveDuration(player: Player): Double = duration + getExtraSeconds(player)

        fun increaseExtraDuration(player: Player, seconds: Double) {
            if (!hasPassive(player.uniqueId, SPELL_NAME)) return

            val spellExtensions =
                extensions.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
            val current = spellExtensions.getOrDefault(name, 0.0)
            val capped = (current + seconds).coerceIn(0.0, maxExtraDuration())
            spellExtensions[name] = capped
        }

        fun removeExtraDuration(player: Player) {
            if (!hasPassive(player.uniqueId, SPELL_NAME)) return
            val spellExtensions = extensions[player.uniqueId] ?: return
            spellExtensions.remove(name)
            if (spellExtensions.isEmpty()) {
                extensions.remove(player.uniqueId)
            }
        }

        fun maxExtraDuration(): Double = Double.MAX_VALUE
    }
}
