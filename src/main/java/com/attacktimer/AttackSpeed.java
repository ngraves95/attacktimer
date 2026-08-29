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

import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.VariableSpeed.State.TickCount;
import com.attacktimer.VariableSpeed.VariableSpeed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

/**
 *
 * AttackSpeed stores the state specifically for computing the attack speed of a player, not all state is here
 * as individual variable speed implementations may also track their own state. It was refactored out of the
 * main plugin so that other capabilities can compute this.
 */
public class AttackSpeed
{
    AttackSpeed(final TickCount tc)
    {
        this.tickCount = tc;
    }

    private ArrayDeque<Integer> specialPercentageEvents = new ArrayDeque<Integer>();
    private final Damage damage = new Damage();
    @Getter
    private int dmgDealt = -1;
    private final TickCount tickCount;
    @Getter
    private int soundEffectTick = -1;
    @Getter
    private int soundEffectId = -1;
    @Getter
    private boolean isUsingMagic = false;

    /**
     * compute determines from the current client state what attack speed the player has, this includes all
     * variables, weapon speed, unique boss mechanics etc.
     *
     * This is centralised here because there's a large amount of complexity, (e.g. the variable speed
     * implementations).
     *
     * This method is stateful it should only be called when the calling code is certain that a player attack
     * has occurred, it sets the isUsing magic flag on call.
     *
     * @param client the runelite client
     * @param curAnimation the animation currently being done by the player
     * @param spellbook the current spellbook the player is on
     * @param itemManager the runelite item manager
     * @return the attack speed of the player.
     */
    public int compute(final Client client, final AnimationData curAnimation, final Spellbook spellbook, final ItemManager itemManager)
    {
        final int weaponId = Utils.getWeaponId(client);
        final PoweredStaves stave = PoweredStaves.getPoweredStaves(weaponId, curAnimation);
        final var specDelta = Utils.getLastDelta(specialPercentageEvents);
        dmgDealt = damage.compute(tickCount);
        if (stave != null && stave.getAnimations().contains(curAnimation))
        {
            isUsingMagic = true;
            // We are currently dealing with a staves in which case we can make decisions based on the
            // spellbook flag. We can only improve this by using a deprecated API to check the projectile
            // matches the stave rather than a manual spell, but this is good enough for now.
            return VariableSpeed.compute(client, curAnimation, AttackProcedure.POWERED_STAVE, spellbook, dmgDealt, specDelta, 4);
        }

        if (matchesSpellbook(client, curAnimation, spellbook)
                && ManualCasting.is(client, curAnimation, soundEffectTick, soundEffectId))
        {
            isUsingMagic = true;
            // You can cast with anything equipped in which case we shouldn't look to invent for speed.
            return VariableSpeed.compute(client, curAnimation, AttackProcedure.MANUAL_AUTO_CAST, spellbook, dmgDealt, specDelta, getMagicBaseSpeed(weaponId));
        }

        isUsingMagic = false;
        final int aspeed = Utils.getWeaponSpeed(client, itemManager, weaponId);
        // Deadline for next available attack.
        return VariableSpeed.compute(client, curAnimation, AttackProcedure.MELEE_OR_RANGE, spellbook, dmgDealt, specDelta, aspeed);
    }

    public void onTick()
    {
        while (specialPercentageEvents.size() > 5)
        {
            specialPercentageEvents.removeFirst();
        }
        damage.onTick();
    }

    public void varbitSpecialAttackChanged(final int value)
    {
        specialPercentageEvents.addLast(value);
    }

    public boolean onXpDrop(final FakeXpDrop event)
    {
        return damage.onXpDrop(event, tickCount);
    }

    public boolean onXpDrop(final StatChanged event)
    {
        return damage.onXpDrop(event, tickCount);
    }

    public void onSoundEffectPlayed(final Client client, final SoundEffectPlayed event)
    {
        // event.getSource() will be null if the player cast a spell, it's only for area sounds.
        soundEffectTick = client.getTickCount();
        soundEffectId = event.getSoundId();
    }

    private static final Map<Integer, Integer> NON_STANDARD_MAGIC_WEAPON_SPEEDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ItemID.TWINFLAME_STAFF, 6).build();

    private static int getMagicBaseSpeed(final int weaponId)
    {
        return NON_STANDARD_MAGIC_WEAPON_SPEEDS.getOrDefault(weaponId, 5);
    }

    // matchesSpellbook tries two methods, matching the animation the spell book based on the enum of
    // pre-coded matches, and then the second set of matches against the known sound id of the spell (which
    // unfortunately doesn't work if the player has them disabled).
    private boolean matchesSpellbook(final Client client, final AnimationData curAnimation, final Spellbook currentSpellBook)
    {
        if (curAnimation != null && curAnimation.matchesSpellbook(currentSpellBook))
        {
            return true;
        }
        if (client.getTickCount() == soundEffectTick)
        {
            return CastingSoundData.getSpellBookFromId(soundEffectId) == currentSpellBook;
        }
        return false;
    }

    @VisibleForTesting
    public void reset()
    {
        dmgDealt = -1;
        soundEffectId = -1;
        soundEffectId = -1;
        isUsingMagic = false;
        specialPercentageEvents.clear();
        damage.reset();
    }
}
