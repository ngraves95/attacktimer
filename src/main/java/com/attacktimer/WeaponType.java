package com.attacktimer;

/*
 * Copyright (c) 2017, honeyhoney <https://github.com/honeyhoney>
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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Getter;
import static com.attacktimer.AttackStyle.ACCURATE;
import static com.attacktimer.AttackStyle.AGGRESSIVE;
import static com.attacktimer.AttackStyle.CASTING;
import static com.attacktimer.AttackStyle.DEFENSIVE;
import static com.attacktimer.AttackStyle.DEFENSIVE_CASTING;
import static com.attacktimer.AttackStyle.OTHER;
import static com.attacktimer.AttackType.CRUSH;
import static com.attacktimer.AttackType.SLASH;
import static com.attacktimer.AttackType.STAB;
import static com.attacktimer.AttackType.RANGED;
import static com.attacktimer.AttackType.MAGIC;
import static com.attacktimer.AttackType.NONE;

import net.runelite.api.Client;
import net.runelite.api.ParamID;
import net.runelite.api.EnumID;
import net.runelite.api.StructComposition;

public enum WeaponType
{
    // https://oldschool.runescape.wiki/w/Weapons/Types
    // * Note this ordering the ordering in which the enum is defined by the client you should not re-order
    //   this list
    // * Note the ordering of attack types is arbitrary and also defined by the client and should not be
    //   re-ordered. Where nulls are empty spots. Arrays are either 4 or 6 items long.
    //
    // Taken from
    // https://github.com/Joshua-F/cs2-scripts/blob/562819dbac880d2890daa40adcb209eb7413d2dd/scripts/%5Bclientscript%2Ccombat_interface_setup%5D.cs2
    // Not sure why the latest update to that repo has this script deleted but the gathered data from the
    // older commit still seem fine.
    UNARMED(CRUSH, CRUSH, null, CRUSH),
    AXE(SLASH, SLASH, CRUSH, SLASH),
    BLUNT(CRUSH, CRUSH, null, CRUSH),
    BOW(RANGED, RANGED, null, RANGED),
    CLAW(SLASH, SLASH, STAB, SLASH),
    HEAVY_RANGED(RANGED, RANGED, null, RANGED),
    SALAMANDER(SLASH, RANGED, MAGIC, null),
    CHINCHOMPA(RANGED, RANGED, null, RANGED),
    GUN(NONE, CRUSH, null, null),
    SLASH_SWORD(SLASH, SLASH, STAB, SLASH),
    TWO_HANDED_SWORD(SLASH, SLASH, CRUSH, SLASH),
    PICKAXE(STAB, STAB, CRUSH, STAB),
    POLEARM(STAB, SLASH, null, STAB),
    POLESTAFF(CRUSH, CRUSH, null, CRUSH),
    SCYTHE(SLASH, SLASH, CRUSH, SLASH),
    SPEAR(STAB, SLASH, CRUSH, STAB),
    SPIKED(CRUSH, CRUSH, STAB, CRUSH),
    STAB_SWORD(STAB, STAB, SLASH, STAB),
    STAFF(CRUSH, CRUSH, null, CRUSH, MAGIC, MAGIC),
    THROWN(RANGED, RANGED, null, RANGED),
    WHIP(SLASH, SLASH, null, SLASH),
    BLADED_STAFF(STAB, SLASH, null, CRUSH, MAGIC, MAGIC),
    TWO_HANDED_SWORD_GODSWORD(STAB, SLASH, null, CRUSH, MAGIC, MAGIC),
    POWERED_STAFF(SLASH, SLASH, CRUSH, SLASH),
    BANNER(MAGIC, MAGIC, null, MAGIC),
    POLEARM_CRYSTAL_HALBERD(STAB, SLASH, CRUSH, STAB),
    BLUDGEON(STAB, SLASH, null, STAB),
    BULWARK(CRUSH, CRUSH, null, CRUSH),
    POWERED_WAND(CRUSH, null, null, NONE),
    PARTISAN(MAGIC, MAGIC, null, MAGIC),
    PARTISAN_2(STAB, STAB, CRUSH, STAB);

    private static final Map<Integer, WeaponType> weaponTypes;

    @Getter
    private final AttackType[] attackTypes;

    static
    {
        ImmutableMap.Builder<Integer, WeaponType> builder = new ImmutableMap.Builder<>();

        for (WeaponType weaponType : values())
        {
            builder.put(weaponType.ordinal(), weaponType);
        }

        weaponTypes = builder.build();
    }

    WeaponType(AttackType... attackTypes)
    {
        this.attackTypes = attackTypes;
    }

    // Copied from https://github.com/runelite/runelite/commit/ffe16454ba894d673db91e2d1fd8d3c4c265b2c7
    // runelite-client\src\main\java\net\runelite\client\plugins\attackstyles\AttackStylesPlugin.java IMO This
    // code should remain fairly in-sync with the upstream unless there is good reason to fork. License was
    // in-sync at time of copying. Credit: [Adam-](https://github.com/Adam-)
    // [honeyhoney](https://github.com/honeyhoney)
    //
    // It seems that at some point the code to generate an attack style started to come from the client rather
    // than being hardcoded. However this only generates the Style of attack not the attack type which is
    // still hardcoded.
    public AttackStyle[] getAttackStyles(Client client)
    {
        // from script4525
        int weaponType = this.ordinal();
        int weaponStyleEnum = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(weaponType);
        if (weaponStyleEnum == -1)
        {
            // Blue moon spear
            if (weaponType == 22)
            {
                return new AttackStyle[]{
                    ACCURATE, AGGRESSIVE, null, DEFENSIVE, CASTING, DEFENSIVE_CASTING
                };
            }

            if (weaponType == 30)
            {
                // Partisan
                return new AttackStyle[]{
                    ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE
                };
            }
            return new AttackStyle[0];
        }
        int[] weaponStyleStructs = client.getEnum(weaponStyleEnum).getIntVals();

        AttackStyle[] styles = new AttackStyle[weaponStyleStructs.length];
        int i = 0;
        for (int style : weaponStyleStructs)
        {
            StructComposition attackStyleStruct = client.getStructComposition(style);
            String attackStyleName = attackStyleStruct.getStringValue(ParamID.ATTACK_STYLE_NAME);
            AttackStyle attackStyle = AttackStyle.valueOf(attackStyleName.toUpperCase());
            if (attackStyle == OTHER)
            {
                // "Other" is used for no style
                ++i;
                continue;
            }

            // "Defensive" is used for Defensive and also Defensive casting
            if (i == 5 && attackStyle == DEFENSIVE)
            {
                attackStyle = DEFENSIVE_CASTING;
            }

            styles[i++] = attackStyle;
        }
        return styles;
    }

    public static WeaponType getWeaponType(int id)
    {
        return weaponTypes.get(id);
    }
}