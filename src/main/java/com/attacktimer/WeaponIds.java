package com.attacktimer;

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

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * WeaponIds is the hard coded list of weapon IDs which are important to this
 * plugin and how it works. As of writing this is just the 4-tick staves which
 * re-use animations from the spell books so need another way to be
 * distinguished.
 */
enum WeaponIds
{
    WEAPON_TRIDENT(11907), // https://oldschool.runescape.wiki/w/Trident_of_the_seas#Partially_charged
    WEAPON_TRIDENT_E(22288), // https://oldschool.runescape.wiki/w/Trident_of_the_seas_(e)#Charged
    WEAPON_SWAMP(12899), // https://oldschool.runescape.wiki/w/Trident_of_the_swamp#Charged
    WEAPON_SWAMP_E(22292), // https://oldschool.runescape.wiki/w/Trident_of_the_swamp_(e)#Charged

    // region CG
    WEAPON_BLUE_C_STAFF_B(23898), // https://oldschool.runescape.wiki/w/Crystal_staff_(basic)
    WEAPON_BLUE_C_STAFF_A(23899), // https://oldschool.runescape.wiki/w/Crystal_staff_(attuned)
    WEAPON_BLUE_C_STAFF_P(23900), // https://oldschool.runescape.wiki/w/Crystal_staff_(perfected)

    WEAPON_RED_C_STAFF_B(23852), // https://oldschool.runescape.wiki/w/Corrupted_staff_(basic)
    WEAPON_RED_C_STAFF_A(23853), // https://oldschool.runescape.wiki/w/Corrupted_staff_(attuned)
    WEAPON_RED_C_STAFF_P(23854), // https://oldschool.runescape.wiki/w/Corrupted_staff_(perfected)
    // endregion

    WEAPON_ACCURSED(27665, 27666), // https://oldschool.runescape.wiki/w/Accursed_sceptre

    WEAPON_SANG(22323), // https://oldschool.runescape.wiki/w/Sanguinesti_staff#Charged
    WEAPON_SANG_KIT(25731); // https://oldschool.runescape.wiki/w/Holy_sanguinesti_staff#Charged

    @Getter
    public final int[] id;

    WeaponIds(int... id)
    {
        this.id = id;
    }

    public static Set<Integer> FourTickStaves()
    {
        Set<Integer> result = new HashSet<Integer>();
        for (WeaponIds wep : WeaponIds.values())
        {
            for (int id : wep.id)
            {
                result.add(id);
            }
        }
        return result;
    }
}