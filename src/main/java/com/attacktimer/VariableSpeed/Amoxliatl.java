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
import com.attacktimer.AttackType;
import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.Spellbook;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

/**
 * Amoxliatl: https://oldschool.runescape.wiki/w/Unstable_ice
 *
 * The unstable ice blocks can only be destroyed with melee, and melee attacks against them will result in a
 * max hit with an attack speed of 1.
 */
public class Amoxliatl implements IVariableSpeed
{
    private static final int AMOXLIATL_REGION_ID = 5446;

    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkType,
            final Spellbook spellbook, final int damageDealt, final int lastSpecDelta, final int baseSpeed,
            final int curSpeed)
    {
        final WorldPoint location = Utils.getLocalLocation(client);
        final int targetId = Utils.getTargetId(client);
        final AttackType attkType = Utils.getAttackType(client);
        if (location.getRegionID() == AMOXLIATL_REGION_ID && targetId == NpcID.AMOXLIATL_ICE_BLOCK && attkType.IsMelee())
        {
            return 1;
        }
        return curSpeed;
    }
}
