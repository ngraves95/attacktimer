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
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;

public interface IVariableSpeed
{
    /**
     * apply is the general method the attack timer plugin will call with all the data about an attack
     * currently being triggered. apply is only called when the plugin is sure the player just started
     * attacking and hence a new cooldown.
     *
     * to ensure a new IVariableSpeed implementation is used ensure to add a new instance of that class in
     * {@see VariableSpeed.toApply}.
     * @param client the RuneScape client.
     * @param curAnimation the animation currently being used to attack.
     * @param atkType the overarching "attack type" for this attack, this is based on all the inference made
     * about manual casts, etc. For more details about the attack {@see com.attacktimer.ClientUtils.Utils}.
     * @param baseSpeed the speed at which the attack speed started before any other variable speeds have
     * changed it.
     * @param curSpeed the current speed at which the attack is now after variable speeds have been applied,
     * e.g. rapid with range style.
     * @return the new attack speed if the pre-conditions for this variable attack speed where met.
     */
    public int apply(Client client, AnimationData curAnimation, AttackProcedure atkType, int baseSpeed, int curSpeed);
    /**
     * onGameTick is pseudo subscription method, a variable speed implementation can implement this if the
     * condition for the variable speed requires some larger state tracking and cannot be implemented in apply
     * alone.
     * @param client the RuneScape client.
     * @param tick the current tick.
     */
    public void onGameTick(Client client, GameTick tick);
}