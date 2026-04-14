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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.attacktimer.AttackTimerMetronomePlugin.AttackState;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;
import org.junit.Test;

public class TormentedDemonsTest extends IntegrationTests
{
    @Test
    public void PunishTest() throws Exception
    {
        // Equip a weapon which is normally MORE than 4 ticks and correct attack style
        runTest("PunishTest", 8, 0, 0, 3);
    }

    @Test
    public void PunishWastedTest() throws Exception
    {
        runTest("PunishWastedTest", 4, 0, 0, 3);
    }

    @Test
    public void PunishWastedWrongStyleTest() throws Exception
    {
        runTest("PunishWastedWrongStyleTest", 8, 0, 8, 7);
    }

    private void runTest(String testName, int aspeed, int EQUIPPED_WEAPON_TYPE, int ATTACK_STYLE, int expected)
            throws Exception
    {
        when(mockedItemManager.getItemStats(-1))
                .thenReturn(new ItemStats(true, 0, 0, ItemEquipmentStats.builder().aspeed(aspeed).build()));
        // Use weapon style specified by the test
        when(mockedClient.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE)).thenReturn(EQUIPPED_WEAPON_TYPE);
        when(mockedClient.getVarpValue(VarPlayer.ATTACK_STYLE)).thenReturn(ATTACK_STYLE);
        // -- Attack Styles
        EnumComposition mockedWeaponEnum = mock(EnumComposition.class);
        when(mockedClient.getEnum(EnumID.WEAPON_STYLES)).thenReturn(mockedWeaponEnum);
        when(mockedWeaponEnum.getIntValue(0)).thenReturn(-1);

        Player player = pluginMockSetup();

        ByteArrayDataOutput channel = ByteStreams.newDataOutput();
        underTest.writeState(channel);

        // Trigger the td spot animation so the next punish attack should be 4 ticks
        when(td.hasSpotAnim(2852)).thenReturn(true);
        // Tell the client a tick has occurred
        onGameTick(channel);
        assertSame(AttackState.NOT_ATTACKING, underTest.attackState);
        // reset the animation (it does last longer than a tick but a tick should be all the client needs)
        when(td.hasSpotAnim(2852)).thenReturn(false);
        // finally "do an attack" and see if the plugin correctly noticed that this is a punish
        when(player.getAnimation()).thenReturn(AnimationData.MELEE_GENERIC_SLASH.animationId);
        onGameTick(channel);

        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        assertSame(expected, underTest.attackDelayHoldoffTicks);

        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + testName + ".txt"));
    }

    @Override
    public Player pluginMockSetup() throws Exception
    {
        // enable the plugin
        when(mockedConfig.enableMetronome()).thenReturn(true);
        // Create player
        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getAnimation()).thenReturn(-1);

        // Create the tormented demon
        td = mock(NPC.class);
        NPCComposition mockedCompositions = mock(NPCComposition.class);
        when(td.getComposition()).thenReturn(mockedCompositions);
        int mockedNpcId = 13600;
        when(td.getId()).thenReturn(mockedNpcId);
        String[] actions = {
                "Attack", "Examine"
        };
        when(mockedCompositions.getActions()).thenReturn(actions);
        when(mockedNpcManager.getHealth(mockedNpcId)).thenReturn(1);

        // set the player as "attacking" the NPC
        when(mockedClient.getLocalPlayer()).thenReturn(mockedPlayer);
        when(mockedPlayer.getInteracting()).thenReturn(td);

        // need some extra mocks to stop the plugin running into an exception on the
        // client APIs
        // -- Mock World
        mockedWorldView = mock(WorldView.class);
        when(mockedClient.getTopLevelWorldView()).thenReturn(mockedWorldView);
        when(mockedClient.getWorldView(0)).thenReturn(mockedWorldView);
        int mockedPlane = 0;
        when(mockedWorldView.getPlane()).thenReturn(mockedPlane);
        WorldPoint worldPoint = new WorldPoint(0, 0, mockedPlane);
        LocalPoint localPoint = new LocalPoint(0, 0, mockedPlane);
        when(mockedPlayer.getWorldLocation()).thenReturn(worldPoint);
        when(mockedPlayer.getLocalLocation()).thenReturn(localPoint);
        var memsWorld = EnumSet.of(WorldType.MEMBERS);
        when(mockedClient.getWorldType()).thenReturn(memsWorld);
        // -- NPCs
        worldViewNPCiter(td);

        // Finally turn the plugin "on"
        underTest.startUp();
        return mockedPlayer;
    }

    protected WorldView mockedWorldView;
    protected NPC td;

    private void worldViewNPCiter(NPC mockedTarget)
    {
        // do a bit of redirection trickery to get a IndexedObjectSet of a non empty list.
        var npcs = new ArrayList<NPC>(1);
        npcs.add(mockedTarget);
        IndexedObjectSet npcsType = mock(IndexedObjectSet.class);
        when(npcsType.iterator()).thenReturn(npcs.iterator());
        when(mockedWorldView.npcs()).thenReturn(npcsType);
    }

    @Override
    protected void onGameTick(ByteArrayDataOutput file)
    {
        super.onGameTick(file);
        // Since an iterator is stateful and consumed by the plugin we re-mock it each time so it's always
        // fresh.
        worldViewNPCiter(td);
    }
}
