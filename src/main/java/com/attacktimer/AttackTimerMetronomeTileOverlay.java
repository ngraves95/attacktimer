package com.attacktimer;

/*
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
 * Copyright (c) 2024, Lexer747 <https://github.com/Lexer747>
 * Copyright (c) 2024, Richardant <https://github.com/Richardant>
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
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Font;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPriority;


public class AttackTimerMetronomeTileOverlay extends Overlay
{

    private final Client client;
    private final AttackTimerMetronomeConfig config;
    private final AttackTimerMetronomePlugin plugin;

    private Player player;

    @Inject
    public AttackTimerMetronomeTileOverlay(Client client, AttackTimerMetronomeConfig config, AttackTimerMetronomePlugin plugin)
    {
        super(plugin);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        player = client.getLocalPlayer();
        plugin.renderedState = plugin.attackState;
        if (plugin.attackState == AttackTimerMetronomePlugin.AttackState.NOT_ATTACKING) {
            return null;
        }

        if (config.showTick())
        {
            if (config.fontType() == FontTypes.REGULAR)
            {
                graphics.setFont(new Font(FontManager.getRunescapeFont().getName(), Font.PLAIN, config.fontSize()));
            }
            else if (config.fontType() == FontTypes.BOLD)
            {
                graphics.setFont(new Font(config.fontType().toString(), Font.BOLD, config.fontSize()));
            }
            else
            {
                graphics.setFont(new Font(config.fontType().toString(), Font.PLAIN, config.fontSize()));
            }

            // Countdown ticks instead of up.
            // plugin.tickCounter => ticksRemaining
            int ticksRemaining = plugin.getTicksUntilNextAttack();

            final LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
            Point playerPoint = null;

            switch (config.ticksPosition()) {
                case TOP:
                    playerPoint = Perspective.localToCanvas(client, localLocation, client.getTopLevelWorldView().getPlane(), 214 + config.heightTickOffset());
                    break;

                case CENTERED:
                    playerPoint = Perspective.localToCanvas(client, localLocation, client.getTopLevelWorldView().getPlane(), 100 + config.heightTickOffset());
                    break;

                case BOTTOM:
                    playerPoint = player.getCanvasTextLocation(graphics, String.valueOf(ticksRemaining), 10 + config.heightTickOffset());
                    break;

                case DEFAULT:
                    int height = client.getLocalPlayer().getLogicalHeight() + 20 + config.heightTickOffset();
                    playerPoint = Perspective.localToCanvas(client, localLocation, client.getTopLevelWorldView().getPlane(), height);
                    break;
            }
            if (playerPoint != null) {
                OverlayUtil.renderTextLocation(graphics, playerPoint, String.valueOf(ticksRemaining), ticksRemaining == 1 ? config.LastColor() : config.NumberColor());
            }
        }
        return null;
    }

    private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final Color fillColor, final double borderWidth)
    {
        if (dest == null)
        {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
    }
}
