package com.attacktimer;
/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * Copyright (c) 2018, Chdata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	this list of conditions and the following disclaimer in the documentation
 *	and/or other materials provided with the distribution.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

@Singleton
class AttackTimerBarOverlay extends Overlay
{
    private static final Color BAR_FILL_COLOR = new Color(201, 161, 28);

    private static final Color BAR_BG_COLOR = Color.black;
    private static final Dimension ATTACK_BAR_SIZE = new Dimension(30, 5);

    private final Client client;
    private final AttackTimerMetronomeConfig config;
    private final AttackTimerMetronomePlugin plugin;

    private boolean shouldShowBar = false;

    @Inject
    private AttackTimerBarOverlay(final Client client, final AttackTimerMetronomeConfig config, final AttackTimerMetronomePlugin plugin)
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.UNDER_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        onTick();

        if (!shouldShowBar)
        {
            return null;
        }

        final int height = client.getLocalPlayer().getLogicalHeight() + config.heightOffset() - 20;
        final LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
        final Point canvasPoint = Perspective.localToCanvas(client, localLocation, client.getTopLevelWorldView().getPlane(), height);

        int denomMod = (config.barEmpties()) ? 1 : 0;
        int numerMod = (config.barFills()) ? 1 : 0;
        float ratio = (float) (plugin.getTicksUntilNextAttack() - numerMod) / (float) (plugin.getWeaponPeriod() - denomMod);
        if (!config.barDirection()) {
            ratio = (float)Math.max(1.0f - ratio, 0f);
        }

        // Draw bar
        final int barX = canvasPoint.getX() - 15;
        final int barY = canvasPoint.getY();
        final int barWidth = ATTACK_BAR_SIZE.width;
        final int barHeight = ATTACK_BAR_SIZE.height;

        // Restricted by the width to prevent the bar from being too long while you are boosted above your real prayer level.
        final int progressFill = (int) Math.ceil(Math.min((barWidth * ratio), barWidth));

        graphics.setColor(BAR_BG_COLOR);
        graphics.fillRect(barX, barY, barWidth, barHeight);
        graphics.setColor(BAR_FILL_COLOR);
        graphics.fillRect(barX, barY, progressFill, barHeight);

        return null;
    }
    private void onTick()
    {
        shouldShowBar = true;

        if (!config.enableMetronome()) {
            shouldShowBar = false;
        }

        if (!config.showBar()) {
            shouldShowBar = false;
        }

        if (!plugin.isAttackCooldownPending()) {
            shouldShowBar = false;
        }
    }
}