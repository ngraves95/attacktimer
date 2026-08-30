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

import com.attacktimer.ClientUtils.Utils;
import java.time.Duration;
import java.time.Instant;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.RSTimeUnit;

/**
 * MarkOfDarkness tracks when mark of darkness was cast and how long it will last, it is queryable
 * via `isActive`.
 */
public class MarkOfDarkness implements IStateTracker
{
    private static final String MARK_OF_DARKNESS_MESSAGE = "You have placed a Mark of Darkness upon yourself.</col>";

    private Instant modStartTime = Instant.now();
    private Instant modEndTime = Instant.now();

    public void onChatMessage(Client client, ChatMessage event)
    {
        final String message = event.getMessage();
        if (message.endsWith(MARK_OF_DARKNESS_MESSAGE))
        {
            final Duration duration = getMarkOfDarknessDuration(client);
            this.modStartTime = Instant.now();
            this.modEndTime = modStartTime.plus(duration);
        }
    }

    /**
     * returns true if and only if mark of darkness is currently active on the player. The only edge
     * case is if the plugin was disabled whilst mark of darkness is active.
     */
    public boolean isActive()
    {
        final var n = Instant.now();
        return this.modStartTime.isBefore(n) && this.modEndTime.isAfter(n);
    }

    // Taken from the timers and buffs plugins (as of commit:
    // https://github.com/runelite/runelite/commit/9a6f7017ed9c9b8ea2687b0d84ce79a28434cefd) same
    // license as this file.
    //
    // # Credits:
    //
    // https://github.com/runelite/runelite/commits?author=Psychemaster
    // https://github.com/runelite/runelite/commits?author=YvesW
    // https://github.com/runelite/runelite/commits?author=Nightfirecat
    private static Duration getMarkOfDarknessDuration(Client client)
    {
        final int magicLevel = client.getRealSkillLevel(Skill.MAGIC);
        final Duration markOfDarknessDuration = Duration.of((long) magicLevel * 3, RSTimeUnit.GAME_TICKS);

        if (Utils.getWeaponId(client) == ItemID.PURGING_STAFF)
        {
            return markOfDarknessDuration.multipliedBy(5);
        }
        return markOfDarknessDuration;
    }
}
