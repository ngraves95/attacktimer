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
import com.attacktimer.Spellbook;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

/**
 * RoyalTitans: https://oldschool.runescape.wiki/w/Royal_Titans/Strategies
 *
 * For each elemental killed, the player receives a 1-tick reduction to their attack delay, allowing spells to
 * be cast consecutively much quicker than usual.
 */
public class RoyalTitans implements IVariableSpeed
{
    private static final int ROYAL_TITANS_REGION_ID = 11669;

    private static final int HP_FUDGE = 1;
    private static final int ELEMENTAL_HP = 40 - HP_FUDGE;

    private Set<NPC> iceElementals = new HashSet<NPC>();
    private Set<NPC> fireElementals = new HashSet<NPC>();

    private boolean removeDead = false;

    private static final Set<AnimationData> ONE_SHOT_SPELLS = new ImmutableSet.Builder<AnimationData>()
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
        if (spellbook != Spellbook.STANDARD
            || !isElemental(targetId)
            || !ONE_SHOT_SPELLS.contains(curAnimation))
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

        // Note that the twinflame second spell does not give Magic or Hitpoints experience and therefore will
        // not be computed properly by the caller.
        final boolean wieldingTwinflame = Utils.getWeaponId(client) == ItemID.TWINFLAME_STAFF;
        final int computedDamage = wieldingTwinflame ? damageDealt + ((damageDealt * 4) / 10) : damageDealt;

        // Awkwardly you only got awarded the exp (and therefore computed damage) against the one of the
        // elementals, hence the 3x3 AoE isn't seen in the damage dealt.
        if (computedDamage < ELEMENTAL_HP)
        {
            // Don't bother computing partial damage assume most players are one-shotting
            return curSpeed;
        }
        // Compute the number of elementals in the a 3x3 from our target:
        final var set = targetId == NpcID.RT_SUMMON_ELEMENTAL_FIRE ? fireElementals : iceElementals;
        int count = 1;
        final var reference = target.getWorldLocation();
        for (final NPC elemental : set)
        {
            if (elemental == target)
            {
                continue;
            }
            final WorldPoint worldLocation = elemental.getWorldLocation();
            final int distanceTo2D = reference.distanceTo2D(worldLocation);
            // 3x3 is 1 distance https://en.wikipedia.org/wiki/Chebyshev_distance
            if (distanceTo2D <= 1)
            {
                count++;
            }
        }
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

        // NOTE: this has one edge case also not covered (as well as the same eating one as purging staff)
        // which is that if the awarded bonus is granted and we already started our next weapon cooldown then
        // the bonus is applied to that attack instead.

        final boolean isTwinflameKillOnSecondProjectile = wieldingTwinflame &&
                                    curAnimation == AnimationData.MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF;
        final int extraHitOffset = isTwinflameKillOnSecondProjectile ? 1 : 0; // add an extra tick of delay for the second projectile.

        // https://oldschool.runescape.wiki/w/Hit_delay#Magic However it's not quite the formula, unclear to
        // me whether the wiki is wrong or if its simply a case of fence post errors counting the tiles.
        // https://en.wikipedia.org/wiki/Off-by-one_error#Fencepost_error
        //
        // from my testing the hit delay was 1 less on the transitions in the table therefore:
        //
        // Distance   Hit delay
        // (tiles)    (wiki)     (measured)
        //  1         1          1
        //  2         2          1 (diff)
        //  3         2          2
        //  4         2          2
        //  5         3          2 (diff)
        //  6         3          3
        //  7         3          3
        //  8         4          3 (diff)
        //  9         4          4
        //  10        4          4
        final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        final int distance = playerLoc.distanceTo2D(reference);
        final int hitDelay = 2 + (distance / 3) + extraHitOffset;

        // Remaining cooldown when the projectile actually impacts:
        final int remainingAtImpact = Math.max(0, curSpeed - hitDelay);

        // Apply reduction to remaining cooldown:
        final int remainingAfterReduction = Math.max(0, remainingAtImpact - count);

        // Total ticks waited = travel time + remaining cooldown after reduction
        final int finalSpeed = Math.min(curSpeed, hitDelay + remainingAfterReduction);

        // despawn happens much later than is dead (hence how Entity Hider works) so we need to remove them
        // now if we succeeded in apply. Deferred till the next onGameTick is called.
        removeDead = true;
        return finalSpeed;
    }

    private static boolean earlyExit(final Client client, final AttackProcedure atkType, final int damageDealt,
            final Spellbook spellbook)
    {
        return notInRegion(client)
               || atkType != AttackProcedure.MANUAL_AUTO_CAST
               || damageDealt <= 0
               || spellbook != Spellbook.STANDARD;
    }

    @Override
    public void onGameTick(Client client, GameTick tick)
    {
        // if we killed some elementals last tick remove them now.
        if (removeDead)
        {
            // don't clear the set(s) do it using the runelite API.
            var removed = iceElementals.removeIf(npc -> npc.isDead());
            removed |= fireElementals.removeIf(npc -> npc.isDead());
            if (removed)
            {
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
        if (id == NpcID.RT_SUMMON_ELEMENTAL_ICE)
        {
            iceElementals.add(npc);
        }
        else if (id == NpcID.RT_SUMMON_ELEMENTAL_FIRE)
        {
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
        if (id == NpcID.RT_SUMMON_ELEMENTAL_ICE)
        {
            iceElementals.remove(npc);
        }
        else if (id == NpcID.RT_SUMMON_ELEMENTAL_FIRE)
        {
            fireElementals.remove(npc);
        }
    }

    private static boolean isElemental(int id)
    {
        return id == NpcID.RT_SUMMON_ELEMENTAL_FIRE || id == NpcID.RT_SUMMON_ELEMENTAL_ICE;
    }

    private static boolean notInRegion(final Client client)
    {
        return Utils.getLocalLocation(client).getRegionID() != ROYAL_TITANS_REGION_ID;
    }
}
