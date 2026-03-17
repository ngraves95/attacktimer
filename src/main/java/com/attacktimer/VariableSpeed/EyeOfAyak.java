package com.attacktimer.VariableSpeed;

/*
 * Copyright (c) 2025, Lexer747 <https://github.com/Lexer747>
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
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;

public class EyeOfAyak implements IVariableSpeed
{
    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkProcedure, final int baseSpeed, final int curSpeed)
    {
        // https://oldschool.runescape.wiki/w/Eye_of_ayak#Charged
        // https://oldschool.runescape.wiki/w/Eye_of_ayak#Special_attack
        if (curAnimation == AnimationData.MAGIC_EYE_OF_AYAK_SPEC)
        {
            // This is unclear if the Ayak spec "sets" the speed to 5 or adds two ticks. We can't know until
            // leagues. If it does set it too 5 this code is correct and it's order in
            // `src\main\java\com\attacktimer\VariableSpeed\VariableSpeed.java` is correct. However if it's
            // actually a +2 this code is wrong and it *might* need to come before the leagues modifier.
            return 5;
        }
        return curSpeed;
    }
    public void onGameTick(Client client, GameTick tick) {}
}
