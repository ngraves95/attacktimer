package com.attacktimer.VariableSpeed;

/*
 * Copyright (c) 2024, Lexer747 <https://github.com/Lexer747>
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
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;

/**
 * There is no cooldown when attacking the skulls with melee.
 */
public class TombsOfAmascut implements IVariableSpeed
{
    // The skulls during p3 wardens.
    // https://oldschool.runescape.wiki/w/Energy_Siphon
    private static final int ENERGY_SIPHON_ID = 11772;

    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkProcedure, final int baseSpeed, final int curSpeed)
    {
        final int targetId = Utils.getTargetId(client);
        final AttackType attkType = Utils.getAttackType(client);
        if (targetId == ENERGY_SIPHON_ID && attkType.IsMelee())
        {
            return 1;
        }
        return curSpeed;
    }
    public void onGameTick(Client client, GameTick tick) {}
}
