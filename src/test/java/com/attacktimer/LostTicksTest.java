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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;
import org.junit.Test;

public class LostTicksTest extends IntegrationTests
{
    private AttackTimerMetronomeTileOverlay textOverlay;
    private ByteArrayDataOutput channel;
    private Player player;
    private int tick;

    private void setupShadow() throws Exception
    {
        channel = ByteStreams.newDataOutput();
        player = pluginMockSetup();
        when(mockedConfig.showLostTicks()).thenReturn(true);
        when(mockedConfig.maximumLostTicks()).thenReturn(2);
        lenient().when(mockedConfig.fontType()).thenReturn(FontTypes.REGULAR);
        lenient().when(mockedConfig.fontSize()).thenReturn(15);
        lenient().when(mockedConfig.ticksPosition()).thenReturn(AttackTimerMetronomeConfig.TicksPosition.BOTTOM);
        lenient().when(mockedConfig.NumberColor()).thenReturn(Color.CYAN);
        lenient().when(mockedConfig.LastColor()).thenReturn(Color.GREEN);
        lenient().when(mockedConfig.lostTickColor()).thenReturn(Color.RED);
        lenient().when(player.getCanvasTextLocation(any(Graphics2D.class), anyString(), anyInt())).thenReturn(new Point(50, 50));
        ItemContainer equipment = mock(ItemContainer.class);
        lenient().when(mockedClient.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
        lenient().when(equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx())).thenReturn(new Item(ItemID.TUMEKENS_SHADOW, 1));
        lenient().when(mockedItemManager.getItemStats(ItemID.TUMEKENS_SHADOW)).thenReturn(
                new ItemStats(true, 0, 0, ItemEquipmentStats.builder().aspeed(5).build()));
        textOverlay = new AttackTimerMetronomeTileOverlay(mockedClient, mockedConfig, underTest);
        underTest.writeState(channel);
    }

    private void nextTick()
    {
        when(mockedClient.getTickCount()).thenReturn(tick++);
        onGameTick(channel);
    }

    private void attack()
    {
        when(player.getAnimation()).thenReturn(AnimationData.MAGIC_TUMEKENS_SHADOW.animationId);
        nextTick();
        when(player.getAnimation()).thenReturn(NO_ANIMATION);
    }

    private void assertRenderedTick(String expected)
    {
        clearInvocations(player);
        Graphics2D graphics = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            textOverlay.render(graphics);
            if (expected == null)
            {
                verify(player, never()).getCanvasTextLocation(any(Graphics2D.class), anyString(), anyInt());
            }
            else
            {
                verify(player).getCanvasTextLocation(graphics, expected, 10);
            }
            writeTestMessage("Overhead: " + (expected == null ? "hidden" : expected), channel);
        }
        finally
        {
            graphics.dispose();
        }
    }

    private void ready()
    {
        attack();
        for (int i = 0; i < 4; i++)
        {
            nextTick();
            assertRenderedTick(null);
        }
    }

    @Test
    public void shadowShowsOnlyTwoLostTicksWithCooldownOff() throws Exception
    {
        setupShadow();
        assertRenderedTick(null);
        ready();
        nextTick();
        assertRenderedTick("-1");
        nextTick();
        assertRenderedTick("-2");
        for (int i = 0; i < 30; i++)
        {
            nextTick();
            assertRenderedTick(null);
        }
        ready();
        nextTick();
        assertRenderedTick("-1");
        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + "LostTicksTest.txt"));
    }

    @Test
    public void normalCooldownDisplayIsUnchanged() throws Exception
    {
        setupShadow();
        when(mockedConfig.showLostTicks()).thenReturn(false);
        when(mockedConfig.showTick()).thenReturn(true);
        attack();
        assertRenderedTick("5");
        when(mockedConfig.useZeroBasedTickCount()).thenReturn(true);
        assertRenderedTick("4");
        when(mockedConfig.showTick()).thenReturn(false);
        assertRenderedTick(null);
        performStateVerificationOrUpdate(channel, Paths.get(TESTDATA + "LostTicksNormalCooldownTest.txt"));
    }
}
