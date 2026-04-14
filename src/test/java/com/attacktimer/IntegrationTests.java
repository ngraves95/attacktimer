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
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.io.ByteArrayDataOutput;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;

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
        when(mockedPlayer.getAnimation()).thenReturn(NO_ANIMATION);

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
            fail("Updated file: " + path);
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
        file.write(PREFIX);
        file.write(message.getBytes(StandardCharsets.UTF_8));
        file.write(SUFFIX);
    }

    protected static final int NO_ANIMATION = -1;
    protected static final String TESTDATA = "src/test/java/com/attacktimer/testdata/";

    private static final byte[] PREFIX = "[TEST MESSAGE] ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SUFFIX = "\n".getBytes(StandardCharsets.UTF_8);

    // This needs at least one public test to keep mockito happy but having real tests in this file would
    // result in any future test which extends this test class also having to run and make that test pass.
    //
    // This does cause test inflation in that the summary will make it look like we have more tests than we
    // really do, but I don't have a nicer way to not repeat all the injector boiler plate.
    @Test
    public void noTest()
    {}
}