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

import com.attacktimer.AnimationData;
import com.attacktimer.AttackProcedure;
import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.VariableSpeed.PurgingStaffSpec.YamaData;
import com.attacktimer.VariableSpeed.PurgingStaffSpec.YamaPhase;
import lombok.NonNull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;

public class PurgingStaffSpec implements IVariableSpeed
{

    private static final int HP_FUDGE = 2;

    private static final int VOID_FLARE_HP_P1_P2 = 140 - HP_FUDGE;
    private static final int VOID_FLARE_HP_P3 = 71 - HP_FUDGE;
    private static final int VOID_FLARE_ID = 14179;

    private static final int PURGING_STAFF_ID = 29594;

    private static final int YAMA_REGION_ID = 6045;
    private static final int YAMA_ID = 14176;
    private static final int YAMA_PHASE_TRANSITION_ANIMATION_ID = 12147;

    private YamaData yama;
    private boolean inYamaRegion;
    private NPC lastTarget;

    // https://oldschool.runescape.wiki/w/Purging_staff#Special_attack
    public int apply(Client client, AnimationData curAnimation, AttackProcedure atkType, int damageDealt, int lastSpecDelta, int baseSpeed, int curSpeed)
    {
        // For now the plugin only works for yama
        if (!inYamaRegion)
        {
            return curSpeed;
        }
        var target = Utils.getTargetNPC(client);
        var flare = isEitherAVoidFlare(target, lastTarget);
        lastTarget = target;
        if (flare == null)
        {
            return curSpeed;
        }
        if (yama == null)
        {
            return curSpeed;
        }


        System.out.println("damageDealt: " + Integer.valueOf(damageDealt).toString());
        yama.dealVoidFlareDamage(flare, damageDealt);
        if (lastSpecDelta != -250)
        {
            // not using the spec
            return curSpeed;
        }
        if (Utils.getWeaponId(client) != PURGING_STAFF_ID)
        {
            // not using a purging staff
            return curSpeed;
        }

        if (yama.getVoidFlareDead(flare))
        {
            // speed up!
            // according to the wiki this should be 3 ticks but is actually 2 ticks is normal circumstances
            return curSpeed - 2;
        }
        return curSpeed;
    }
    public void onGameTick(Client client, GameTick tick)
    {
        inYamaRegion = Utils.isInRegionId(client, YAMA_REGION_ID);
        if (!inYamaRegion)
        {
            yama = null;
            return;
        }
        if (yama != null)
        {
            yama.determineYamaPhase();
            return;
        }
        for (NPC npc : client.getTopLevelWorldView().npcs())
        {
            if (npc.getId() == YAMA_ID)
            {
                System.out.println("new yama data");
                yama = new YamaData(npc);
                return;
            }
        }
    }

    public void onChatMessage(ChatMessage event)
    {
        if (!inYamaRegion) return;
        if (yama == null) return;
        if (event.getMessage().startsWith("Your Yama success count is:"))
        {
            System.out.println("yama.killed()");
            yama.killed();
        }
    }

    private static NPC isEitherAVoidFlare(NPC a, NPC b)
    {
        if (a != null && a.getId() == VOID_FLARE_ID) return a;
        if (b != null && b.getId() == VOID_FLARE_ID) return b;
        return null;
    }

    class YamaData
    {
        @NonNull
        NPC yama;
        YamaPhase phase;
        int phaseChangeCooldown;
        Map<NPC, Integer> voidFlares;
        YamaData(NPC yama)
        {
            this.yama = yama;
            this.phase = YamaPhase.P1;
            this.phaseChangeCooldown = 0;
            this.voidFlares = new HashMap<NPC, Integer>();
        }

        public boolean getVoidFlareDead(NPC target)
        {
            if (!this.voidFlares.containsKey(target)) return false;
            Boolean dead = this.voidFlares.get(target) <= 0;
            System.out.println("getVoidFlareDead => "+dead.toString());
            return dead;
        }

        public void dealVoidFlareDamage(NPC target, int damageDealt)
        {
            if (this.voidFlares.containsKey(target))
            {
                this.voidFlares.put(target, this.voidFlares.get(target)-damageDealt);
            }
            else
            {
                var hp = this.getVoidFlareHp()-damageDealt;
                System.out.println("tracking new void flare, started with "+ this.getVoidFlareHp()+" hp - "+damageDealt+" total: "+hp+" hp");
                this.voidFlares.put(target, hp);
            }
        }

        private void determineYamaPhase()
        {
            if (phaseChangeCooldown == 0 && this.yama.getAnimation() == YAMA_PHASE_TRANSITION_ANIMATION_ID)
            {
                System.out.println("yama moving to next phase");
                this.phaseChangeCooldown = 20;
                this.phase = this.phase.nextPhase();
            }
            if (this.phaseChangeCooldown > 0)
                this.phaseChangeCooldown--;
        }

        private int getVoidFlareHp()
        {
            switch (this.phase) {
                case P1:
                case P2:
                    return VOID_FLARE_HP_P1_P2;
                default:
                    return VOID_FLARE_HP_P3;
            }
        }

        private void killed()
        {
            this.phase = YamaPhase.P1;
        }
    }

    enum YamaPhase {
        P1,
        P2,
        P3;

        private YamaPhase nextPhase()
        {
            switch (this) {
                case P1:
                    return P2;
                case P2:
                    return P3;
                case P3:
                    return P3;
            }
            return P1;
        }
    }
}
