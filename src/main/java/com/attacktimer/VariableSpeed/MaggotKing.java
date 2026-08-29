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
import com.attacktimer.Attacking.Attacking;
import com.attacktimer.ClientUtils.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

@Slf4j
public class MaggotKing
{
    private static final int MAGGOT_KING_REGION_ID = 11645;

    MaggotKing()
    {}

    // https://oldschool.runescape.wiki/w/Maggot_King/Strategies#Ur-maggot_larvae
    //
    // If the player attacks a maggot with a "standard bow" it doesn't matter what current cooldown is the
    // player is immediately set to the cooldown of the bow they used.
    //
    // Therefore this method returns `attackDelayHoldoffTicks` in all cases where this condition isn't met.
    // But if the condition is met this method returns a brand new number which is the attack speed of the bow
    // used. This number can be the same as the current delay and that's ok.
    public int onRender(final Client client, final ItemManager itemManager, final int attackDelayHoldoffTicks,
            final boolean debugLogs)
    {
        if (!Utils.isInRegionId(client, MAGGOT_KING_REGION_ID))
        {
            return attackDelayHoldoffTicks;
        }

        final var atk = Attacking.PlayerAttack(client);
        final AnimationData anim = AnimationData.fromId(atk.getAnimationId());
        if (anim == null || atk.getTarget() == null || !(atk.getTarget() instanceof NPC))
        {
            return attackDelayHoldoffTicks;
        }

        final NPC npc = (NPC) atk.getTarget();
        if (npc.getId() != NpcID.UR_MAGGOT_LARVAE)
        {
            return attackDelayHoldoffTicks;
        }

        if (!anim.isStandardBowAttack())
        {
            return attackDelayHoldoffTicks;
        }
        if (debugLogs)
        {
            log.debug("MaggotKing success, attacking maggot with bow");
        }

        final ItemStats weaponStats = Utils.getWeaponStats(client, itemManager, Utils.getWeaponId(client));
        final int aspeed = weaponStats.getEquipment().getAspeed();
        // We don't want a full variable speed here, we know apriori that none of them will apply (leagues
        // will but that's hard to test and changes every time it comes around)
        return VariableSpeed.RAPID_ATTACK_STYLE.apply(client, anim, AttackProcedure.MELEE_OR_RANGE, null, -1, -1,
                aspeed, aspeed);
    }
}
