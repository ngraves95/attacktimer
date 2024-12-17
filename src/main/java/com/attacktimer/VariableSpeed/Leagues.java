package com.attacktimer.VariableSpeed;

/*
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
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
import com.attacktimer.AttackStyle;
import com.attacktimer.ClientUtils.Utils;

import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameTick;

public class Leagues implements IVariableSpeed
{
    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkProcedure, final int baseSpeed, final int curSpeed)
    {
        int leagueRelicVarbit = 0;
        if (client.getWorldType().contains(WorldType.SEASONAL))
        {
            leagueRelicVarbit = client.getVarbitValue(Varbits.LEAGUE_RELIC_4);
        }

        AttackStyle attackStyle = Utils.getAttackStyle(client);
        int masteryLevel;
        switch (leagueRelicVarbit)
        {
            case 0:
                // No league relic active - player does not have t4 relic or is not in leagues.
                return baseSpeed;
            case 1:
                // Archer's Embrace (ranged).
                // Ranged Mastery 3 & 5.
                masteryLevel = client.getVarbitValue(Varbits.LEAGUES_RANGED_COMBAT_MASTERY_LEVEL);
                if (attackStyle == AttackStyle.RANGING || attackStyle == AttackStyle.LONGRANGE)
                {
                    return applyLeagueFormulaSpeed(baseSpeed, masteryLevel);
                }
                break;
            case 2:
                // Brawler's Resolve (melee)
                // Melee Mastery 3 & 5.
                masteryLevel = client.getVarbitValue(Varbits.LEAGUES_MELEE_COMBAT_MASTERY_LEVEL);
                if (attackStyle == AttackStyle.ACCURATE ||
                        attackStyle == AttackStyle.AGGRESSIVE ||
                        attackStyle == AttackStyle.CONTROLLED ||
                        attackStyle == AttackStyle.DEFENSIVE)
                {
                    return applyLeagueFormulaSpeed(baseSpeed, masteryLevel);
                }
                break;
            case 3:
                // Superior Sorcerer (magic)
                // Magic Mastery 3 & 5.
                masteryLevel = client.getVarbitValue(Varbits.LEAGUES_MAGIC_COMBAT_MASTERY_LEVEL);
                if (attackStyle == AttackStyle.CASTING || attackStyle == AttackStyle.DEFENSIVE_CASTING)
                {
                    return applyLeagueFormulaSpeed(baseSpeed, masteryLevel);
                }
                break;
        }

        return baseSpeed;
    }

    private int applyLeagueFormulaSpeed(int baseSpeed, int masteryLevel)
    {
        // Older league's had no masteries and were all: "attack rate set to 50%, rounded down for 5t and above, rounded up below 4t. "
        if (masteryLevel >= 5 || masteryLevel <= 0)
        {
            if (baseSpeed >= 4)
            {
                return baseSpeed / 2;
            }
            else
            {
                return (baseSpeed + 1) / 2;
            }
        }
        else if (masteryLevel >= 3)
        {
            // "attack rate set to 80%, rounding down." e.g. https://oldschool.runescape.wiki/w/Melee_III
            return (int) Math.floor(((double) baseSpeed) * 0.8);
        }
        return baseSpeed;
    }

    public void onGameTick(Client client, GameTick tick) {}
}
