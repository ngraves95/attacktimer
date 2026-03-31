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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.attacktimer.AttackTimerMetronomePlugin.AttackState;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.overlay.OverlayManager;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationTests
{
    @Mock
    @Bind
    protected OverlayManager mockedOverlayManager;

    @Mock
    @Bind
    protected ConfigManager mockedConfigManager;

    @Mock
    @Bind
    protected AttackTimerMetronomeTileOverlay mockedOverlay;

    @Mock
    @Bind
    protected AttackTimerBarOverlay mockedBarOverlay;

    @Mock
    @Bind
    protected AttackTimerMetronomeConfig mockedConfig;

    @Mock
    @Bind
    protected ItemManager mockedItemManager;

    @Mock
    @Bind
    protected Client mockedClient;

    @Mock
    @Bind
    protected NPCManager mockedNpcManager;

    @Inject
    protected AttackTimerMetronomePlugin underTest;

    @Before
    public void setup()
    {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
    }

    public Player pluginMockSetup() throws Exception
    {
        // enable the plugin
        when(mockedConfig.enableMetronome()).thenReturn(true);
        // Create player
        Player mockedPlayer = mock(Player.class);

        // Create the enemy
        NPC mockedTarget = mock(NPC.class);
        NPCComposition mockedCompositions = mock(NPCComposition.class);
        when(mockedTarget.getComposition()).thenReturn(mockedCompositions);
        int mockedNpcId = 0xFFFF;
        when(mockedTarget.getId()).thenReturn(mockedNpcId);
        String[] actions = {
                "Attack", "Examine"
        };
        when(mockedCompositions.getActions()).thenReturn(actions);
        when(mockedNpcManager.getHealth(mockedNpcId)).thenReturn(1);

        // set the player as "attacking" the NPC
        when(mockedClient.getLocalPlayer()).thenReturn(mockedPlayer);
        when(mockedPlayer.getInteracting()).thenReturn(mockedTarget);

        // need some extra mocks to stop the plugin running into an exception on the
        // client APIs
        // -- Mock World
        WorldView mockedWorldView = mock(WorldView.class);
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
        IndexedObjectSet mockedNpcs = mock(IndexedObjectSet.class);
        when(mockedNpcs.iterator()).thenReturn(Collections.emptyIterator());
        when(mockedWorldView.npcs()).thenReturn(mockedNpcs);
        // -- Attack Styles
        EnumComposition mockedWeaponEnum = mock(EnumComposition.class);
        when(mockedClient.getEnum(EnumID.WEAPON_STYLES)).thenReturn(mockedWeaponEnum);
        when(mockedWeaponEnum.getIntValue(0)).thenReturn(-1); // blue-moon-spear mock

        // Finally turn the plugin "on"
        underTest.startUp();
        return mockedPlayer;
    }

    @Test
    public void basicTest() throws Exception
    {
        ByteArrayDataOutput channel = ByteStreams.newDataOutput();
        underTest.writeState(channel);
        // Trivial Pre-conditions:
        assertSame(AttackState.NOT_ATTACKING, underTest.attackState);

        // Basic test case:
        // 1. Start by setting up the player
        // 2. Mock an attack animation
        // 3. Check that the plugin has registered the attack
        // 4. Check that the plugin counts down correctly
        // 5. Check that the plugin is back to a waiting state and it still counts down

        writeTestMessage("1. Start by setting up the player and plugin", channel);
        int atkSpeed = 3; // no weapon equipped (4 ticks, plugin starts counting from 3)
        int tick = 0;
        Player mockedPlayer = pluginMockSetup();
        when(mockedClient.getTickCount()).thenReturn(tick);

        writeTestMessage("2. Mock an attack animation", channel);

        // set the animation to an attack
        when(mockedPlayer.getAnimation()).thenReturn(AnimationData.MELEE_GENERIC_SLASH.animationId);
        // tell the plugin that a tick occurred
        onGameTick(channel);

        writeTestMessage("3. Check that the plugin has registered the attack", channel);
        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        assertSame(atkSpeed, underTest.attackDelayHoldoffTicks);

        // clear the animation
        when(mockedPlayer.getAnimation()).thenReturn(noAnimation);

        writeTestMessage("4. Check that the plugin counts down correctly", channel);
        while (atkSpeed > 0)
        {
            tick++;
            atkSpeed--;
            when(mockedClient.getTickCount()).thenReturn(tick);
            onGameTick(channel);
            assertSame(AttackState.DELAYED, underTest.attackState);
            assertSame(atkSpeed, underTest.attackDelayHoldoffTicks);
        }

        writeTestMessage("5. Check that the plugin is back to a waiting state and it still counts down", channel);
        tick++;
        onGameTick(channel);
        assertSame(AttackState.NOT_ATTACKING, underTest.attackState);
        while (tick < 30)
        {
            tick++;
            when(mockedClient.getTickCount()).thenReturn(tick);
            onGameTick(channel);
            assertSame(AttackState.NOT_ATTACKING, underTest.attackState);
            assertTrue(underTest.attackDelayHoldoffTicks < 0); // hold off should go negative
        }

        performStateVerificationOrUpdate(channel, Paths.get(testdata + "basicTest.txt"));
    }

    @Test
    public void eatingFoodTest() throws Exception
    {
        ByteArrayDataOutput channel = ByteStreams.newDataOutput();
        underTest.writeState(channel);
        // Trivial Pre-conditions:
        assertSame(AttackState.NOT_ATTACKING, underTest.attackState);
        writeTestMessage("1. Start by setting up the player and plugin", channel);
        int atkSpeed = 3; // no weapon equipped (4 ticks, plugin starts counting from 3)
        int tick = 0;
        Player mockedPlayer = pluginMockSetup();
        when(mockedClient.getTickCount()).thenReturn(tick);

        writeTestMessage("2. Mock an attack animation", channel);

        // set the animation to an attack
        when(mockedPlayer.getAnimation()).thenReturn(AnimationData.MELEE_GENERIC_SLASH.animationId);
        // tell the plugin that a tick occurred
        onGameTick(channel);

        writeTestMessage("3. Check that the plugin has registered the attack", channel);
        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        assertSame(atkSpeed, underTest.attackDelayHoldoffTicks);

        when(mockedPlayer.getAnimation()).thenReturn(noAnimation);
        underTest.writeState(channel);

        writeTestMessage("Perform an eat", channel);
        ChatMessage chatMessage = new ChatMessage(null, ChatMessageType.SPAM, "", "You eat the shark.", "", 0);
        underTest.onChatMessage(chatMessage);
        // the plugin wont apply the delay directly it waits until the next game tick
        assertSame(3, underTest.pendingEatDelayTicks);
        // Should still be same state
        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        underTest.writeState(channel);

        // Subsequent values should be offset by the eat delay
        writeTestMessage("Next game tick", channel);
        atkSpeed += 2;
        onGameTick(channel);
        assertSame(AttackState.DELAYED, underTest.attackState);
        assertSame(atkSpeed, underTest.attackDelayHoldoffTicks);

        // Note that this is essentially a "double" eat test.
        writeTestMessage("Perform a fast eat", channel);
        chatMessage = new ChatMessage(null, ChatMessageType.SPAM, "", "You eat the halibut.", "", 0);
        underTest.onChatMessage(chatMessage);
        // the plugin wont apply the delay directly it waits until the next game tick
        assertSame(2, underTest.pendingEatDelayTicks);
        // Should still be same state
        assertSame(AttackState.DELAYED, underTest.attackState);
        underTest.writeState(channel);

        // Subsequent values should be offset by the eat delay
        writeTestMessage("Next game tick", channel);
        atkSpeed += 1;
        onGameTick(channel);
        assertSame(AttackState.DELAYED, underTest.attackState);
        assertSame(atkSpeed, underTest.attackDelayHoldoffTicks);

        writeTestMessage("4. Check that the plugin counts down correctly", channel);
        while (atkSpeed > 0)
        {
            tick++;
            atkSpeed--;
            when(mockedClient.getTickCount()).thenReturn(tick);
            onGameTick(channel);
            assertSame(AttackState.DELAYED, underTest.attackState);
            assertSame(atkSpeed, underTest.attackDelayHoldoffTicks);
        }

        performStateVerificationOrUpdate(channel, Paths.get(testdata + "eatingFoodTest.txt"));
    }

    protected void performStateVerificationOrUpdate(ByteArrayDataOutput channel, Path path) throws IOException
    {
        var actualBytes = channel.toByteArray();
        switch (Update.system())
        {
        case NONE:
            // We verify that the file on disk/repo are the same as the one generated
            var expectedBytes = Files.readAllBytes(path);
            assertArrayEquals(expectedBytes, actualBytes);
            return;
        case ALL:
            // Write the generated one to disk
            try (var file = Files.newByteChannel(path, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE)))
            {
                file.write(ByteBuffer.wrap(actualBytes));
                file.close();
            }
            return;
        default:
            fail("Unexpected Update enum");
        }
    }

    protected void onGameTick(ByteArrayDataOutput file)
    {
        underTest.onGameTick(new GameTick());
        underTest.writeState(file);
    }

    protected void writeTestMessage(String message, ByteArrayDataOutput file)
    {
        file.write(testMessagePrefix);
        file.write(message.getBytes(StandardCharsets.UTF_8));
        file.write(testMessageSuffix);
    }

    private static final int noAnimation = -1;
    private static final String testdata = "src/test/java/com/attacktimer/testdata/";

    private static final byte[] testMessagePrefix = "[TEST MESSAGE] ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] testMessageSuffix = "\n".getBytes(StandardCharsets.UTF_8);
}