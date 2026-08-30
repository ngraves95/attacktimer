package com.attacktimer.ClientUtils;

/*
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
 * Copyright (c) 2024-2026, Lexer747 <https://github.com/Lexer747>
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
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
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
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import org.apache.commons.lang3.ArrayUtils;

public class Utils
{
    public static int getItemIdFromContainer(final ItemContainer container, final int slotID)
    {
        if (container == null)
        {
            return -1;
        }
        final Item item = container.getItem(slotID);
        return (item != null) ? item.getId() : -1;
    }

    public static int getWeaponIdRaw(final Client client)
    {
        return getItemIdFromContainer(client.getItemContainer(InventoryID.WORN),
                EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    // getLocation will return the current world point of the player accounting for instances.
    //
    // For computing tile based distances you probably don't want this and instead should use
    // client.getLocalPlayer().getWorldLocation().
    public static WorldPoint getLocalLocation(final Client client)
    {
        final LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
        return WorldPoint.fromLocalInstance(client, localPoint);
    }

    // returns ACCURATE for unknown weapons/styles
    public static AttackStyle getAttackStyle(final Client client)
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
    public static WeaponType getWeaponType(final Client client)
    {
        final int currentEquippedWeaponTypeVarbit = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
        return WeaponType.getWeaponType(currentEquippedWeaponTypeVarbit);
    }

    // returns null for unknown weapons
    public static AttackType getAttackType(final Client client)
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
    public static int getTargetId(final Client client)
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
    public static NPC getTargetNPC(final Client client)
    {
        final Actor target = client.getLocalPlayer().getInteracting();
        if (target != null && (target instanceof NPC))
        {
            final NPC npc = (NPC) target;
            return npc;
        }
        return null;
    }

    // returns true if the client is in the region specified by the id
    public static boolean isInRegionId(final Client client, final int id)
    {
        final int[] regions = regions(client);
        if (regions == null || regions.length == 0)
        {
            return false;
        }

        return ArrayUtils.contains(regions, id);
    }
    // returns true if the client is in the region specified by the id
    public static boolean isInRegionId(final Client client, final Set<Integer> ids)
    {
        final int[] regions = regions(client);
        if (regions == null || regions.length == 0)
        {
            return false;
        }

        for (final int id : regions)
        {
            if (ids.contains(id))
            {
                return true;
            }
        }
        return false;
    }
    private static int[] regions(final Client client)
    {
        final WorldView wv = client.getTopLevelWorldView();
        if (wv == null)
        {
            return null;
        }
        return wv.getMapRegions();
    }

    // getLastDelta gets the last two elements and returns the delta between the two items. It does not modify
    // the queue. Returns 0 if theres no items in the queue, returns <element> + 1 if there's only 1 item in
    // the queue.
    public static int getLastDelta(final ArrayDeque<Integer> events)
    {
        int i = 0, last = -1, secondLast = -1;
        final var it = events.descendingIterator();
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

    // Map of problematic itemIds to equivalent working ones.
    // The Echo Venator Bow's ItemStats are returning null, so use the regular bow instead.
    private static final Map<Integer, Integer> WEAPON_ID_MAPPING_WORKAROUNDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ItemID.VENATOR_BOW_ORNAMENT, ItemID.VENATOR_BOW)
            .build();

    // Add other weapons here if in the Runelite dev shell this prints a different value to it's actual
    // speed:
    //
    // var itemManager = inject(ItemManager.class);
    // log.info("Speed {}", itemManager.getItemStats(<id_to_test>).getEquipment().getAspeed());
    private static final Map<Integer, Integer> NON_STANDARD_ATTACK_SPEEDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ItemID.HALLOWFELL, 6)
            .build();

    public static int getWeaponId(final Client client)
    {
        final int weaponId = Utils.getWeaponIdRaw(client);
        return WEAPON_ID_MAPPING_WORKAROUNDS.getOrDefault(weaponId, weaponId);
    }

    public static ItemStats getWeaponStats(final Client client, final ItemManager itemManager, final int weaponId)
    {
        if (NON_STANDARD_ATTACK_SPEEDS.containsKey(weaponId))
        {
            return new ItemStats(true, -1, -1,
                    ItemEquipmentStats.builder().aspeed(NON_STANDARD_ATTACK_SPEEDS.get(weaponId)).build());
        }
        return itemManager.getItemStats(weaponId);
    }

    public static int getWeaponSpeed(final Client client, final ItemManager itemManager, final int weaponId)
    {
        final ItemStats weaponStats = getWeaponStats(client, itemManager, weaponId);
        if (weaponStats == null)
        {
            // Assume bare-handed
            return 4;
        }
        return weaponStats.getEquipment().getAspeed();
    }
}
