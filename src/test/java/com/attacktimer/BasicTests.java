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

import static org.mockito.Mockito.when;

import java.nio.file.Paths;

import static com.attacktimer.IntegrationTests.NO_ANIMATION;
import static com.attacktimer.IntegrationTests.TESTDATA;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.attacktimer.AttackTimerMetronomePlugin.AttackState;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;

public class BasicTests extends IntegrationTests
{
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
        when(mockedPlayer.getAnimation()).thenReturn(NO_ANIMATION);

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

        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + "basicTest.txt"));
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

        when(mockedPlayer.getAnimation()).thenReturn(NO_ANIMATION);
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

        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + "eatingFoodTest.txt"));
    }
}
