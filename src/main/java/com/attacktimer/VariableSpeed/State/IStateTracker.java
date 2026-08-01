package com.attacktimer.VariableSpeed.State;

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

import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

public interface IStateTracker
{
    /**
     * onGameTick is a subscription method, an implementation can implement this if the condition for
     * the variable speed requires some larger state tracking and cannot be implemented in apply alone.
     *
     * @param client the RuneScape client.
     * @param tick   the current tick.
     */
    default public void onGameTick(final Client client, final GameTick tick)
    {};

    /**
     * onChatMessage is a subscription method, an implementation can implement this if the condition for
     * the variable speed requires some larger state tracking and cannot be implemented in apply alone.
     *
     * @param client the RuneScape client.
     * @param event  the chat message event.
     */
    default public void onChatMessage(final Client client, final ChatMessage event)
    {};

    /**
     * TODO
     *
     * @param npcSpawned
     */
    default public void onNpcSpawned(final Client client, final NpcSpawned npcSpawned)
    {};

    /**
     * TODO
     *
     * @param npcDespawned
     */
    default public void onNpcDespawned(final Client client, final NpcDespawned npcDespawned)
    {};
}
