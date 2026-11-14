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
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;

public class Teleported implements IStateTracker
{
    private final TickCount tc;
    private WorldPoint lastWorldPoint = null;
    private boolean teleported = false;
    private int tpTick = -1;

    public Teleported(final TickCount tc)
    {
        this.tc = tc;
    }

    public void onGameTick(final Client client, final GameTick tick)
    {
        // Track player location to detect teleports
        final var lp = client.getLocalPlayer();
        if (lp == null) return;
        final var pos = lp.getWorldLocation();
        if (pos == null) return;
        if (lastWorldPoint == null)
        {
            lastWorldPoint = pos;
            return;
        }
        // this will be too naive for some bosses
        teleported = lastWorldPoint.distanceTo2D(pos) > 2;
        if (teleported)
        {
            tpTick = tc.get();
        }
        lastWorldPoint = pos;
    }

    /**
     * HasTeleported returns true if the player has teleported this tick.
     * @return false if the player hasn't teleported, true if the player has
     */
    public boolean HasTeleported()
    {
        return teleported;
    }

    /**
     * TeleportedInWindow returns true if the player has teleported within the given tick window
     * @param start the starting tick to check
     * @param stop the final tick to check
     * @return true if the player teleported within the window, false otherwise.
     */
    public boolean TeleportedInWindow(final int start, final int stop)
    {
        return tpTick >= start && tpTick <= stop;
    }
};
