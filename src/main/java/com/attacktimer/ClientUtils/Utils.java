package com.attacktimer.ClientUtils;

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

import com.attacktimer.AttackStyle;
import com.attacktimer.AttackType;
import com.attacktimer.WeaponType;
import java.util.ArrayDeque;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.apache.commons.lang3.ArrayUtils;

public class Utils
{
    public static int getItemIdFromContainer(ItemContainer container, int slotID)
    {
        if (container == null)
        {
            return -1;
        }
        final Item item = container.getItem(slotID);
        return (item != null) ? item.getId() : -1;
    }

    public static int getWeaponId(Client client)
    {
        return getItemIdFromContainer(client.getItemContainer(InventoryID.WORN),
                EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    // getLocation will return the current world point of the player accounting for instances.
    public static WorldPoint getLocation(Client client)
    {
        WorldPoint location = client.getLocalPlayer().getWorldLocation();
        final LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
        location = WorldPoint.fromLocalInstance(client, localPoint);
        return location;
    }

    // returns ACCURATE for unknown weapons/styles
    public static AttackStyle getAttackStyle(Client client)
    {
        final AttackStyle[] attackStyles = getWeaponType(client).getAttackStyles(client);
        int currentAttackStyleVarbit = client.getVarpValue(VarPlayerID.COM_MODE);
        final int castingMode = client.getVarbitValue(VarbitID.AUTOCAST_DEFMODE);
        if (currentAttackStyleVarbit < attackStyles.length)
        {
            // from script4525
            // Even though the client has 5 attack styles for Staffs, only attack styles 0-4 are used, with an additional
            // casting mode set for defensive casting
            if (currentAttackStyleVarbit == 4)
            {
                currentAttackStyleVarbit += castingMode;
            }
            return attackStyles[currentAttackStyleVarbit];
        }

        return AttackStyle.ACCURATE;
    }

    // returns null for unknown weapons
    public static WeaponType getWeaponType(Client client)
    {
        final int currentEquippedWeaponTypeVarbit = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
        return WeaponType.getWeaponType(currentEquippedWeaponTypeVarbit);
    }

    // returns null for unknown weapons
    public static AttackType getAttackType(Client client)
    {
        final WeaponType weaponType = getWeaponType(client);
        final int currentAttackStyleVarbit = client.getVarpValue(VarPlayerID.COM_MODE);
        if (currentAttackStyleVarbit < weaponType.getAttackTypes().length)
        {
            return weaponType.getAttackTypes()[currentAttackStyleVarbit];
        }
        return null;
    }

    // returns zero for no target
    public static int getTargetId(Client client)
    {
        final NPC target = getTargetNPC(client);
        int targetId = 0;
        if (target != null)
        {
            targetId = target.getId();
        }
        return targetId;
    }

    // returns null for no target
    public static NPC getTargetNPC(Client client)
    {
        final Actor target = client.getLocalPlayer().getInteracting();
        if (target != null && (target instanceof NPC))
        {
            final NPC npc = (NPC) target;
            return npc;
        }
        return null;
    }

    // TODO comment
    public static boolean isInRegionId(Client client, int id)
    {
        WorldView wv = client.getTopLevelWorldView();
        if (wv == null)
        {
            return false;
        }

        int[] regions = wv.getMapRegions();
        if (regions == null || regions.length == 0)
        {
            return false;
        }

        return ArrayUtils.contains(regions, id);
    }

    // TODO comment
    public static int getLastDelta(ArrayDeque<Integer> events)
    {
        int i = 0, last = -1, secondLast = -1;
        var it = events.descendingIterator();
        while (it.hasNext())
        {
            if (i == 0)
                last = it.next();
            else if (i == 1)
                secondLast = it.next();
            else
                break;
            i++;
        }
        var delta = last - secondLast;
        return delta;
    }
}
