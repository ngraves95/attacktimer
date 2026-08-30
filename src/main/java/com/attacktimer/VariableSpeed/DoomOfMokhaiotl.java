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
import com.attacktimer.AttackSpeed;
import com.attacktimer.Attacking.Attacking;
import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.Spellbook;
import com.attacktimer.VariableSpeed.State.TickCount;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.ItemManager;

@Slf4j
public class DoomOfMokhaiotl implements IVariableSpeed
{
    // As you delve deeper you change region
    private static final Set<Integer> DOOM_REGION_IDS = new ImmutableSet.Builder<Integer>()
            .add(5269) // Delve 1
            .add(13668) // Delve 2 - 5
            .add(14180) // Delve 5+
            .build();
    // All larvae and the volatile earth work the same:
    // - if you attack them with demon bane you get no CD.
    // - else non demonbane overwrites you're current CD
    // - you can attack them whilst on CD.
    private static final Set<Integer> COOLDOWN_OVERWRITES_IDS = new ImmutableSet.Builder<Integer>()
            .add(NpcID.DOM_DEMONIC_ENERGY)
            .add(NpcID.DOM_DEMONIC_ENERGY_GIANT_MAGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_GIANT_RANGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_MAGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_RANGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_MELEE)
            .add(NpcID.DOM_SHOCKWAVE_PATH_NODE)
            .build();

    private static final Set<Integer> NO_COOLDOWN_WEAPON = new ImmutableSet.Builder<Integer>()
            .add(ItemID.SILVERLIGHT)
            .add(ItemID.DARKLIGHT)
            .add(ItemID.ARCLIGHT)
            .add(ItemID.EMBERLIGHT)
            .add(ItemID.BONE_CLAWS)
            .add(ItemID.SCORCHING_BOW)
            .add(ItemID.HOLY_WATER)
            .add(ItemID.EYE_OF_AYAK)
            .build();

    private final TickCount tickCount;
    private final AttackSpeed attackSpeed;
    private int larvaeConsumed = -1;
    private int shieldConsumed = -1;

    DoomOfMokhaiotl(final TickCount tc, final AttackSpeed attackSpeed)
    {
        this.tickCount = tc;
        this.attackSpeed = attackSpeed;
    }

    // https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl/Strategies#Demonic_larvae
    //
    // They may be attacked on attack cooldown: Non-demonbane attacks incur the weapon's attack delay
    // afterwards, whereas demonbane attacks and the Eye of Ayak will not incur any attack delay.
    //
    // Doom can also be attacked whilst on cooldown when he is charging his shield (melee punish only)
    //
    // The https://oldschool.runescape.wiki/w/Volatile_earth can also be attacked with no attack delay.
    public int onRender(final Client client, final ItemManager itemManager, final int attackDelayHoldoffTicks, final Spellbook spellbook, final boolean debugLogs)
    {
        if (!Utils.isInRegionId(client, DOOM_REGION_IDS))
        {
            return attackDelayHoldoffTicks;
        }

        final var atk = Attacking.PlayerAttack(client);
        final AnimationData anim = AnimationData.fromId(atk.getAnimationId());
        if (anim == null || atk.getTarget() == null || !(atk.getTarget() instanceof NPC))
        {
            return attackDelayHoldoffTicks;
        }

        final NPC npc = (NPC) atk.getTarget();
        final int npcId = npc.getId();
        if (COOLDOWN_OVERWRITES_IDS.contains(npcId))
        {
            if (tickCount.isWithinNTicks(larvaeConsumed, 1))
            {
                return attackDelayHoldoffTicks;
            }
            final int weaponId = Utils.getWeaponId(client);
            final boolean isDemonbaneSpell = spellbook == Spellbook.ARCEUUS && AnimationData.isManualCasting(anim) && anim == AnimationData.MAGIC_ARCEUUS_DEMONBANE;
            if (NO_COOLDOWN_WEAPON.contains(weaponId) || isDemonbaneSpell)
            {
                return attackDelayHoldoffTicks;
            }

            if (debugLogs)
            {
                log.debug("DoomOfMokhaiotl success, attacking larvae with normal weapon");
            }
            larvaeConsumed = tickCount.get();
            return attackSpeed.compute(client, anim, spellbook, itemManager);
        }
        else if (npcId == NpcID.DOM_BOSS)
        {
            if (tickCount.isWithinNTicks(shieldConsumed, 30))
            {
                return attackDelayHoldoffTicks;
            }
            final var animId = npc.getAnimation();
            // Undocumented in the wiki but from my testing these can be hit while on cooldown but unlike the
            // grubs do add up the delay. This is original research:
            //
            // Atk (5) 1923 -> Chally (7) 1926      -> If plain off CD 1935 (actual: 1933 = 2 tick reduction) (3 tick gap between punish)
            // Atk (5) 1945 -> Swift blade (3) 1946 -> If plain off CD 1953 (actual: 1949 = 4 tick reduction) (1 tick gap between punish)
            // Atk (5) 1964 -> Rapier (4) 1966      -> If plain off CD 1973 (actual: 1970 = 3 tick reduction) (2 tick gap between punish)
            // Atk (5) 2046 -> Battle axe (6) 2048  -> If plain off CD 2057 (actual: 2054 = 3 tick reduction) (2 tick gap between punish)
            // Atk (5) 4317 -> Rapier (4) 4318      -> If plain off CD 4326 (actual: 4322 = 4 tick reduction) (1 tick gap between punish)
            //
            // Therefore my conclusion based off these samples is that resulting delay is just the attack
            // delay of the weapon used, overwriting the current delay not addition.
            if (animId == AnimationID.DOM_BEAM_CHARGE_LOOP || animId == AnimationID.DOM_BEAM_CHARGE)
            {
                if (Utils.getAttackType(client).IsMelee())
                {
                    if (debugLogs)
                    {
                        log.debug("DoomOfMokhaiotl success, on cooldown melee swing");
                    }
                    shieldConsumed = tickCount.get();
                    return attackSpeed.compute(client, anim, spellbook, itemManager);
                }
            }
            return attackDelayHoldoffTicks;
        }
        else
        {
            return attackDelayHoldoffTicks;
        }
    }

    // Take care here to ensure no infinite loop or affect on the speed as the onRender does call this via variable speed
    public int apply(final Client client, final AnimationData curAnimation, final AttackProcedure atkType,
            final Spellbook spellbook, final int damageDealt, final int lastSpecDelta, final int baseSpeed,
            final int curSpeed)
    {
        final int targetId = Utils.getTargetId(client);
        final boolean inDoom = Utils.isInRegionId(client, DOOM_REGION_IDS);
        if (inDoom && COOLDOWN_OVERWRITES_IDS.contains(targetId))
        {
            final int weaponId = Utils.getWeaponId(client);
            final boolean isDemonbaneSpell = spellbook == Spellbook.ARCEUUS && AnimationData.isManualCasting(curAnimation) && curAnimation == AnimationData.MAGIC_ARCEUUS_DEMONBANE;
            if (NO_COOLDOWN_WEAPON.contains(weaponId) || isDemonbaneSpell)
            {
                log.debug("DoomOfMokhaiotl success, zero delay grub");
                return 1;
            }
        }
        return curSpeed;
    }
}
