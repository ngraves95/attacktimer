package com.attacktimer;

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

import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.VariableSpeed.State.TickCount;
import java.util.ArrayDeque;
import net.runelite.api.Skill;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.StatChanged;

public class Damage
{
    private static final double MODIFIER = 1; // this is where NPC specific modifiers go
    private static final double GLOBAL_MODIFIER = 1; // this is where global specific modifiers go i.e. leagues

    private ArrayDeque<Integer> hpExpEarned = new ArrayDeque<Integer>();
    private ArrayDeque<Integer> hpExpEarnedTickCount = new ArrayDeque<Integer>();

    public boolean onXpDrop(StatChanged event, TickCount tc)
    {
        final var skill = event.getSkill();
        if (skill != Skill.HITPOINTS)
        {
            return false;
        }
        hpExpEarnedTickCount.addLast(tc.get());
        hpExpEarned.addLast(event.getXp());
        return true;
    }
    public boolean onXpDrop(FakeXpDrop event, TickCount tc)
    {
        final var skill = event.getSkill();
        if (skill != Skill.HITPOINTS)
        {
            return false;
        }
        hpExpEarnedTickCount.addLast(tc.get());
        hpExpEarned.addLast(event.getXp());
        return true;
    }

    public int compute(TickCount tc)
    {
        if (hpExpEarnedTickCount.isEmpty())
        {
            return -1;
        }
        final var lastTc = hpExpEarnedTickCount.getLast();
        if (!tc.isWithinNTicks(lastTc, 1))
        {
            // In this case the last exp tick wasn't this tick in which case we hit a 0.
            return 0;
        }
        // https://oldschool.runescape.wiki/w/Combat#Experience_gain
        final var xp = (double) Utils.getLastDelta(hpExpEarned);
        return (int) Math.round(xp * (3.0d / 4.0d) * MODIFIER * GLOBAL_MODIFIER);
    }

    public void expire()
    {
        if (hpExpEarnedTickCount.size() > 5)
        {
            hpExpEarnedTickCount.removeFirst();
        }
        if (hpExpEarned.size() > 5)
        {
            hpExpEarned.removeFirst();
        }
    }
}
