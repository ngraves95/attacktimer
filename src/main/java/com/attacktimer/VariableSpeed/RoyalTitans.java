package com.attacktimer.VariableSpeed;

/*
 * Copyright (c) 2026, Lexer747 <https://github.com/Lexer747>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.attacktimer.AnimationData;
import com.attacktimer.AttackProcedure;
import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.VariableSpeed.State.IStateTracker;
import com.attacktimer.Spellbook;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

@Slf4j
public class RoyalTitans implements IVariableSpeed
{
    private static final int TWINFLAME_STAFF_WEAPON_ID = 30634;

    private static final int ROYAL_TITANS_REGION_ID = 11669;

    private static final int FIRE_ELEMENTAL_ID = 14150;
    private static final int ICE_ELEMENTAL_ID = 14151;

    private static final int HP_FUDGE = 1;
    private static final int ELEMENTAL_HP = 40 - HP_FUDGE;

    private Set<NPC> iceElementals = new HashSet<NPC>();
    private Set<NPC> fireElementals = new HashSet<NPC>();

    private boolean removeDead = false;

    private static final Set<AnimationData> STANDARD_SPELLS = new ImmutableSet.Builder<AnimationData>()
            .add(AnimationData.MAGIC_STANDARD_STRIKE_BOLT_BLAST)
            .add(AnimationData.MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF)
            .add(AnimationData.MAGIC_STANDARD_SURGE_STAFF)
            .add(AnimationData.MAGIC_STANDARD_WAVE)
            .add(AnimationData.MAGIC_STANDARD_WAVE_STAFF)
            .build();

    @Override
    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkType,
            final Spellbook spellbook, final int damageDealt, final int lastSpecDelta, final int baseSpeed,
            final int curSpeed)
    {
        if (earlyExit(client, atkType, damageDealt, spellbook))
        {
            return curSpeed;
        }
        final int targetId = Utils.getTargetId(client);
        if (spellbook != Spellbook.STANDARD ||
            !isElemental(targetId) ||
            !STANDARD_SPELLS.contains(curAnimation))
        {
            return curSpeed;
        }
        final NPC target = Utils.getTargetNPC(client);
        if (target == null)
        {
            return curSpeed;
        }
        // We are now in the royal titan region, attacking an elemental using magic and one of the standard
        // spells which could one shot it.
        log.debug("[RoyalTitans] Attacking elemental With correct spell");
        // Awkwardly you only got awarded the exp (and therefore computed damage) against the one of the
        // elementals, hence the 3x3 AoE isn't seen in the damage dealt.
        if (damageDealt < ELEMENTAL_HP)
        {
            log.debug("[RoyalTitans] didn't do enough damage");
            // Don't bother computing partial damage assume most players are one-shotting
            return curSpeed;
        }
        log.debug("[RoyalTitans] enough damage");
        // Compute the number of elementals in the a 3x3 from our target:
        final var set = targetId == FIRE_ELEMENTAL_ID ? fireElementals : iceElementals;
        log.debug("[RoyalTitans] elemental set: {}", set);
        int count = 1;
        final var reference = target.getWorldLocation();
        log.debug("[RoyalTitans] reference {}", reference);
        for (final NPC elemental : set)
        {
            if (elemental == target)
            {
                log.debug("[RoyalTitans] distance check skipped - is target");
                continue;
            }
            final WorldPoint worldLocation = elemental.getWorldLocation();
            final int distanceTo2D = reference.distanceTo2D(worldLocation);
            log.debug("[RoyalTitans] distance check new {}, distance {}", worldLocation, distanceTo2D);
            if (distanceTo2D <= 1)
            {
                count++;
            }
        }
        log.debug("[RoyalTitans] found AoE will kill: {}", count);
        // Now compute the travel delay, we are only awarded the improved tick delay when the projectile lands
        // (this can be pre-computed) so if we kill 3 elementals 10 tiles away we don't see the full 3 tick
        // improvement but in fact we see the 3 ticks awarded 4 ticks after we attacked. And because of
        // https://oldschool.runescape.wiki/w/Hit_delay#Processing_order_delay:
        //
        // > NPCs are processed earlier than players each tick, so this effect will make all hits on NPCs
        // > delayed by an additional one tick compared to the numbers listed in this article.
        //
        // This means the 3 tick reduction is awarded on tick 5, by which time we're already off-cool down
        // (assuming manual cast). And if we use the twin-flame (6 tick) we will only get a single tick of the
        // improvement we earned.
        //
        // Therefore this is generalised as:
        //
        // We calculate the tick on which the reductions take effect (hitDelay + 1). A reduction of N ticks
        // only benefits us if it lands **before** the natural attack timer expires.
        //
        // Effective ready tick = max(hitDelay + 1, curSpeed - elementals_killed)

        // NOTE: This is probably why the purging staff has that bug, because it only awards the 3 ticks of
        // reduction when the spell lands (which is always 2 ticks for dark demon bane).

        final boolean isTwinflame = Utils.getWeaponId(client) == TWINFLAME_STAFF_WEAPON_ID &&
                                    curAnimation == AnimationData.MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF;
        final int extraHitOffset = isTwinflame ? 1 : 0;

        final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        final int distance = playerLoc.distanceTo2D(reference);
        log.debug("[RoyalTitans] player {} - distance to target {}", playerLoc, distance);

        // https://oldschool.runescape.wiki/w/Hit_delay#Magic

        final int hitDelay = 2 + (distance / 3) + extraHitOffset;

        // Remaining cooldown when the projectile actually impacts:
        final int remainingAtImpact = Math.max(0, curSpeed - hitDelay);

        // Apply reduction to remaining cooldown:
        final int remainingAfterReduction = Math.max(0, remainingAtImpact - count);

        // Total ticks waited = travel time + remaining cooldown after reduction
        final int finalSpeed = Math.min(curSpeed, hitDelay + remainingAfterReduction);

        log.debug("[RoyalTitans] distance: {}, hitDelay: {}, remainingAtImpact: {}, remainingAfterReduction: {}, finalSpeed: {}",
                distance, hitDelay, remainingAtImpact, remainingAfterReduction, finalSpeed);


        // despawn happens much later than is dead (hence how Entity Hider works) so we need to remove them
        // now if we succeeded in apply. Deferred till the next onGameTick is called.
        removeDead = true;
        log.debug("[RoyalTitans] success, final cool down {}", finalSpeed);
        return finalSpeed;
    }

    private static boolean earlyExit(final Client client, final AttackProcedure atkType, final int damageDealt,
            final Spellbook spellbook)
    {
        return notInRegion(client) &&
               atkType != AttackProcedure.MANUAL_AUTO_CAST &&
               damageDealt <= 0 &&
               spellbook != Spellbook.STANDARD;
    }

    @Override
    public void onGameTick(Client client, GameTick tick)
    {
        if (removeDead)
        {
            var before = iceElementals.size() + fireElementals.size();
            var removed = iceElementals.removeIf(npc -> npc.isDead());
            removed |= fireElementals.removeIf(npc -> npc.isDead());
            var after = iceElementals.size() + fireElementals.size();
            if (removed)
            {
                log.debug("[RoyalTitans] removed dead elementals in onGameTick - before {}, after {}", before, after);
                removeDead = false;
            }
        }
    }

    @Override
    public void onNpcSpawned(final Client client, final NpcSpawned npcSpawned)
    {
        if (notInRegion(client))
        {
            return;
        }
        final NPC npc = npcSpawned.getNpc();
        final int id = npc.getId();
        if (id == ICE_ELEMENTAL_ID)
        {
            log.debug("[RoyalTitans] added ice elemental");
            iceElementals.add(npc);
        }
        else if (id == FIRE_ELEMENTAL_ID)
        {
            log.debug("[RoyalTitans] added fire elemental");
            fireElementals.add(npc);
        }
    }

    @Override
    public void onNpcDespawned(final Client client, final NpcDespawned npcDespawned)
    {
        if (notInRegion(client))
        {
            return;
        }
        final NPC npc = npcDespawned.getNpc();
        final int id = npc.getId();
        if (id == ICE_ELEMENTAL_ID)
        {
            log.debug("[RoyalTitans] removed ice elemental");
            iceElementals.remove(npc);
        }
        else if (id == FIRE_ELEMENTAL_ID)
        {
            log.debug("[RoyalTitans] removed fire elemental");
            fireElementals.remove(npc);
        }
    }

    private static boolean isElemental(int id)
    {
        return id == FIRE_ELEMENTAL_ID || id == ICE_ELEMENTAL_ID;
    }

    private static boolean notInRegion(final Client client)
    {
        return Utils.getLocalLocation(client).getRegionID() != ROYAL_TITANS_REGION_ID;
    }
}
