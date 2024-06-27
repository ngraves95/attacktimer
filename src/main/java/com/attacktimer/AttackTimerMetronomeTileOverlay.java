package net.runelite.client.plugins.attacktimer;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.GameTickInfo.GameTickInfoPlugin;
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
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;


public class AttackTimerMetronomeTileOverlay extends OverlayPanel
{

    private final Client client;
    private final AttackTimerMetronomeConfig config;
    private final AttackTimerMetronomePlugin plugin;
    private final PanelComponent AttackTimerMetronomeTileOverlay = new PanelComponent();

    @Inject
    public AttackTimerMetronomeTileOverlay(Client client, AttackTimerMetronomeConfig config, AttackTimerMetronomePlugin plugin)
    {
        super(plugin);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.MED);
        isResizable();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        int ticksRemaining = plugin.getTicksUntilNextAttack();
        if (plugin.attackState != AttackTimerMetronomePlugin.AttackState.DELAYED) {
            return null;
        }

        if (config.showTick())
        {
            if (config.fontType() == FontTypes.REGULAR)
            {
                graphics.setFont(new Font(FontManager.getRunescapeFont().getName(), Font.PLAIN, config.fontSize()));
            }
            else
            {
                graphics.setFont(new Font(config.fontType().toString(), Font.PLAIN, config.fontSize()));
            }

            String timeToDisplay = String.valueOf(ticksRemaining);

            AttackTimerMetronomeTileOverlay.getChildren().clear();
            AttackTimerMetronomeTileOverlay.getChildren().add(TitleComponent.builder()
                            .text(timeToDisplay)
                            .color(config.NumberColor())
                            .build());


         //   final int height = client.getLocalPlayer().getLogicalHeight()+20;
        //    final LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
         //   final Point playerPoint = Perspective.localToCanvas(client, localLocation, client.getPlane(), height);

            // Countdown ticks instead of up.
            // plugin.tickCounter => ticksRemaining
            AttackTimerMetronomeTileOverlay.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(String.valueOf(GameTickInfoPlugin.timeSinceCycleStart))+10,0));
           //int ticksRemaining = plugin.getTicksUntilNextAttack();
           // OverlayUtil.renderTextLocation(graphics, playerPoint, String.valueOf(ticksRemaining), config.NumberColor());
        }
        return AttackTimerMetronomeTileOverlay.render(graphics);
        //return null;
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


