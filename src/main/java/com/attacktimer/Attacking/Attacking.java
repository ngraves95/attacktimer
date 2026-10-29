package com.attacktimer.Attacking;

/*
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
 * Copyright (c) 2024-2026, Lexer747 <https://github.com/Lexer747>
 * Copyright (c) 2024-2026, Richardant <https://github.com/Richardant>
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
import com.attacktimer.AnimationData.CombatSkill;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.game.NPCManager;

public class Attacking
{
    // These animations are the ones which exceed the duration of their attack cooldown
    // so in this case DO NOT fall back the animation as it is un-reliable.
    private static final Set<AnimationData> UNRELIABLE_ANIMATIONS = new ImmutableSet.Builder<AnimationData>()
            .add(AnimationData.RANGED_BLOWPIPE).add(AnimationData.RANGED_BLAZING_BLOWPIPE)
            .add(AnimationData.MAGIC_EYE_OF_AYAK).add(AnimationData.MAGIC_EYE_OF_AYAK_SPEC).build();

    // Attacks which can happen without the player interacting with a target. The Obligator thrust can hit
    // several targets, and while using the Obligator (thrust or smash) the player is often not interacting with
    // any of them. Both animations last 3 ticks, never longer than their cooldown (5t thrust, 3t smash), so they
    // can't be mistaken for the end of the previous attack.
    private static final Set<AnimationData> NO_TARGET_ATTACKS = new ImmutableSet.Builder<AnimationData>()
            .add(AnimationData.MELEE_OBLIGATOR).add(AnimationData.MELEE_OBLIGATOR_FEMALE)
            .add(AnimationData.MELEE_OBLIGATOR_MULTIHIT).add(AnimationData.MELEE_OBLIGATOR_MULTIHIT_FEMALE).build();

    // Combat Dummy
    public static final Set<Integer> COMBAT_DUMMY = new ImmutableSet.Builder<Integer>()
            .add(NpcID.POH_COMBAT_DUMMY_UPGRADED_UNDEADSLAYER_NPC, NpcID.POH_COMBAT_DUMMY_UPGRADED_ETHER_NPC,
                    NpcID.POH_COMBAT_DUMMY_UPGRADED_KQ_NPC, NpcID.POH_COMBAT_DUMMY_UPGRADED_KURASK_NPC,
                    NpcID.POH_COMBAT_DUMMY_UPGRADED_VAMPIRE_NPC, NpcID.POH_COMBAT_DUMMY_UPGRADED_VORKATH_NPC,
                    NpcID.POH_COMBAT_DUMMY_NPC, NpcID.POH_COMBAT_DUMMY_UNDEADSLAYER_NPC,
                    NpcID.POH_COMBAT_DUMMY_UPGRADED_NPC)
            .build();

    // Combat Dummy + Nightmare Pillars
    private static final Set<Integer> SPECIAL_NPCS = new ImmutableSet.Builder<Integer>()
            .add(NpcID.NIGHTMARE_TOTEM_1_READY, NpcID.NIGHTMARE_TOTEM_2_READY,
                    NpcID.NIGHTMARE_TOTEM_3_READY, NpcID.NIGHTMARE_TOTEM_4_READY)
            .addAll(COMBAT_DUMMY)
            .build();

    public static boolean isPlayerAttacking(final Client client, final NPCManager npcManager, final Map<GameObject, Tile> sailingCannons)
    {
        final Player localPlayer = client.getLocalPlayer();
        final int animationId = localPlayer.getAnimation();
        // Most weapons overwrite blockList animations, apart from the zorya tome, how odd.
        if (AnimationData.isBlockListAnimation(animationId) && !getZoryaAttack(localPlayer))
        {
            return false;
        }

        final AnimationData fromId = AnimationData.fromId(animationId);
        // Unlike normal interactions sailing combat from a facility does in fact not set `getInteracting`,
        // the usual techniques of inspecting for health and combat level therefore also don't apply because
        // the client doesn't really know which NPC we could be interacting with. This is also because the NPC
        // being attacked interacts with the boat itself (dmg).
        if (sailingAttack(client, sailingCannons, fromId))
        {
            return true;
        }

        // Not walking is either ANY player animation or the edge cases which don't trigger an animation,
        // e.g Salamander.
        final boolean notWalking = animationId != -1 || getSpotAnimAttack(localPlayer);

        // Testing if we are attacking by checking the target is more future proof to new weapons which don't
        // need custom code and the weapon stats are enough.
        final Actor target = localPlayer.getInteracting();
        if (target != null && (target instanceof NPC))
        {
            final NPC npc = (NPC) target;
            final boolean containsAttackOption = Arrays.stream(npc.getComposition().getActions())
                    .anyMatch("Attack"::equals);
            final Integer health = npcManager.getHealth(npc.getId());
            final boolean hasHealthAndLevel = health != null && health > 0 && target.getCombatLevel() > 0;
            final boolean attackingNPC = hasHealthAndLevel || SPECIAL_NPCS.contains(npc.getId())
                    || containsAttackOption;
            // just having a target is not enough the player may be out of range, we must wait for any
            // animation which isn't running/walking/etc
            return attackingNPC && notWalking;
        }
        if (target != null && (target instanceof Player))
        {
            return notWalking;
        }
        if (target == null)
        {
            // Not attacking anything, unless it's an attack which doesn't need a target
            return NO_TARGET_ATTACKS.contains(fromId);
        }

        // Do not use any animations from this set
        if (UNRELIABLE_ANIMATIONS.contains(fromId))
        {
            return false;
        }
        // fall back to animations.
        return fromId != null;
    }

    public static Attack PlayerAttack(final Client client)
    {
        final Player localPlayer = client.getLocalPlayer();
        final int animationId = localPlayer.getAnimation();
        final Actor target = localPlayer.getInteracting();
        return new Attack(animationId, target);
    }

    private static boolean getSpotAnimAttack(final Player lp)
    {
        return getSalamanderAttack(lp) || getZoryaAttack(lp);
    }

    private static boolean getSalamanderAttack(final Player lp)
    {
        return lp.hasSpotAnim(SpotanimID.FIREBREATH);
    }

    private static boolean getZoryaAttack(final Player lp)
    {
        return lp.hasSpotAnim(SpotanimID.VFX_ZORYA_TOME_STANDARD_SPOTANIM)
                || lp.hasSpotAnim(SpotanimID.VFX_ZORYA_TOME_STANDARD_SPOTANIM_FEMALE)
                || getZoryaSpecAttack(lp);
    }

    public static boolean getZoryaSpecAttack(final Player lp)
    {
        return lp.hasSpotAnim(SpotanimID.VFX_ZORYA_TOME_SPEC_SPOTANIM)
                || lp.hasSpotAnim(SpotanimID.VFX_ZORYA_TOME_SPEC_SPOTANIM_FEMALE);
    }


    private static boolean sailingAttack(final Client client, final Map<GameObject, Tile> sailingCannons, final AnimationData anim)
    {
        // Normal combat (e.g. shooting a bow) from a boat does follow all the normal rules and therefore
        // should not be handled by this function.

        // The naive approach gets pretty close (is the animation `CombatSkill.SAILING`), the player is only
        // in sailing animations when the player is attacking. However there's one on main edge case, the
        // final shot of the cannon the player will remain the in animation even past the point where the
        // attacking has stopped.
        final var sailingAnimation = anim != null && anim.combatSkill == CombatSkill.SAILING;
        if (!sailingAnimation)
        {
            // don't check cannon stuff, early return
            return false;
        }

        // To improve the naive approach we inspect the game objects (tracked by the plugin and passed in the
        // map), the actual cannon blast effect has a specific animation versus the idle animation so we can
        // use that. This animation can only occur on the exact first tick the player starts an attack.

        final var playerLoc = client.getLocalPlayer().getWorldLocation();
        for (final var entry : sailingCannons.entrySet())
        {
            // If the cannon isn't *exactly* adjacent to the player then it is be a crewmate manning the
            // cannon in which case the animation isn't useful.
            if (!isAdjacent(playerLoc, entry.getValue().getWorldLocation()))
            {
                continue;
            }

            final var cannon = entry.getKey();
            // We only added DynamicObject's to the map so this cast is safe.
            final var animation = ((DynamicObject) cannon.getRenderable()).getAnimation().getId();
            if (animation == AnimationID.SAILING_CANNON01_SMALL01_ATTACK01)
            {
                // shared animation by all cannons so the player must be attacking with it (i.e. rune/bronze
                // cannons are indistinguishable)
                return true;
            }
        }

        // We exhausted all the cannons the plugin knows about and couldn't find the animation so the player
        // cannot be attacking with it.
        return false;
    }

    private static boolean isAdjacent(final WorldPoint a , final WorldPoint b )
    {
        return a.distanceTo(b) == 1;
    }
}
