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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import com.attacktimer.AnimationData;
import com.attacktimer.AttackProcedure;
import com.attacktimer.AttackType;
import com.attacktimer.WeaponType;
import com.attacktimer.ClientUtils.Utils;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;

/**
 * TormentedDemons is the variable speed implementation for the "punish" attack a player can do against a
 * demon.
 *
 * https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies#Fire_bombs_and_shield
 * > Within this moment of vulnerability, attacking the demon using crush weapons, heavy ranged weapons
 * > (crossbows and ballistae), or casting a spell (not from a powered staff) increases all damage dealt by
 * > X2−16, where X is the weapon's attack speed. The attack speed for this one attack is set to 4 ticks
 * > (2.4s); if multiple demons are attacking, the attack speed decreases by one tick per additional demon
 * > that is fighting the player.
 *
 * This class works by keeping a map which tracks every demon that the client is provided, then if any demon
 * triggers a spot animation matching the vulnerability animation then we store this in the map. Every attack
 * the parent {@see VariableSpeed} will call apply, in which if we are attacking a vulnerable demon we can do
 * extra checks and compute the new attack speed and show the correct tick countdown.
 */
public class TormentedDemons implements IVariableSpeed
{
    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkProcedure, final int baseSpeed, final int curSpeed)
    {
        WorldPoint location = Utils.getLocation(client);
        int weaponId = Utils.getWeaponId(client);
        int targetId = Utils.getTargetId(client);
        if (!attackingTormentedDemon(weaponId, location.getRegionID(), location.getX(), location.getY(), targetId))
        {
            return curSpeed;
        }
        final NPC target = Utils.getTargetNPC(client);
        if (!tormentedDemons.containsKey(target))
        {
            // This doesn't feel very likely but this is more graceful than throwing.
            return curSpeed;
        }
        final DemonData targetDemon = tormentedDemons.get(target);
        if (!targetDemon.isVulnerable(tickCount))
        {
            return curSpeed;
        }
        // The demon is vulnerable! However if it's already been attacked in a previous tick then the
        // vulnerability is moot.
        if (targetDemon.vulnConsumed(tickCount))
        {
            return curSpeed;
        }
        // Finally the last checks, only certain attack styles and weapons can trigger the effect.
        switch (atkProcedure)
        {
            case POWERED_STAVE:
                // Powered staves cannot trigger the effect
                // TODO edge case with harm?
                return curSpeed;
            case MANUAL_AUTO_CAST:
                break;
            case MELEE_OR_RANGE:
                final WeaponType wep = Utils.getWeaponType(client);
                final AttackType attkType = Utils.getAttackType(client);
                if (wep != WeaponType.HEAVY_RANGED && attkType != AttackType.CRUSH)
                {
                    // This weapon is not a heavy ranged weapon or a melee weapon set to crush
                    // so it wont trigger the shorter cooldown.
                    return curSpeed;
                }
                break;
        }
        // All checks passed this is consuming the vulnerability, change our speed.
        //
        // TODO implement more than 1 demon cases
        if (curSpeed > 4)
        {
            return 4;
        }
        else
        {
            return curSpeed;
        }
    }

    private static final int TORMENTED_DEMON_VULN_SPOT_ANIM = 2852;
    private static final int TORMENTED_DEMON_ID = 13600;

    private static final int TORMENTED_DEMON_2_ID = 13599;
    private static final int TORMENTED_DEMON_REGION_ID = 16197;

    private static final int TORMENTED_DEMON_REGION_2_ID = 16452;
    private static final int TORMENTED_DEMON_MIN_X = 4010;

    private static final int TORMENTED_DEMON_MAX_X = 4180;
    private static final int TORMENTED_DEMON_MIN_Y = 4320;

    private static final int TORMENTED_DEMON_MAX_Y = 4490;

    private static boolean attackingTormentedDemon(int equipped, int regionId, int x, int y, int target)
    {
        boolean correctCoords = x >= TORMENTED_DEMON_MIN_X && x <= TORMENTED_DEMON_MAX_X && y >= TORMENTED_DEMON_MIN_Y && y <= TORMENTED_DEMON_MAX_Y;
        boolean correctRegion = regionId == TORMENTED_DEMON_REGION_ID || regionId == TORMENTED_DEMON_REGION_2_ID;
        return correctCoords && correctRegion && isTormentedDemon(target);
    }

    private static boolean isTormentedDemon(int targetId)
    {
        return targetId == TORMENTED_DEMON_ID || targetId == TORMENTED_DEMON_2_ID;
    }

    private Map<NPC, DemonData> tormentedDemons = new HashMap<NPC,DemonData>();;
    private int tickCount;

    public void onGameTick(Client client, GameTick tick)
    {
        tickCount++;
        for (NPC npc : client.getTopLevelWorldView().npcs())
        {
            if (!isTormentedDemon(npc.getId())) { continue; }
            boolean isVulnerable = npc.hasSpotAnim(TORMENTED_DEMON_VULN_SPOT_ANIM);
            if (tormentedDemons.containsKey(npc))
            {
                DemonData d = tormentedDemons.get(npc);
                d.update(tickCount, isVulnerable);
            }
            else
            {
                tormentedDemons.put(npc, new DemonData(tickCount, isVulnerable));
            }
        }
        // Only check for staleness every so often
        if (tickCount % 100 == 0)
        {
            var toDelete = new ArrayList<NPC>();
            for (Entry<NPC, DemonData> td : tormentedDemons.entrySet())
            {
                if (td.getValue().isStale(tickCount))
                {
                    toDelete.add(td.getKey());
                }
            }
            for (NPC td : toDelete)
            {
                tormentedDemons.remove(td);
            }
        }
    }

    /**
     * DemonData is an internal helper class containing the last ticks in which a demon was noticed by the
     * client, the tick it was vulnerable (if ever) and the tick in which it was attacked while vulnerable.
     */
    class DemonData
    {
        // VulTicksAfterEnd is just a guess the wiki isn't clear how long this period is, from testing 10
        // ticks feels about right.
        private static final int VulTicksAfterEnd = 10;
        private int lastSpotted;
        private Integer vulnerableStart;
        private Integer vulnerableFinish;
        private int attacked;
        DemonData(int tick, boolean vuln)
        {
            lastSpotted = tick;
            this.update(tick, vuln);
            this.attacked = -1;
        }

        void update(int tick, boolean vuln)
        {
            // NOTE: we can't use the chat message "The demon's spell binds you" because this doesn't trigger
            // at the start of any kill.
            lastSpotted = tick;
            if (vuln && this.vulnerableStart == null)
            {
                this.vulnerableStart = Integer.valueOf(tick);
                this.vulnerableFinish = Integer.valueOf(tick);
            }
            else if (vuln && this.vulnerableStart != null)
            {
                this.vulnerableFinish = Integer.valueOf(tick);
            }

            if (!vuln)
            {
                this.vulnerableStart = null;
            }
            if (this.attacked != -1 && this.attacked + 1 >= tick)
            {
                this.attacked = -1;
            }
        }

        boolean isVulnerable(int tick)
        {
            if (this.vulnerableFinish == null)
            {
                return false;
            }
            return (this.vulnerableFinish + VulTicksAfterEnd) > tick;
        }

        // isStale returns true if the last time this demon was spotted by the client was too long ago.
        boolean isStale(int tick)
        {
            if (this.lastSpotted + 50 < tick)
            {
                // Last update was over 50 ticks ago, this is stale
                return true;
            }
            return false;
        }

        boolean vulnConsumed(int tick)
        {
            if (this.attacked == tick || this.attacked == -1)
            {
                this.attacked = tick;
                return false;
            }
            else if (this.attacked > tick + VulTicksAfterEnd)
            {
                // already attacked within the window
                return true;
            }
            else
            {
                this.attacked = tick;
                return false;
            }
        }

        @Override
        public String toString()
        {
            return "created: " + String.valueOf(this.lastSpotted) + " vulnerableStart: " + String.valueOf(this.vulnerableStart) + " vulnerableFinish: " + String.valueOf(this.vulnerableFinish);
        }
    }

}
