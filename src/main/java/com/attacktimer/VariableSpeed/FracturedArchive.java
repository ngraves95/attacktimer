package com.attacktimer.VariableSpeed;

/*
 * Copyright (c) 2026, Richardant <https://github.com/Richardant>
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
import com.attacktimer.Spellbook;
import com.attacktimer.VariableSpeed.State.TickCount;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;

// Weapons from The Fractured Archive.
//
// The Ascension crossbows are not handled here: their base speed (3t) plus RapidAttackStyle already gives the
// correct 3t on accurate/longrange and 2t on rapid.
public class FracturedArchive implements IVariableSpeed
{
    private static final int ZORYA_FAST_ATTACKS = 3;

    private final TickCount tickCount;
    // Buffed (2t) Zorya's tome attacks left after the current attack.
    private int zoryaFastAttacksRemaining = 0;
    // The speed can be computed more than once for the same attack (again when the xp drop arrives), so the
    // buff is always recomputed from the state it had before the first computation on that tick.
    private int zoryaAttackTick = -1;
    private int zoryaFastAttacksBeforeAttack = 0;

    FracturedArchive(final TickCount tc)
    {
        this.tickCount = tc;
    }

    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkType,
            final Spellbook spellbook, final int damageDealt, final int lastSpecDelta, final int baseSpeed,
            final int curSpeed)
    {
        if (curAnimation == null)
        {
            return curSpeed;
        }
        final int weaponId = Utils.getWeaponId(client);
        if (isObligator(weaponId))
        {
            switch (curAnimation)
            {
                case MELEE_OBLIGATOR:
                case MELEE_OBLIGATOR_FEMALE:
                    return 3;
                case MELEE_OBLIGATOR_MULTIHIT:
                case MELEE_OBLIGATOR_MULTIHIT_FEMALE:
                    return 5;
                default:
                    return curSpeed;
            }
        }
        else if (isZoryasTome(weaponId))
        {
            final boolean hit = successfulHit(client, damageDealt);
            switch (curAnimation)
            {
                case MAGIC_ZORYA_TOME_SPEC:
                case MAGIC_ZORYA_TOME_SPEC_FEMALE:
                    return zoryaAttackSpeed(hit);
                case MAGIC_ZORYA_TOME:
                case MAGIC_ZORYA_TOME_FEMALE:
                    return zoryaAttackSpeed(false);
                default:
                    return curSpeed;
            }
        }

        return curSpeed;
    }

    private static boolean isObligator(final int weaponId)
    {
        return weaponId == ItemID.OBLIGATOR || weaponId == ItemID.OBLIGATOR_UNCHARGED;
    }

    private static boolean isZoryasTome(final int weaponId)
    {
        return weaponId == ItemID.ZORYAS_TOME || weaponId == ItemID.ZORYAS_TOME_UNCHARGED;
    }

    private int zoryaAttackSpeed(final boolean isSpecAndHit)
    {
        // https://oldschool.runescape.wiki/w/Zorya%27s_tome
        // A landed spec buffs the next 3 attacks (2t instead of 3t). A spec used while buffed is itself a
        // buffed attack, so two landed specs in a row leave 3 - 1 + 3 = 5 buffed attacks.
        final int tick = tickCount.get();
        if (tick != zoryaAttackTick)
        {
            zoryaAttackTick = tick;
            zoryaFastAttacksBeforeAttack = zoryaFastAttacksRemaining;
        }
        int remaining = zoryaFastAttacksBeforeAttack;
        int speed = 3;
        if (remaining > 0)
        {
            remaining--;
            speed = 2;
        }
        if (isSpecAndHit)
        {
            remaining += ZORYA_FAST_ATTACKS;
        }
        zoryaFastAttacksRemaining = remaining;
        return speed;
    }

    private boolean successfulHit(final Client client, final int damageDealt)
    {
        if (damageDealt > 0)
        {
            return true;
        }
        final Actor target = client.getLocalPlayer().getInteracting();
        if (target != null && (target instanceof NPC))
        {
            final NPC npc = (NPC) target;
            // These don't compute damage normally (zero exp)
            return Attacking.COMBAT_DUMMY.contains(npc.getId());
        }
        return false;
    }
}
