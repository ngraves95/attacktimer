package com.attacktimer.VariableSpeed.State;

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
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;

/**
 * Yama tracks various bits of state related to the boss https://oldschool.runescape.wiki/w/Yama.
 */
public class Yama implements IStateTracker
{
    private static final int HP_FUDGE = 2;

    private static final int VOID_FLARE_HP_P1_P2 = 140 - HP_FUDGE;
    private static final int VOID_FLARE_HP_P3 = 71 - HP_FUDGE;

    private static final int YAMA_REGION_ID = 6045;
    private static final int YAMA_PHASE_TRANSITION_ANIMATION_ID = 12147;

    // null if yama isn't alive.
    public YamaData yama;
    public boolean inYamaRegion;
    public NPC lastTarget;

    @Override
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
    }

    @Override
    public void onChatMessage(Client client, ChatMessage event)
    {
        if (!inYamaRegion || yama == null)
        {
            return;
        }
        if (event.getMessage().startsWith("Your Yama success count is:"))
        {
            yama.killed();
        }
    }

    @Override
    public void onNpcSpawned(final Client client, final NpcSpawned npcSpawned)
    {
        if (!inYamaRegion)
        {
            return;
        }
        if (yama != null)
        {
            return;
        }
        final NPC npc = npcSpawned.getNpc();
        if (npc.getId() == NpcID.YAMA)
        {
            yama = new YamaData(npc);
        }
    }

    public static NPC isEitherVoidFlare(NPC a, NPC b)
    {
        if (a != null && a.getId() == NpcID.YAMA_VOIDFLARE)
        {
            return a;
        }

        if (b != null && b.getId() == NpcID.YAMA_VOIDFLARE)
        {
            return b;
        }
        return null;
    }

    public void dealVoidFlareDamage(NPC target, int damageDealt)
    {
        if (yama == null)
        {
            return;
        }
        this.yama.dealVoidFlareDamage(target, damageDealt);
    }

    public boolean getVoidFlareDead(NPC target)
    {
        if (yama == null)
        {
            return false;
        }
        return this.yama.getVoidFlareDead(target);
    }

    public YamaPhase phase()
    {
        if (yama == null)
        {
            return YamaPhase.P1;
        }
        return this.yama.phase;
    }

    class YamaData
    {
        @NonNull
        private NPC yama;
        private YamaPhase phase;
        private int phaseChangeCooldown;
        private Map<NPC, Integer> voidFlares;

        YamaData(NPC yama)
        {
            this.yama = yama;
            this.phase = YamaPhase.P1;
            this.phaseChangeCooldown = 0;
            this.voidFlares = new HashMap<NPC, Integer>();
        }

        public boolean getVoidFlareDead(NPC target)
        {
            if (!this.voidFlares.containsKey(target))
            {
                return false;
            }
            return this.voidFlares.get(target) <= 0;
        }

        public void dealVoidFlareDamage(NPC target, int damageDealt)
        {
            if (this.voidFlares.containsKey(target))
            {
                this.voidFlares.put(target, this.voidFlares.get(target) - damageDealt);
            }
            else
            {
                final var hp = this.getVoidFlareHp() - damageDealt;
                this.voidFlares.put(target, hp);
            }
        }

        private void determineYamaPhase()
        {
            if (phaseChangeCooldown == 0 && this.yama.getAnimation() == YAMA_PHASE_TRANSITION_ANIMATION_ID)
            {
                this.phaseChangeCooldown = 20;
                this.phase = this.phase.nextPhase();
            }
            if (this.phaseChangeCooldown > 0)
            {
                this.phaseChangeCooldown--;
            }
        }

        private int getVoidFlareHp()
        {
            switch (this.phase)
            {
            case P1: // fallthrough
            case P2:
                return VOID_FLARE_HP_P1_P2;
            default:
                return VOID_FLARE_HP_P3;
            }
        }

        private void killed()
        {
            this.phase = YamaPhase.P1;
            this.voidFlares.clear();
        }
    }
}
