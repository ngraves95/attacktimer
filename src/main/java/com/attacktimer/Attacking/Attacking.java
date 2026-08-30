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
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
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

    // Combat Dummy + Nightmare Pillars
    private static final Set<Integer> SPECIAL_NPCS = new ImmutableSet.Builder<Integer>()
            .add(NpcID.POH_COMBAT_DUMMY_UPGRADED_UNDEADSLAYER_NPC, NpcID.POH_COMBAT_DUMMY_UPGRADED_ETHER_NPC,
                    NpcID.POH_COMBAT_DUMMY_UPGRADED_KQ_NPC, NpcID.POH_COMBAT_DUMMY_UPGRADED_KURASK_NPC,
                    NpcID.POH_COMBAT_DUMMY_UPGRADED_VAMPIRE_NPC, NpcID.POH_COMBAT_DUMMY_UPGRADED_VORKATH_NPC,
                    NpcID.POH_COMBAT_DUMMY_NPC, NpcID.POH_COMBAT_DUMMY_UNDEADSLAYER_NPC,
                    NpcID.POH_COMBAT_DUMMY_UPGRADED_NPC, NpcID.NIGHTMARE_TOTEM_1_READY, NpcID.NIGHTMARE_TOTEM_2_READY,
                    NpcID.NIGHTMARE_TOTEM_3_READY, NpcID.NIGHTMARE_TOTEM_4_READY)
            .build();

    public static boolean isPlayerAttacking(final Client client, final NPCManager npcManager)
    {
        final Player localPlayer = client.getLocalPlayer();
        final int animationId = localPlayer.getAnimation();
        if (AnimationData.isBlockListAnimation(animationId))
        {
            return false;
        }

        // Not walking is either ANY player animation or the edge cases which don't trigger an animation,
        // e.g Salamander.
        final boolean notWalking = animationId != -1 || getSalamanderAttack(client);

        // Testing if we are attacking by checking the target is more future proof to new weapons which
        // don't
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
            // Not attacking anything
            return false;
        }

        // Do not use any animations from this set
        final AnimationData fromId = AnimationData.fromId(animationId);
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
        return new Attack(animationId, getSalamanderAttack(client), target);
    }

    private static boolean getSalamanderAttack(final Client client)
    {
        return client.getLocalPlayer().hasSpotAnim(SpotanimID.FIREBREATH);
    }
}
