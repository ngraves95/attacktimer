package com.attacktimer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
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
	String TickNumberSettings = "Attack Cooldown Tick Settings";

	@ConfigItem(
			position = 1,
			keyName = "showTick",
			name = "Show Attack Cooldown Ticks",
			description = "Shows number of ticks until next attack",
			section = TickNumberSettings
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
			section = TickNumberSettings
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
			section = TickNumberSettings
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
			section = TickNumberSettings
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
			section = TickNumberSettings
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
			section = TickNumberSettings
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
			section = TickNumberSettings
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
			section = TickNumberSettings
	)
	@Range(min = -50, max = 50)
	default int heightTickOffset()
	{
		return 0;
	}

	@ConfigSection(
			name = "Attack Bar",
			description = "Change the colors and number of colors to cycle through",
			position = 2
	)
	String AttackBarSettings = "Attack Cooldown Bar Settings";

	@ConfigItem(
			position = 1,
			keyName = "attackBar",
			name = "Show Attack Bar",
			description = "Show the attack bar",
			section = AttackBarSettings
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
			section = AttackBarSettings
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
			section = AttackBarSettings
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
			section = AttackBarSettings
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
			section = AttackBarSettings
	)
	default boolean barDirection()
	{
		return true;
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
