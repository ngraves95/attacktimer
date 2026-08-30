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

import net.runelite.api.Client;

public class ManualCasting
{
    /**
     * is returns true if the plugin believes the player is currently manually casting a spell
     */
    public static boolean is(final Client client, final AnimationData curId, final int soundEffectTick, final int soundEffectId)
    {
        // If you use a weapon like a blow pipe which has an animation longer than it's cool down then cast an
        // ancient attack it wont have an animation at all. We can therefore need to detect this with a list
        // of sounds instead. This obviously doesn't work if the player is muted. ATM I can't think of a way
        // to detect this type of attack as a cast, only sound is an indication that the player is on
        // cooldown, melee attacks, etc will trigger an animation overwriting the last frame of the blowpipe's
        // idle animation.
        final boolean castingFromSound = client.getTickCount() == soundEffectTick
                ? CastingSoundData.isCastingSound(soundEffectId)
                : false;
        final boolean castingFromAnimation = AnimationData.isManualCasting(curId);
        return castingFromSound || castingFromAnimation;
    }
}
