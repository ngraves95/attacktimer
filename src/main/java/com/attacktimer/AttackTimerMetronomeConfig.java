package com.attacktimer;

/*
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
 * Copyright (c) 2024-2026, Lexer747 <https://github.com/Lexer747>
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigSection;
import java.awt.Color;

@ConfigGroup("attacktimermetronome")
public interface AttackTimerMetronomeConfig extends Config
{
    @ConfigItem(
            position = 0,
            keyName = "enableMetronome",
            name = "Attack Timer Metronome",
            description = "Enable visual metronome"
    )
    default boolean enableMetronome()
    {
        return true;
    }

    @ConfigSection(
            name = "Attack Cooldown Tick Settings",
            description = "Change attack tick cooldown settings",
            position = 1
    )
    final String TICK_NUMBER_SETTINGS = "Attack Cooldown Tick Settings";

    @ConfigItem(
            position = 1,
            keyName = "showTick",
            name = "Show Attack Cooldown Ticks",
            description = "Shows number of ticks until next attack",
            section = TICK_NUMBER_SETTINGS
    )
    default boolean showTick()
    {
        return true;
    }


    @ConfigItem(
            position = 2,
            keyName = "disableFontScaling",
            name = "Disable Font Size Scaling (Metronome Tick Only)",
            description = "Disables font size scaling for metronome tick number",
            section = TICK_NUMBER_SETTINGS
    )
    default boolean disableFontScaling()
    {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "fontSize",
            name = "Font Size (Overhead Tick Only)",
            description = "Change the font size of the overhead attack cooldown ticks",
            section = TICK_NUMBER_SETTINGS
    )
    @Range(min = 8, max = 50)
    default int fontSize()
    {
        return 15;
    }

    @ConfigItem(
            position = 4,
            keyName = "countColor",
            name = "Tick Number Color",
            description = "Configures the color of tick number",
            section = TICK_NUMBER_SETTINGS
    )
    default Color NumberColor()
    {
        return Color.CYAN;
    }

    @ConfigItem(
            position = 5,
            keyName = "lastColor",
            name = "Last Tick Color",
            description = "Configures the color of tick number when it says 1",
            section = TICK_NUMBER_SETTINGS
    )
    default Color LastColor()
    {
        return Color.CYAN;
    }

    @ConfigItem(
            position = 6,
            keyName = "fontType",
            name = "Font Type",
            description = "Change the font of the tick number",
            section = TICK_NUMBER_SETTINGS
    )
    default FontTypes fontType()
    {
        return FontTypes.REGULAR;
    }

    @ConfigItem(
            position = 7,
            keyName = "ticksPosition",
            name = "Ticks Position",
            description = "Position of the tick number respective to the player",
            section = TICK_NUMBER_SETTINGS
    )
    default TicksPosition ticksPosition()
    {
        return TicksPosition.DEFAULT;
    }

    @ConfigItem(
            position = 8,
            keyName = "tickHeightOffset",
            name = "Height Offset",
            description = "Height offset for minor adjustments of the tick number",
            section = TICK_NUMBER_SETTINGS
    )
    @Range(min = -50, max = 50)
    default int heightTickOffset()
    {
        return 0;
    }

    @ConfigItem(
            position = 9,
            keyName = "useZeroBasedTickCount",
            name = "Zero-based Tick Count",
            description = "Count ticks to 0 instead of 1",
            section = TICK_NUMBER_SETTINGS
    )
    default boolean useZeroBasedTickCount()
    {
        return false;
    }

    @ConfigSection(
            name = "Attack Bar",
            description = "Change the colors and number of colors to cycle through",
            position = 2
    )
    final String ATTACK_BAR_SETTINGS = "Attack Cooldown Bar Settings";

    @ConfigItem(
            position = 1,
            keyName = "attackBar",
            name = "Show Attack Bar",
            description = "Show the attack bar",
            section = ATTACK_BAR_SETTINGS
    )
    default boolean showBar()
    {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "attackBarHeightOffset",
            name = "Height Offset",
            description = "Height offset for the bar from top of player model",
            section = ATTACK_BAR_SETTINGS
    )
    @Range(min = -100, max = 100)
    default int heightOffset()
    {
        return 0;
    }

    @ConfigItem(
            position = 3,
            keyName = "attackBarEmpties",
            name = "Empties Before Attack",
            description = "Controls whether the attack bar will fully empty before a new attack can occur",
            section = ATTACK_BAR_SETTINGS
    )
    default boolean barEmpties()
    {
        return true;
    }

    @ConfigItem(
            position = 4,
            keyName = "attackBarFills",
            name = "Fills Before Attack",
            description = "Controls whether the attack bar will fill completely after an attack",
            section = ATTACK_BAR_SETTINGS
    )
    default boolean barFills()
    {
        return true;
    }

    @ConfigItem(
            position = 5,
            keyName = "attackBarDirection",
            name = "Attack Bar Fills or Drains",
            description = "Controls whether the attack bar will fill or drain as a cooldown",
            section = ATTACK_BAR_SETTINGS
    )
    default boolean barDirection()
    {
        return true;
    }

    @ConfigItem(
            position = 6,
            keyName = "attackBarStyle",
            name = "Attack Bar Style",
            description = "Auto matches HD/SD from Interface Styles plugin. Standard forces the basic bar. High Detail forces the HD bar.",
            section = ATTACK_BAR_SETTINGS
    )
    default AttackBarStyle barStyle()
    {
        return AttackBarStyle.AUTO;
    }

    @Getter
    @AllArgsConstructor
    enum TicksPosition
    {
        DEFAULT("Default"),
        TOP("Top"),
        CENTERED("Centered"),
        BOTTOM("Bottom");

        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }
}
