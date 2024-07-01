package com.attacktimer;

/*
 * Copyright (c) 2024, Lexer747 <https://github.com/Lexer747>
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.helpers.MessageFormatter;

public class EnumTests
{
    @Test
    public void runtimeCheck()
    {
        // Ensures every enum has no duplicate values
        for (AnimationData a : AnimationData.values())
        {
            System.out.println(a.toString());
        }
        for (PoweredStaves a : PoweredStaves.values())
        {
            System.out.println(a.toString());
        }
        for (CastingSoundData a : CastingSoundData.values())
        {
            System.out.println(a.toString());
        }
        for (WeaponType a : WeaponType.values())
        {
            System.out.println(a.toString());
        }
        assertFalse(PoweredStaves.LOCAL_DEBUGGING);
    }

    @Ignore("Only for reporting purposes")
    @Test
    public void missingIdReport()
    {
        boolean failed = false;
        String fails = new String();
        for (PoweredStaves staff : PoweredStaves.values())
        {
            for (int id : staff.getIds()) {
                boolean containsKey = PoweredStaves.poweredStaves.get(id).containsKey(PoweredStaves.UNKNOWN_SPELL);
                if (containsKey)
                {
                    failed = true;
                    fails += MessageFormatter.format("Staff | {} IDs:{} doesn't have a spell associated\n", staff, staff.getIds()).getMessage();
                }
                break;
            }
            if (staff.getProjectiles() == null)
            {
                fails += MessageFormatter.format("Staff | {} doesn't have a projectile associated\n", staff).getMessage();
            }
        }

        if (failed) {
            fail(fails);
        }
    }
}