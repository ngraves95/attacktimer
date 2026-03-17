package com.attacktimer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttackBarStyle
{
	AUTO("Auto"),
	STANDARD("Standard"),
	HIGH_DETAIL("High Detail");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
