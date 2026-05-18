package com.attacktimer.VariableSpeed;

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

import com.attacktimer.VariableSpeed.State.MarkOfDarkness;
import com.attacktimer.VariableSpeed.State.TickCount;
import com.attacktimer.VariableSpeed.State.Yama;
import com.attacktimer.VariableSpeed.State.YamaPhase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class ShadowCrash
{
    private static final int FIREBALL_ID = 3262;
    // The unit of a LocalPoint is 1/128th of a tile
    private static final int TILE = 128;

    // Each fireball is 3 tiles apart when vertical or horizontal, diagonal are shorter. See
    // shadowCrash.png.
    private static final int HV_GAP_1 = 3;
    private static final int HV_GAP_2 = 6;
    private static final int HV_DELTA_1 = HV_GAP_1 * TILE;
    private static final int HV_DELTA_2 = HV_GAP_2 * TILE;
    private static final int D_GAP_1 = 2;
    private static final int D_GAP_2 = 4;
    private static final int D_DELTA_1 = D_GAP_1 * TILE;
    private static final int D_DELTA_2 = D_GAP_2 * TILE;

    // externally tracked state
    private TickCount tickCount;
    private Yama yama;
    private MarkOfDarkness mod;

    // local state

    // when was the speed up consumed in ticks
    private int consumed = -1;
    private Map<LocalPoint, GraphicsObject> fireballs;
    private ArrayList<FireballLine> lines;
    private WorldPoint player;

    ShadowCrash(Yama yama, MarkOfDarkness mod, TickCount tc)
    {
        this.yama = yama;
        this.mod = mod;
        this.tickCount = tc;
        this.fireballs = new HashMap<>();
        this.lines = new ArrayList<>();
    }

    // https://oldschool.runescape.wiki/w/Yama/Strategies#Phase_3
    //
    // If the player only moves one tile away from the crash and has Mark of Darkness active, their next
    // attack will be used one tick faster than usual. Each crash is individual to the player themselves and
    // they will not take damage from another player's crash.
    //
    // This function will adjust the attackDelayHoldoffTicks if the player successfully dodges the shadowcrash
    // in the sweet spot.
    public int onRender(final Client client, final int attackDelayHoldoffTicks, final boolean isUsingMagic, final boolean debugLogs)
    {
        // if mark of darkness isn't active then no speed up can be gained.
        if (!yama.inYamaRegion || yama.phase() != YamaPhase.P3 || !mod.isActive())
        {
            return 0;
        }
        if (!tickCount.isWithinNTicks(consumed, 2) && shadowCrashSweetSpot(client))
        {
            // the player is in the sweet spot with MoD active so the cooldown of whatever they used is now
            // improved by a tick.
            consumed = tickCount.get();
            // theres a bug/feature in the yama mechanic, if you are using a mage weapon then the speed up
            // doesn't apply if the global cooldown is 1 or less. It might also affect range but no-one uses
            // that at yama so IDC. Another reason to prefer melee P3.
            if (attackDelayHoldoffTicks <= 1 && isUsingMagic)
            {
                if (debugLogs)
                {
                    log.debug("shadowCrash success, but magic and low CD");
                }
                return 0;
            }
            if (debugLogs)
            {
                log.debug("shadowCrash success");
            }
            return -1;
        }
        return 0;
    }

    private boolean shadowCrashSweetSpot(final Client client)
    {
        getState(client);
        // if there's not lines on screen then we can't be in the sweet spot yet
        if (lines.size() <= 0)
        {
            return false;
        }

        for (var line : lines)
        {
            if (line.isActive())
            {
                // there can only be one active line at once
                return line.inSweetSpot(client, player);
            }
        }
        return false;
    }

    private void getState(final Client client)
    {
        fireballs.clear();
        lines.clear();
        final WorldView tlwv = client.getTopLevelWorldView();
        final Player localPlayer = client.getLocalPlayer();
        final WorldView playerWv = localPlayer.getWorldView();
        getFireballs(tlwv);
        if (playerWv != tlwv)
        {
            getFireballs(playerWv);
        }
        if (fireballs.size() < 3)
        {
            // don't compute further we should wait for at least 3 fireballs to be on screen
            return;
        }
        player = localPlayer.getWorldLocation();
        lines = getLines();
    }

    private void getFireballs(final WorldView wv)
    {
        for (GraphicsObject graphicsObject : wv.getGraphicsObjects())
        {
            if (graphicsObject.getId() == FIREBALL_ID)
            {
                fireballs.put(graphicsObject.getLocation(), graphicsObject);
            }
        }
    }

    private ArrayList<FireballLine> getLines()
    {
        final var result = new ArrayList<FireballLine>();

        while (fireballs.size() >= 3)
        {
            // fireballs can be in exactly 4 orientations (when accounting for symmetry): vertical,
            // horizontal, diagonal. We start by getting the lexicographically smallest fireball (x first).
            LocalPoint first = null;
            final Set<LocalPoint> fireballsLocations = fireballs.keySet();
            for (var location : fireballsLocations)
            {
                if (first == null)
                {
                    first = location;
                }
                else
                {
                    if (first.getX() > location.getX())
                    {
                        first = location;
                    }
                    else if (first.getX() == location.getX())
                    {
                        if (first.getY() > location.getY())
                        {
                            first = location;
                        }
                    }
                }
            }
            if (first == null)
            {
                return result;
            }
            // We have found the minimum fireball in the x and y

            // See shadowCrash.png
            //
            // This means we have exactly 4 cases to check for
            // * Increasing X -> is vertical line ending at D1
            // * Increasing Y -> is a horizontal line ending at 2
            // * Increasing X & Y -> is a diagonal (D2) as it ends at D2
            // * Increasing X, Decreasing Y -> is a diagonal (D1)
            //
            // This only works if the first point is actually the smallest x first, then smallest y.

            var centre = first.plus(D_DELTA_1, D_DELTA_1);
            var end = first.plus(D_DELTA_2, D_DELTA_2);
            if (fireballsLocations.contains(centre) && fireballsLocations.contains(end))
            {
                result.add(new FireballLine(LineType.D2, centre, fireballs.get(centre)));
                removeLine(first, centre, end);
                continue;
            }

            centre = first.plus(HV_DELTA_1, 0);
            end = first.plus(HV_DELTA_2, 0);
            if (fireballsLocations.contains(centre) && fireballsLocations.contains(end))
            {
                result.add(new FireballLine(LineType.H, centre, fireballs.get(centre)));
                removeLine(first, centre, end);
                continue;
            }

            centre = first.plus(0, HV_DELTA_1);
            end = first.plus(0, HV_DELTA_2);
            if (fireballsLocations.contains(centre) && fireballsLocations.contains(end))
            {
                result.add(new FireballLine(LineType.V, centre, fireballs.get(centre)));
                removeLine(first, centre, end);
                continue;
            }

            centre = first.plus(D_DELTA_1, -D_DELTA_1);
            end = first.plus(D_DELTA_2, -D_DELTA_2);
            if (fireballsLocations.contains(centre) && fireballsLocations.contains(end))
            {
                result.add(new FireballLine(LineType.D1, centre, fireballs.get(centre)));
                removeLine(first, centre, end);
                continue;
            }
            // we do reach this occasionally (no valid line found) and it's when some of the set of 3 has
            // spawned but not all 3, in which case we should just return early.
            return result;
        }
        return result;
    }

    private void removeLine(final LocalPoint a, final LocalPoint b, final LocalPoint c)
    {
        fireballs.remove(a);
        fireballs.remove(b);
        fireballs.remove(c);
    }

    public static boolean eq(final WorldPoint t, final int dx, final int dy, final WorldPoint other)
    {
        return t.getX() + dx == other.getX() && t.getY() + dy == other.getY();
    }

    class FireballLine
    {
        private LineType type;
        private LocalPoint centre;
        private GraphicsObject cObject;

        FireballLine(final LineType type, final LocalPoint centre, final GraphicsObject cObject)
        {
            this.centre = centre;
            this.cObject = cObject;
            this.type = type;
        }

        public boolean isActive()
        {
            // The entire fireball animation is 40 frames, crashes and tick speed up occur between frames: 30
            // and 40. There is 10 animation frames per tick.
            final int animationFrame = cObject.getAnimationFrame();
            return animationFrame >= 30 && animationFrame <= 40;
        }

        public boolean inSweetSpot(final Client client, final WorldPoint lp)
        {
            final var wp = WorldPoint.fromLocal(client, centre);
            // See shadowCrash.png | each line type has two valid sweet spots, +- one tile from the centre
            // where the exact tiles are defined by which line type it is.
            switch (this.type)
            {
            case V:
                return eq(wp, 1, 0, lp) || eq(wp, -1, 0, lp);
            case H:
                return eq(wp, 0, 1, lp) || eq(wp, 0, -1, lp);
            case D1:
                return eq(wp, 1, 1, lp) || eq(wp, -1, -1, lp);
            case D2:
                return eq(wp, -1, 1, lp) || eq(wp, 1, -1, lp);
            }
            return false;
        }
    }

    enum LineType
    {
        V, H, D1, D2;
    }
}
