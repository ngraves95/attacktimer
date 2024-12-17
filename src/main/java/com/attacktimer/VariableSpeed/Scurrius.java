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
import com.attacktimer.ClientUtils.Utils;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;

/**
 * Scurrius: https://oldschool.runescape.wiki/w/Scurrius/Strategies#Strategies
 *
 * When attacking the giant rats summoned by Scurrius and attacking them with a bone weapon the player has no
 * attack delay.
 */
public class Scurrius implements IVariableSpeed
{
    private static final int BONE_STAFF_ID = 28796;
    private static final int BONE_MACE_ID = 28792;
    private static final int BONE_BOW_ID = 28794;
    private static final int GIANT_RAT = 7223;

    private static final int SCURRIUS_REGION_ID = 13210;

    private static final int SCURRIUS_MIN_X = 3290;
    private static final int SCURRIUS_MAX_X = 3307;

    private static final int SCURRIUS_MIN_Y = 9859;
    private static final int SCURRIUS_MAX_Y = 9876;

    private static boolean attackingGiantRatWithBoneWeapon(final int equipped, final int regionId, final int x, final int y, final int target)
    {
        final boolean correctWeapon = equipped == BONE_STAFF_ID || equipped == BONE_MACE_ID || equipped == BONE_BOW_ID;
        final boolean correctCoords = x >= SCURRIUS_MIN_X && x <= SCURRIUS_MAX_X && y >= SCURRIUS_MIN_Y && y <= SCURRIUS_MAX_Y;
        final boolean correctRegion = regionId == SCURRIUS_REGION_ID;
        final boolean correctEnemy = target == GIANT_RAT;
        return correctWeapon && correctCoords && correctRegion && correctEnemy;
    }

    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkProcedure, final int baseSpeed, final int curSpeed)
    {
        final WorldPoint location = Utils.getLocation(client);
        final int weaponId = Utils.getWeaponId(client);
        final int targetId = Utils.getTargetId(client);
        if (attackingGiantRatWithBoneWeapon(weaponId, location.getRegionID(), location.getX(), location.getY(), targetId))
        {
            return 1;
        }
        return curSpeed;
    }
    public void onGameTick(final Client client, final GameTick tick) {}
}
