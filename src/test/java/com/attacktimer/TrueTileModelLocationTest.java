package com.attacktimer;

/*
 * Copyright (c) 2026, jordenjacobsen-ai <https://github.com/jordenjacobsen-ai>
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

public class TrueTileModelLocationTest extends IntegrationTests
{
    private static final int WORLD_VIEW = -1;
    private static final LocalPoint PLAYER_LOCATION = new LocalPoint(6464, 6464, WORLD_VIEW);
    private static final LocalPoint MODEL_LOCATION = new LocalPoint(6500, 6600, WORLD_VIEW);

    private static PluginMessage modelLocation(boolean active, LocalPoint location)
    {
        final Map<String, Object> data = new HashMap<>();
        data.put("active", active);
        if (location != null)
        {
            data.put("x", location.getX());
            data.put("y", location.getY());
            data.put("worldView", location.getWorldView());
        }
        return new PluginMessage("true-tile-movement-animation", "local-player-model-location", data);
    }

    private void setTick(int tick)
    {
        lenient().when(mockedClient.getTickCount()).thenReturn(tick);
    }

    @Test
    public void overlaysFollowTheTrueTileModelOnlyWhileItIsPublished()
    {
        final Player player = mock(Player.class);
        lenient().when(mockedClient.getLocalPlayer()).thenReturn(player);
        lenient().when(player.getLocalLocation()).thenReturn(PLAYER_LOCATION);
        setTick(100);

        // Without True Tile Movement Animations nothing changes
        assertNull(underTest.getTrueTileModelLocation());
        assertEquals(PLAYER_LOCATION, underTest.getLocalPlayerOverlayLocation());

        // Messages from other plugins are ignored
        underTest.onPluginMessage(new PluginMessage("some-other-plugin", "local-player-model-location", modelLocation(true, MODEL_LOCATION).getData()));
        assertNull(underTest.getTrueTileModelLocation());

        // While the model is published the overlays follow it
        underTest.onPluginMessage(modelLocation(true, MODEL_LOCATION));
        assertEquals(MODEL_LOCATION, underTest.getTrueTileModelLocation());
        assertEquals(MODEL_LOCATION, underTest.getLocalPlayerOverlayLocation());
        setTick(102);
        assertEquals(MODEL_LOCATION, underTest.getLocalPlayerOverlayLocation());

        // It is re-sent every game tick, so an old location means the other plugin stopped
        setTick(103);
        assertNull(underTest.getTrueTileModelLocation());
        assertEquals(PLAYER_LOCATION, underTest.getLocalPlayerOverlayLocation());

        // Going back to the real player is announced explicitly
        underTest.onPluginMessage(modelLocation(true, MODEL_LOCATION));
        assertEquals(MODEL_LOCATION, underTest.getLocalPlayerOverlayLocation());
        underTest.onPluginMessage(modelLocation(false, null));
        assertEquals(PLAYER_LOCATION, underTest.getLocalPlayerOverlayLocation());

        // A model in another world view than the player is not used
        underTest.onPluginMessage(modelLocation(true, new LocalPoint(6500, 6600, 3)));
        assertEquals(PLAYER_LOCATION, underTest.getLocalPlayerOverlayLocation());
    }
}
