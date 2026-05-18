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
import com.attacktimer.VariableSpeed.State.Yama;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;

public class PurgingStaffSpec implements IVariableSpeed
{
    private Yama yama;
    public NPC lastTarget;

    PurgingStaffSpec(Yama yama)
    {
        this.yama = yama;
    }

    // https://oldschool.runescape.wiki/w/Purging_staff#Special_attack
    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkType,
            final int damageDealt, final int lastSpecDelta, final int baseSpeed, final int curSpeed)
    {
        // For now the plugin only works for yama
        if (!this.yama.inYamaRegion)
        {
            return curSpeed;
        }
        var target = Utils.getTargetNPC(client);
        var flare = Yama.isEitherVoidFlare(target, lastTarget);
        lastTarget = target;
        if (flare == null)
        {
            return curSpeed;
        }
        if (yama == null)
        {
            return curSpeed;
        }

        yama.dealVoidFlareDamage(flare, damageDealt);
        if (lastSpecDelta != -250)
        {
            // not using the spec
            return curSpeed;
        }
        if (Utils.getWeaponId(client) != ItemID.PURGING_STAFF)
        {
            // not using a purging staff
            return curSpeed;
        }

        if (yama.getVoidFlareDead(flare))
        {
            // speed up!
            // according to the wiki this should be 3 ticks but is actually 2 ticks is normal circumstances
            return curSpeed - 2;
        }
        return curSpeed;
    }
}
