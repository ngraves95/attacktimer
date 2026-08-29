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
import com.attacktimer.AttackSpeed;
import com.attacktimer.Attacking.Attacking;
import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.Spellbook;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.ItemManager;

@Slf4j
public class DoomOfMokhaiotl
{
    private static final int DOOM_REGION_ID = -1;
    private static final Set<Integer> DEMONIC_LARVAE_IDS = new ImmutableSet.Builder<Integer>()
            .add(NpcID.DOM_DEMONIC_ENERGY)
            .add(NpcID.DOM_DEMONIC_ENERGY_GIANT_MAGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_GIANT_RANGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_MAGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_RANGE)
            .add(NpcID.DOM_DEMONIC_ENERGY_MELEE)
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


    private final AttackSpeed attackSpeed;

    DoomOfMokhaiotl(final AttackSpeed attackSpeed)
    {
        this.attackSpeed = attackSpeed;
    }

    // https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl/Strategies#Demonic_larvae
    //
    // They may be attacked on attack cooldown: Non-demonbane attacks incur the weapon's attack delay
    // afterwards, whereas demonbane attacks and the Eye of Ayak will not incur any attack delay.
    public int onRender(final Client client, final ItemManager itemManager, final int attackDelayHoldoffTicks, final Spellbook spellbook, final boolean debugLogs)
    {
        if (!Utils.isInRegionId(client, DOOM_REGION_ID))
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
        if (!DEMONIC_LARVAE_IDS.contains(npc.getId()))
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

        return attackSpeed.compute(client, anim, spellbook, itemManager);
    }
}
