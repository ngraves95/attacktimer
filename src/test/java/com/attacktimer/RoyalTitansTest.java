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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import org.junit.Test;

public class RoyalTitansTest extends IntegrationTests
{
    @Test
    public void SingleKillManualCast() throws Exception
    {
        int expected = 3;
        final int xp = 60;

        ByteArrayDataOutput channel = ByteStreams.newDataOutput();
        underTest.writeState(channel);

        writeTestMessage("distance 1", channel);
        runSingleTest(channel, 1, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 2", channel);
        runSingleTest(channel, 2, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 3", channel);
        runSingleTest(channel, 3, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 4", channel);
        runSingleTest(channel, 4, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 5", channel);
        runSingleTest(channel, 5, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 6", channel);
        runSingleTest(channel, 6, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 7", channel);
        runSingleTest(channel, 7, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 8", channel);
        runSingleTest(channel, 8, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        expected = 4;
        writeTestMessage("distance 9", channel);
        runSingleTest(channel, 9, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 10", channel);
        runSingleTest(channel, 10, expected, xp);

        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + "SingleKillManualCast.txt"));
    }

    private void runSingleTest(ByteArrayDataOutput channel, int distance, int expected, int xp)
            throws Exception, IOException
    {
        final Player player = pluginMockSetup();
        // Ensure we're using correct magic
        when(player.getAnimation()).thenReturn(AnimationData.MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF.animationId);

        final var it = iceElementals.iterator();
        final NPC target = it.next();
        // Set up hit delay by setting distance
        final var bounds = getWorldForRegionId(11669);
        final var wp = new WorldPoint(bounds.minX + 128, bounds.minY + 128, 0);
        when(target.getWorldLocation()).thenReturn(wp);
        when(player.getWorldLocation()).thenReturn(wp.dx(distance));
        when(player.getInteracting()).thenReturn(target);
        while (it.hasNext())
        {
            final var splashed = it.next();
            // need to mock all the other elementals, ensure they are off screen:
            when(splashed.getWorldLocation()).thenReturn(wp.dx(128));
        }

        // Ensure we deal enough damage
        underTest.onFakeXpDrop(new FakeXpDrop(Skill.HITPOINTS, xp));

        onGameTick(channel);

        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        assertSame(expected, underTest.attackDelayHoldoffTicks);
    }

    @Test
    public void AoEKillManualCast() throws Exception
    {
        // We start quicker than the single kills and expect decay as they get further away due to hit delay
        int expected = 1;
        final int xp = 60;

        ByteArrayDataOutput channel = ByteStreams.newDataOutput();
        underTest.writeState(channel);

        writeTestMessage("distance 1", channel);
        runAoETest(channel, 1, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 2", channel);
        runAoETest(channel, 2, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        expected = 2;
        writeTestMessage("distance 3", channel);
        runAoETest(channel, 3, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 4", channel);
        runAoETest(channel, 4, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 5", channel);
        runAoETest(channel, 5, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        expected = 3;
        writeTestMessage("distance 6", channel);
        runAoETest(channel, 6, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 7", channel);
        runAoETest(channel, 7, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 8", channel);
        runAoETest(channel, 8, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        expected = 4;
        writeTestMessage("distance 9", channel);
        runAoETest(channel, 9, expected, xp);
        for (int i = 0; i < expected; i++)
        {
            onGameTick(channel);
        }
        writeTestMessage("distance 10", channel);
        runAoETest(channel, 10, expected, xp);

        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + "AoEKillManualCast.txt"));
    }

    private void runAoETest(ByteArrayDataOutput channel, int distance, int expected, int xp)
            throws Exception, IOException
    {
        final Player player = pluginMockSetup();
        // Ensure we're using correct magic
        when(player.getAnimation()).thenReturn(AnimationData.MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF.animationId);

        when(mockedClient.getLocalPlayer()).thenReturn(player);
        final var it = iceElementals.iterator();
        final NPC target = it.next();
        // Set up hit delay by setting distance
        final var bounds = getWorldForRegionId(11669);
        final var wp = new WorldPoint(bounds.minX + 128, bounds.minY + 128, 0);
        when(target.getWorldLocation()).thenReturn(wp);
        when(player.getWorldLocation()).thenReturn(wp.dx(distance));
        when(player.getInteracting()).thenReturn(target);
        while (it.hasNext())
        {
            final var splashed = it.next();
            when(splashed.getWorldLocation()).thenReturn(wp);
        }

        // Ensure we deal enough damage
        underTest.onFakeXpDrop(new FakeXpDrop(Skill.HITPOINTS, xp));

        onGameTick(channel);

        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        assertSame(expected, underTest.attackDelayHoldoffTicks);

        // clean up:
        for (final var elemental : iceElementals)
        {
            underTest.onNpcDespawned(new NpcDespawned(elemental));
        }
    }

    @Override
    public Player pluginMockSetup() throws Exception
    {
        // enable the plugin
        when(mockedConfig.enableMetronome()).thenReturn(true);
        // Create player
        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getAnimation()).thenReturn(-1);

        // need some extra mocks to stop the plugin running into an exception on the
        // client APIs
        // -- Mock World

        WorldView mockedWorldView = mock(WorldView.class);
        when(mockedWorldView.isInstance()).thenReturn(true);
        when(mockedWorldView.getInstanceTemplateChunks()).thenReturn(createInstanceTemplateChunks(11669));
        int mockedPlane = 0;
        LocalPoint localPoint = LocalPoint.fromScene(30, 30, mockedWorldView);
        when(mockedClient.getLocalPlayer()).thenReturn(mockedPlayer);
        when(mockedPlayer.getLocalLocation()).thenReturn(localPoint);
        when(mockedClient.getTopLevelWorldView()).thenReturn(mockedWorldView);
        when(mockedClient.getWorldView(0)).thenReturn(mockedWorldView);
        when(mockedWorldView.getPlane()).thenReturn(mockedPlane);

        // Finally turn the plugin "on"
        underTest.startUp();

        // Create the elementals
        for (int i = 0; i < 3; i++)
        {
            final var ice = mock(NPC.class);
            iceElementals.add(ice);
            setNPCMock(ice, 14151);
            underTest.onNpcSpawned(new NpcSpawned(ice));
        }

        return mockedPlayer;
    }

    protected Set<NPC> iceElementals = new HashSet<NPC>();
    protected Set<NPC> fireElementals = new HashSet<NPC>();
}
