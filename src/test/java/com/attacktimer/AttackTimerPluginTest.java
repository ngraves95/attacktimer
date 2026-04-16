package com.attacktimer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AttackTimerPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AttackTimerMetronomePlugin.class);
        RuneLite.main(args);
    }
}