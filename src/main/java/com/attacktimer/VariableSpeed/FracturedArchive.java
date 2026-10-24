package com.attacktimer.VariableSpeed;

/*
 * Copyright (c) 2026, Richardant <https://github.com/Richardant>
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
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;

// Weapons from The Fractured Archive.
public class FracturedArchive implements IVariableSpeed
{
    private static final int ZORYA_FAST_ATTACKS = 3;
    private int zoryaFastAttacksRemaining = 0;

    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkType,
            final Spellbook spellbook, final int damageDealt, final int lastSpecDelta, final int baseSpeed,
            final int curSpeed)
    {
        if (curAnimation == null)
        {
            return curSpeed;
        }
        final int weaponId = Utils.getWeaponId(client);
        switch (curAnimation)
        {
            case MELEE_OBLIGATOR:
            case MELEE_OBLIGATOR_FEMALE:
                return isObligator(weaponId) ? 3 : curSpeed;
            case MELEE_OBLIGATOR_MULTIHIT:
            case MELEE_OBLIGATOR_MULTIHIT_FEMALE:
                return isObligator(weaponId) ? 5 : curSpeed;
            case RANGED_ASCENSION_CROSSBOW_LEFT:
            case RANGED_ASCENSION_CROSSBOW_LEFT_PVN:
            case RANGED_ASCENSION_CROSSBOW_RIGHT:
            case RANGED_ASCENSION_CROSSBOW_RIGHT_PVN:
                return weaponId == ItemID.ASCENSION_CROSSBOWS ? 2 : curSpeed;
            case MAGIC_ZORYA_TOME_SPEC:
            case MAGIC_ZORYA_TOME_SPEC_FEMALE:
                return isZoryasTome(weaponId) ? zoryaSpecAttackSpeed(damageDealt) : curSpeed;
            case MAGIC_ZORYA_TOME:
            case MAGIC_ZORYA_TOME_FEMALE:
                return isZoryasTome(weaponId) ? zoryaAttackSpeed() : curSpeed;
            default:
                return curSpeed;
        }
    }

    private static boolean isObligator(final int weaponId)
    {
        return weaponId == ItemID.OBLIGATOR || weaponId == ItemID.OBLIGATOR_UNCHARGED;
    }

    private static boolean isZoryasTome(final int weaponId)
    {
        return weaponId == ItemID.ZORYAS_TOME || weaponId == ItemID.ZORYAS_TOME_UNCHARGED;
    }

    private int zoryaSpecAttackSpeed(final int damageDealt)
    {
        // https://oldschool.runescape.wiki/w/Zorya%27s_tome
        // A landed spec buffs the next 3 attacks (2t instead of 3t). A spec used while buffed is itself a
        // buffed attack, so two landed specs in a row leave 3 - 1 + 3 = 5 buffed attacks.
        final int speed = zoryaAttackSpeed();
        if (damageDealt > 0)
        {
            zoryaFastAttacksRemaining += ZORYA_FAST_ATTACKS;
        }
        return speed;
    }

    private int zoryaAttackSpeed()
    {
        if (zoryaFastAttacksRemaining > 0)
        {
            zoryaFastAttacksRemaining--;
            return 2;
        }
        return 3;
    }
}
