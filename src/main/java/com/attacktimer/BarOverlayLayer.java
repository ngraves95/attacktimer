package com.attacktimer;

import net.runelite.client.ui.overlay.OverlayLayer;

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

/**
 * BarOverlayLayer is a mirror of Runelite's OverlayLayer but with `MANUAL` and `ALWAYS_ON_TOP` removed since
 * we don't want to show those options.
 */
public enum BarOverlayLayer
{
    ABOVE_SCENE, UNDER_WIDGETS, ABOVE_WIDGETS;

    public OverlayLayer ToOverlayLayer()
    {
        switch (this)
        {
        case ABOVE_SCENE:
            return OverlayLayer.ABOVE_SCENE;
        case UNDER_WIDGETS:
            return OverlayLayer.UNDER_WIDGETS;
        case ABOVE_WIDGETS:
            return OverlayLayer.ABOVE_WIDGETS;
        }
        // unreachable
        return OverlayLayer.ABOVE_SCENE;
    }

    @Override
    public String toString()
    {
        switch (this)
        {
        case ABOVE_SCENE:
            return "Below Overheads";
        case UNDER_WIDGETS:
            return "Above Overheads";
        case ABOVE_WIDGETS:
            return "Above Widgets";
        }
        // unreachable
        return "";
    }
}
