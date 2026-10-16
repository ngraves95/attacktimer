package com.attacktimer;

/*
 * Copyright (c) 2026, Jalawoud <https://github.com/Jalawoud>
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.ByteStreams;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.ChatMessageType;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;
import org.junit.Test;

public class LostTicksTest extends IntegrationTests
{
    private AttackTimerMetronomeTileOverlay textOverlay;
    private Player player;
    private int tick;

    private void setupShadow() throws Exception
    {
        player = pluginMockSetup();
        when(mockedConfig.showLostTicks()).thenReturn(true);
        when(mockedConfig.maximumLostTicks()).thenReturn(2);
        lenient().when(mockedClient.getGameState()).thenReturn(GameState.LOGGED_IN);
        lenient().when(player.getInteracting().getHealthRatio()).thenReturn(-1);
        ItemContainer equipment = mock(ItemContainer.class);
        lenient().when(mockedClient.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
        lenient().when(equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx())).thenReturn(new Item(ItemID.TUMEKENS_SHADOW, 1));
        lenient().when(mockedItemManager.getItemStats(ItemID.TUMEKENS_SHADOW)).thenReturn(
                new ItemStats(true, 0, 0, ItemEquipmentStats.builder().aspeed(5).build()));
        textOverlay = new AttackTimerMetronomeTileOverlay(mockedClient, mockedConfig, underTest);
    }

    private void nextTick()
    {
        when(mockedClient.getTickCount()).thenReturn(tick++);
        underTest.onGameTick(new GameTick());
        underTest.writeState(ByteStreams.newDataOutput());
    }

    private void attack()
    {
        when(player.getAnimation()).thenReturn(AnimationData.MAGIC_TUMEKENS_SHADOW.animationId);
        nextTick();
        when(player.getAnimation()).thenReturn(NO_ANIMATION);
    }

    private void ready()
    {
        attack();
        for (int i = 0; i < 4; i++)
        {
            nextTick();
            assertNull(textOverlay.getTickText());
        }
    }

    @Test
    public void shadowShowsOnlyTwoLostTicksWithCooldownOff() throws Exception
    {
        setupShadow();
        assertNull(textOverlay.getTickText());
        ready();
        nextTick();
        assertEquals("-1", textOverlay.getTickText());
        nextTick();
        assertEquals("-2", textOverlay.getTickText());
        for (int i = 0; i < 30; i++)
        {
            nextTick();
            assertNull(textOverlay.getTickText());
        }
        ready();
        nextTick();
        assertEquals("-1", textOverlay.getTickText());
    }

    @Test
    public void onTimeAttackNeverShowsLostTicks() throws Exception
    {
        setupShadow();
        ready();
        attack();
        assertNull(textOverlay.getTickText());
        assertEquals(0, underTest.getLostAttackTicks());
    }

    @Test
    public void resumedAttackClearsLostTicksImmediately() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        assertEquals("-1", textOverlay.getTickText());
        attack();
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void idleWithoutAnAttackDoesNotShowLostTicks() throws Exception
    {
        setupShadow();
        for (int i = 0; i < 30; i++)
        {
            nextTick();
            assertEquals(0, underTest.getLostAttackTicks());
            assertNull(textOverlay.getTickText());
        }
    }

    @Test
    public void maximumTwentyExpiresDespiteInternalCounterClamp() throws Exception
    {
        setupShadow();
        when(mockedConfig.maximumLostTicks()).thenReturn(20);
        ready();
        for (int i = 1; i <= 20; i++)
        {
            nextTick();
            assertEquals("-" + i, textOverlay.getTickText());
        }
        for (int i = 0; i < 5; i++)
        {
            nextTick();
            assertNull(textOverlay.getTickText());
        }
    }

    @Test
    public void maximumOneHidesOnSecondLostTick() throws Exception
    {
        setupShadow();
        when(mockedConfig.maximumLostTicks()).thenReturn(1);
        ready();
        nextTick();
        assertEquals("-1", textOverlay.getTickText());
        nextTick();
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void eatingExtendsCooldownRatherThanShowingLostTicks() throws Exception
    {
        setupShadow();
        ready();
        underTest.onChatMessage(new ChatMessage(null, ChatMessageType.SPAM, "", "You eat the shark.", "", 0));
        for (int i = 0; i < 3; i++)
        {
            nextTick();
            assertNull(textOverlay.getTickText());
        }
        nextTick();
        assertEquals("-1", textOverlay.getTickText());
    }

    @Test
    public void targetDeathClearsLostTicks() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        underTest.onActorDeath(new ActorDeath(player.getInteracting()));
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void playerDeathClearsLostTicks() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        underTest.onActorDeath(new ActorDeath(player));
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void targetDespawnClearsLostTicks() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        underTest.onNpcDespawned(new NpcDespawned((NPC) player.getInteracting()));
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void loadingClearsLostTicks() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        GameStateChanged event = new GameStateChanged();
        event.setGameState(GameState.LOADING);
        underTest.onGameStateChanged(event);
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void configurationChangeDoesNotStartAnIdleCounter() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        ConfigChanged event = new ConfigChanged();
        event.setGroup("attacktimermetronome");
        underTest.onConfigChanged(event);
        nextTick();
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void shutdownClearsLostTicks() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        underTest.shutDown();
        assertNull(textOverlay.getTickText());
    }

    @Test
    public void combinedDisplayCountsDownThenShowsBoundedNegativeTicks() throws Exception
    {
        setupShadow();
        when(mockedConfig.showTick()).thenReturn(true);
        attack();
        for (int remaining = 5; remaining >= 1; remaining--)
        {
            assertEquals(String.valueOf(remaining), textOverlay.getTickText());
            nextTick();
        }
        assertEquals("-1", textOverlay.getTickText());
        nextTick();
        assertEquals("-2", textOverlay.getTickText());
        for (int i = 0; i < 30; i++)
        {
            nextTick();
            assertNull(textOverlay.getTickText());
        }
        attack();
        assertEquals("5", textOverlay.getTickText());
    }

    @Test
    public void onTimeAttackRestartsCombinedCountdownWithoutNegativeTicks() throws Exception
    {
        setupShadow();
        when(mockedConfig.showTick()).thenReturn(true);
        for (int attacks = 0; attacks < 3; attacks++)
        {
            attack();
            assertEquals("5", textOverlay.getTickText());
            for (int i = 0; i < 4; i++)
            {
                nextTick();
                assertEquals(String.valueOf(4 - i), textOverlay.getTickText());
            }
        }
    }

    @Test
    public void zeroBasedCountdownDoesNotShiftLostTicks() throws Exception
    {
        setupShadow();
        when(mockedConfig.showTick()).thenReturn(true);
        when(mockedConfig.useZeroBasedTickCount()).thenReturn(true);
        attack();
        for (int remaining = 4; remaining >= 0; remaining--)
        {
            assertEquals(String.valueOf(remaining), textOverlay.getTickText());
            nextTick();
        }
        assertEquals("-1", textOverlay.getTickText());
    }

    @Test
    public void combinedDisplayUsesSeparateCooldownAndLostColors() throws Exception
    {
        setupShadow();
        when(mockedConfig.showTick()).thenReturn(true);
        when(mockedConfig.NumberColor()).thenReturn(Color.CYAN);
        when(mockedConfig.LastColor()).thenReturn(Color.GREEN);
        when(mockedConfig.lostTickColor()).thenReturn(Color.RED);
        attack();
        assertEquals(Color.CYAN, textOverlay.getTickColor());
        for (int i = 0; i < 4; i++)
        {
            nextTick();
        }
        assertEquals("1", textOverlay.getTickText());
        assertEquals(Color.GREEN, textOverlay.getTickColor());
        nextTick();
        assertEquals("-1", textOverlay.getTickText());
        assertEquals(Color.RED, textOverlay.getTickColor());
        attack();
        assertEquals("5", textOverlay.getTickText());
        assertEquals(Color.CYAN, textOverlay.getTickColor());
    }

    @Test
    public void bothDisplaysOffHidesCooldownAndLostTicks() throws Exception
    {
        setupShadow();
        when(mockedConfig.showLostTicks()).thenReturn(false);
        ready();
        for (int i = 0; i < 5; i++)
        {
            nextTick();
            assertNull(textOverlay.getTickText());
        }
    }

    @Test
    public void lostTicksRenderWhenCooldownSettingIsOff() throws Exception
    {
        setupShadow();
        ready();
        nextTick();
        when(mockedConfig.fontType()).thenReturn(FontTypes.REGULAR);
        when(mockedConfig.fontSize()).thenReturn(15);
        when(mockedConfig.ticksPosition()).thenReturn(AttackTimerMetronomeConfig.TicksPosition.BOTTOM);
        when(mockedConfig.lostTickColor()).thenReturn(Color.RED);
        when(player.getCanvasTextLocation(any(Graphics2D.class), eq("-1"), anyInt())).thenReturn(new Point(50, 50));
        Graphics2D graphics = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            textOverlay.render(graphics);
            verify(player).getCanvasTextLocation(graphics, "-1", 10);
        }
        finally
        {
            graphics.dispose();
        }
    }

    @Test
    public void normalCooldownDisplayIsUnchanged() throws Exception
    {
        setupShadow();
        when(mockedConfig.showLostTicks()).thenReturn(false);
        when(mockedConfig.showTick()).thenReturn(true);
        attack();
        assertEquals("5", textOverlay.getTickText());
        when(mockedConfig.useZeroBasedTickCount()).thenReturn(true);
        assertEquals("4", textOverlay.getTickText());
        when(mockedConfig.showTick()).thenReturn(false);
        assertNull(textOverlay.getTickText());
    }
}
