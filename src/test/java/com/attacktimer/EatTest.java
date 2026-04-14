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
import static org.mockito.Mockito.when;

import com.attacktimer.AttackTimerMetronomePlugin.AttackState;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import org.junit.Test;

public class EatTest extends IntegrationTests
{
    @Test
    public void ExhaustiveEatTest() throws Exception
    {
        Player mockedPlayer = pluginMockSetup();
        when(mockedClient.getTickCount()).thenReturn(0);
        when(mockedPlayer.getAnimation()).thenReturn(AnimationData.MELEE_GENERIC_SLASH.animationId);
        underTest.onGameTick(new GameTick());
        assertSame(AttackState.DELAYED_FIRST_TICK, underTest.attackState);
        assertSame(3, underTest.attackDelayHoldoffTicks);
        String[] fastFoodsToTest = {
                // NOTE case sensitivity here is import this should reflect in-game so that the plugin is resilient
                // to mixed casing
                "Karambwan", "halibut", "choc chip crunchies",
        };
        String[] foodsToTest = {
                "shark", "meat pizza", "A brand new food message", "sunlight antelope", "moonlight antelope",
                "purple sweets",
        };
        var curEatDelayTicks = underTest.pendingEatDelayTicks;
        for (String food : foodsToTest)
        {
            ChatMessage chatMessage = new ChatMessage(null, ChatMessageType.SPAM, "", "You eat the " + food + ".", "",
                    0);
            underTest.onChatMessage(chatMessage);
            curEatDelayTicks += 3;
            assertSame(curEatDelayTicks, underTest.pendingEatDelayTicks);
        }

        for (String food : fastFoodsToTest)
        {
            ChatMessage chatMessage = new ChatMessage(null, ChatMessageType.SPAM, "", "You eat the " + food + ".", "",
                    0);
            underTest.onChatMessage(chatMessage);
            curEatDelayTicks += 2;
            assertSame(curEatDelayTicks, underTest.pendingEatDelayTicks);
        }
    }
}
