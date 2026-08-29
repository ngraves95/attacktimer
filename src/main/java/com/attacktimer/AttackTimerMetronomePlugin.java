package com.attacktimer;

/*
 * Copyright (c) 2022, Nick Graves <https://github.com/ngraves95>
 * Copyright (c) 2024-2026, Lexer747 <https://github.com/Lexer747>
 * Copyright (c) 2024-2026, Richardant <https://github.com/Richardant>
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

import com.attacktimer.Attacking.Attacking;
import com.attacktimer.ClientUtils.Utils;
import com.attacktimer.VariableSpeed.State.TickCount;
import com.attacktimer.VariableSpeed.VariableSpeed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteArrayDataOutput;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "Attack Timer Metronome",
        description = "Shows a visual cue on an overlay every game tick to help timing based activities",
        tags = {"timers", "overlays", "tick", "skilling"}
)
public class AttackTimerMetronomePlugin extends Plugin
{
    public enum AttackState
    {
        NOT_ATTACKING,
        DELAYED_FIRST_TICK,
        DELAYED,
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private AttackTimerMetronomeTileOverlay overlay;

    @Inject
    private AttackTimerBarOverlay barOverlay;

    @Inject
    private AttackTimerMetronomeConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private Client client;

    @Inject
    private NPCManager npcManager;

    public int tickPeriod = 0;

    private int uiHideDebounceTickCount = 0;
    public int attackDelayHoldoffTicks = ATTACK_DELAY_NONE;

    public AttackState attackState = AttackState.NOT_ATTACKING;
    // The state of the renderer, will lag a few cycles behind the plugin's state. "cycles" in this comment
    // refers to the client.getGameCycle() method, a cycle occurs every 20ms, meaning 30 of them occur per
    // game tick.
    public AttackState renderedState = AttackState.NOT_ATTACKING;

    public Color CurrentColor = Color.WHITE;

    private Spellbook currentSpellBook = Spellbook.STANDARD;
    private int lastUsedWeaponId = -1;
    private Actor lastTarget = null;
    private int soundEffectTick = -1;
    private int soundEffectId = -1;
    private boolean isUsingMagic = false;

    public int pendingEatDelayTicks = 0;

    private ArrayDeque<Integer> specialPercentageEvents = new ArrayDeque<Integer>();
    private static final Damage DAMAGE = new Damage();
    private int dmgDealt = -1;

    public static final TickCount TC = new TickCount();

    private static final int UI_HIDE_DEBOUNCE_TICKS_MAX = 1;
    private static final int ATTACK_DELAY_NONE = 0;
    public static final int DEFAULT_SIZE_UNIT_PX = 25;

    // Add other weapons here if in the Runelite dev shell this prints a different value to it's actual speed:
    //
    //  var itemManager = inject(ItemManager.class);
    //  log.info("Speed {}", itemManager.getItemStats(<id_to_test>).getEquipment().getAspeed());
    private static final Map<Integer, Integer> NON_STANDARD_ATTACK_SPEEDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ItemID.HALLOWFELL, 6)
            .build();

    // These animations are the ones which exceed the duration of their attack cooldown
    // so in this case DO NOT fall back the animation as it is un-reliable.
    private static final Set<AnimationData> UNRELIABLE_ANIMATIONS = new ImmutableSet.Builder<AnimationData>()
            .add(AnimationData.RANGED_BLOWPIPE)
            .add(AnimationData.RANGED_BLAZING_BLOWPIPE)
            .add(AnimationData.MAGIC_EYE_OF_AYAK)
            .add(AnimationData.MAGIC_EYE_OF_AYAK_SPEC)
            .build();

    private static final Map<Integer, Integer> NON_STANDARD_MAGIC_WEAPON_SPEEDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ItemID.TWINFLAME_STAFF, 6)
            .build();

    // Map of problematic itemIds to equivalent working ones.
    // The Echo Venator Bow's ItemStats are returning null, so use the regular bow instead.
    private static final Map<Integer, Integer> WEAPON_ID_MAPPING_WORKAROUNDS = new ImmutableMap.Builder<Integer, Integer>()
            .put(ItemID.VENATOR_BOW_ORNAMENT, ItemID.VENATOR_BOW)
            .build();

    // https://oldschool.runescape.wiki/w/Food/Fast_foods#Food_Delays
    // These constants are not to be confused with eat delay.
    private final int SLOW_FOOD_ATTACK_DELAY_TICKS = 4;
    private final int DEFAULT_FOOD_ATTACK_DELAY_TICKS = 3;
    private final int FAST_EAT_ATTACK_DELAY_TICKS = 2;

    public static final Dimension DEFAULT_SIZE = new Dimension(DEFAULT_SIZE_UNIT_PX, DEFAULT_SIZE_UNIT_PX);

    // region subscribers

    @Subscribe
    public void onVarbitChanged(final VarbitChanged varbitChanged)
    {
        if (varbitChanged.getVarbitId() == VarbitID.SPELLBOOK)
        {
            currentSpellBook = Spellbook.fromVarbit(varbitChanged.getValue());
        }
        if (varbitChanged.getVarpId() == VarPlayerID.SA_ENERGY)
        {
            specialPercentageEvents.addLast(varbitChanged.getValue());
        }
    }

    // onSoundEffectPlayed used to track spell casts, for when the player casts a spell on first tick coming
    // off cooldown, in some cases (e.g. ice barrage) the player will have no animation.
    @Subscribe
    public void onSoundEffectPlayed(final SoundEffectPlayed event)
    {
        if (!config.enableMetronome())
            return;
        // event.getSource() will be null if the player cast a spell, it's only for area sounds.
        soundEffectTick = client.getTickCount();
        soundEffectId = event.getSoundId();
    }

    @Subscribe
    protected void onFakeXpDrop(final FakeXpDrop event)
    {
        if (!config.enableMetronome())
            return;
        if (DAMAGE.onXpDrop(event, TC))
        {
            if (inPreAttackWindow())
            {
                // We recompute attack speed here incase the hitsplat mattered (e.g. purging staff)
                logStateTrace("onFakeXpDrop");
                performAttack();
            }
        }
    }

    @Subscribe
    protected void onStatChanged(final StatChanged event)
    {
        if (!config.enableMetronome())
            return;
        if (DAMAGE.onXpDrop(event, TC))
        {
            if (inPreAttackWindow())
            {
                // We recompute attack speed here incase the hitsplat mattered (e.g. purging staff)
                logStateTrace("onFakeXpDrop");
                performAttack();
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(final NpcSpawned npcSpawned)
    {
        if (!config.enableMetronome())
            return;
        VariableSpeed.onNpcSpawned(client, npcSpawned);
    };

    @Subscribe
    public void onNpcDespawned(final NpcDespawned npcDespawned)
    {
        if (!config.enableMetronome())
            return;
        VariableSpeed.onNpcDespawned(client, npcDespawned);
    };

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("attacktimermetronome"))
        {
            attackDelayHoldoffTicks = 0;
        }
    }

    @Subscribe
    public void onChatMessage(final ChatMessage event)
    {
        if (!config.enableMetronome())
            return;
        final String message = event.getMessage();

        if (EAT_MESSAGE.matcher(message).find())
        {
            int attackDelay;
            if (FAST_EAT.matcher(message).find())
            {
                attackDelay = FAST_EAT_ATTACK_DELAY_TICKS;
            }
            else if (SLOW_FOOD.matcher(message).find())
            {
                attackDelay = SLOW_FOOD_ATTACK_DELAY_TICKS;
            }
            else
            {
                attackDelay = DEFAULT_FOOD_ATTACK_DELAY_TICKS;
            }

            // We should always add eat delay
            pendingEatDelayTicks += attackDelay;
        }
        VariableSpeed.onChatMessage(client, event);
    }

    // endregion

    @Provides
    AttackTimerMetronomeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AttackTimerMetronomeConfig.class);
    }

    private int getWeaponId()
    {
        final int weaponId = Utils.getWeaponId(client);
        return WEAPON_ID_MAPPING_WORKAROUNDS.getOrDefault(weaponId, weaponId);
    }

    private ItemStats getWeaponStats(int weaponId)
    {
        if (NON_STANDARD_ATTACK_SPEEDS.containsKey(weaponId))
        {
            return new ItemStats(true, -1, -1,
                    ItemEquipmentStats.builder().aspeed(NON_STANDARD_ATTACK_SPEEDS.get(weaponId)).build());
        }
        return itemManager.getItemStats(weaponId);
    }

    private boolean getSalamanderAttack()
    {
        return client.getLocalPlayer().hasSpotAnim(SpotanimID.FIREBREATH);
    }

    private void setAttackDelay()
    {
        int weaponId = getWeaponId();
        AnimationData curAnimation = AnimationData.fromId(client.getLocalPlayer().getAnimation());
        PoweredStaves stave = PoweredStaves.getPoweredStaves(weaponId, curAnimation);
        boolean matchesSpellbook = matchesSpellbook(curAnimation);
        attackDelayHoldoffTicks = getWeaponSpeed(weaponId, stave, curAnimation, currentSpellBook, matchesSpellbook);
        lastUsedWeaponId = weaponId;
    }

    // matchesSpellbook tries two methods, matching the animation the spell book based on the enum of
    // pre-coded matches, and then the second set of matches against the known sound id of the spell (which
    // unfortunately doesn't work if the player has them disabled).
    private boolean matchesSpellbook(AnimationData curAnimation)
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

    private int getMagicBaseSpeed(int weaponId)
    {
        return NON_STANDARD_MAGIC_WEAPON_SPEEDS.getOrDefault(weaponId, 5);
    }

    private int getWeaponSpeed(int weaponId, PoweredStaves stave, AnimationData curAnimation, Spellbook spellbook, boolean matchesSpellbook)
    {
        final var specDelta = Utils.getLastDelta(specialPercentageEvents);
        dmgDealt = DAMAGE.compute(TC);
        if (stave != null && stave.getAnimations().contains(curAnimation))
        {
            isUsingMagic = true;
            // We are currently dealing with a staves in which case we can make decisions based on the
            // spellbook flag. We can only improve this by using a deprecated API to check the projectile
            // matches the stave rather than a manual spell, but this is good enough for now.
            return VariableSpeed.compute(client, curAnimation, AttackProcedure.POWERED_STAVE, spellbook, dmgDealt, specDelta, 4);
        }

        if (matchesSpellbook && isManualCasting(curAnimation))
        {
            isUsingMagic = true;
            // You can cast with anything equipped in which case we shouldn't look to invent for speed.
            return VariableSpeed.compute(client, curAnimation, AttackProcedure.MANUAL_AUTO_CAST, spellbook, dmgDealt, specDelta, getMagicBaseSpeed(weaponId));
        }

        isUsingMagic = false;
        final ItemStats weaponStats = getWeaponStats(weaponId);
        if (weaponStats == null)
        {
            // Assume barehanded == 4t
            return VariableSpeed.compute(client, curAnimation, AttackProcedure.MELEE_OR_RANGE, spellbook, dmgDealt, specDelta, 4);
        }
        // Deadline for next available attack.
        final int aspeed = weaponStats.getEquipment().getAspeed();
        return VariableSpeed.compute(client, curAnimation, AttackProcedure.MELEE_OR_RANGE, spellbook, dmgDealt, specDelta, aspeed);
    }

    private boolean isManualCasting(AnimationData curId)
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

    private void performAttack()
    {
        attackState = AttackState.DELAYED_FIRST_TICK;
        setAttackDelay();
        tickPeriod = attackDelayHoldoffTicks;
        uiHideDebounceTickCount = UI_HIDE_DEBOUNCE_TICKS_MAX;
        lastTarget = client.getLocalPlayer().getInteracting();
    }

    public int getTicksUntilNextAttack()
    {
        return 1 + attackDelayHoldoffTicks;
    }

    public int getWeaponPeriod()
    {
        return tickPeriod;
    }

    public boolean isAttackCooldownPending()
    {
        return attackState == AttackState.DELAYED
            || attackState == AttackState.DELAYED_FIRST_TICK
            || uiHideDebounceTickCount > 0;
    }

    private static final String GENERIC_EAT = "You eat";
    // unfortunately you don't get any message when full HP
    private static final String VAMPYRIUM_EAT = "Your stomach doesn't like it... but it heals some health"; // https://oldschool.runescape.wiki/w/Stymphike_tartare
    private static final String BARBARIAN_POTIONS = "You drink the lumpy potion"; // barbarian potions
                                                                                  // https://oldschool.runescape.wiki/w/Barbarian_Training#Barbarian_potions
    private static final String JUG_OF_WINE = "You drink the wine"; // Wine
                                                                    // https://oldschool.runescape.wiki/w/Jug_of_wine

    // Match only the start of the line with `^` and the Pattern.MULTILINE
    private static final Pattern EAT_MESSAGE = Pattern.compile(
            "^(" + GENERIC_EAT + "|" + BARBARIAN_POTIONS + "|" + JUG_OF_WINE + "|" + VAMPYRIUM_EAT + ")",
            Pattern.MULTILINE & Pattern.CASE_INSENSITIVE);

    //
    private static final Pattern SLOW_FOOD = Pattern.compile("^(" + VAMPYRIUM_EAT + ")",
            Pattern.MULTILINE & Pattern.CASE_INSENSITIVE);

    // gnome foods are also fast eats (Note these are not the food names as the wiki lists them, but the name
    // as written in chat), also pre-made and handmade have the same chat message.
    private static final String FAST_GNOME_FOOD = "worm hole|tangled toads legs|veg ball|chocolate bomb|worm crunchies|toad crunchies|"
            + "choc chip crunchies|spicy crunchies|fruit batta|cheese and tomato batta|toad batta|vegetable batta|worm batta";
    private static final String FAST_FOOD = "karambwan|halibut";
    // Unfortunately these have just the generic "You eat the food." so there is no easy way to tell if you
    // have the quicker eat delay. https://oldschool.runescape.wiki/w/Crystal_paddlefish and
    // https://oldschool.runescape.wiki/w/Corrupted_paddlefish
    private static final Pattern FAST_EAT = Pattern.compile("(" + FAST_FOOD + "|" + FAST_GNOME_FOOD + ")",
            Pattern.CASE_INSENSITIVE);

    // onInteractingChanged is the driver for detecting if the player attacked out side the usual tick window
    // of the onGameTick events.
    @Subscribe
    public void onInteractingChanged(InteractingChanged interactingChanged)
    {
        if (!config.enableMetronome())
            return;
        Actor source = interactingChanged.getSource();
        Actor target = interactingChanged.getTarget();

        Player p = client.getLocalPlayer();

        if (source.equals(p) && (target instanceof NPC))
        {
            switch (attackState)
            {
            case NOT_ATTACKING:
                isUsingMagic = false;
                // If not previously attacking, this action can result in a queued attack or
                // an instant attack. If its queued, don't trigger the cooldown yet.
                if (Attacking.isPlayerAttacking(client, npcManager))
                {
                    logStateTrace("onInteractingChanged");
                    performAttack();
                }
                break;
            case DELAYED_FIRST_TICK:
                // fallthrough
            case DELAYED:
                // Don't reset tick counter or tick period.
                break;
            }
        }

        applyAndClearEats();
    }

    private void applyAndClearEats()
    {
        int pendingEats = pendingEatDelayTicks;
        attackDelayHoldoffTicks += pendingEats;
        pendingEatDelayTicks -= pendingEats;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (!config.enableMetronome())
            return;
        VariableSpeed.onGameTick(client, tick);
        final boolean isAttacking = Attacking.isPlayerAttacking(client, npcManager);
        switch (attackState)
        {
        case NOT_ATTACKING:
            if (isAttacking)
            {
                logStateTrace("onGameTick");
                performAttack(); // Sets state to DELAYED_FIRST_TICK.
            }
            else
            {
                uiHideDebounceTickCount = Math.max(-20, uiHideDebounceTickCount - 1);
            }
            break;
        case DELAYED_FIRST_TICK:
            // we stay in this state for one tick to allow for 0-ticking
            logStateTrace("onGameTick DELAYED_FIRST_TICK");
            attackState = AttackState.DELAYED;
            // fallthrough
        case DELAYED:
            logStateTrace("onGameTick DELAYED");
            if (attackDelayHoldoffTicks <= 0)
            { // Eligible for a new attack
                if (isAttacking)
                {
                    logStateTrace("onGameTick");
                    performAttack();
                }
                else
                {
                    attackState = AttackState.NOT_ATTACKING;
                }
            }
        }

        // This needs to come after performAttack as it's an additive affect
        applyAndClearEats();

        // clamp the attackDelayHoldoffTicks at -20, this is so we correctly account for eats even when not
        // attacking, but don't count down forever.
        attackDelayHoldoffTicks = Math.max(-20, attackDelayHoldoffTicks - 1);
        while (specialPercentageEvents.size() > 5)
        {
            specialPercentageEvents.removeFirst();
        }
        DAMAGE.cleanup();
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        overlay.setPreferredSize(DEFAULT_SIZE);
        overlayManager.add(barOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        overlayManager.remove(barOverlay);
        attackDelayHoldoffTicks = 0;
    }

    @VisibleForTesting
    public void writeState(ByteArrayDataOutput outChannel)
    {
        StringBuilder sb = getState();
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        outChannel.write(bytes);
    }

    public void logStateTrace(String trace)
    {
        if (!config.debugLogs())
        {
            return;
        }
        StringBuilder sb = getState();
        log.debug("[" + trace + "]: " + sb.toString());
    }

    private StringBuilder getState()
    {
        StringBuilder sb = new StringBuilder();
        // @formatter:off
        sb.append("tickPeriod: "); sb.append(this.tickPeriod);sb.append(SEPARATOR);
        sb.append("uiHideDebounceTickCount: "); sb.append(this.uiHideDebounceTickCount);sb.append(SEPARATOR);
        sb.append("attackDelayHoldoffTicks: "); sb.append(this.attackDelayHoldoffTicks);sb.append(SEPARATOR);
        sb.append("dmgDealt: "); sb.append(this.dmgDealt);sb.append(SEPARATOR);
        sb.append("attackState: "); sb.append(this.attackState);sb.append(SEPARATOR);
        sb.append("renderedState: "); sb.append(this.renderedState);sb.append(SEPARATOR);
        sb.append("lastTarget: "); sb.append(this.lastTarget == null ? "null" : this.lastTarget.getName());sb.append("\n");
        sb.append("pendingEatDelayTicks: "); sb.append(this.pendingEatDelayTicks);sb.append(SEPARATOR);
        sb.append("currentSpellBook: "); sb.append(this.currentSpellBook);sb.append(SEPARATOR);
        sb.append("soundEffectTick: "); sb.append(this.soundEffectTick);sb.append(SEPARATOR);
        sb.append("soundEffectId: "); sb.append(this.soundEffectId);sb.append("\n");
        // @formatter:on
        return sb;
    }

    private static final String SEPARATOR = ", ";

    public void onRender()
    {
        final int delta = VariableSpeed.SHADOW_CRASH.onRender(client, attackDelayHoldoffTicks, isUsingMagic, config.debugLogs());

        if (delta != 0)
        {
            logStateTrace("onRender");
            attackDelayHoldoffTicks += delta;
            // if a change in attack delay would cause the delay to be less than 0 we hide the display
            if (attackDelayHoldoffTicks < 0)
            {
                attackState = AttackState.NOT_ATTACKING;
            }
        }
        checkForLateWeaponSwaps();
    }

    public void checkForLateWeaponSwaps()
    {
        final boolean weaponMisMatch = getWeaponId() != lastUsedWeaponId;

        // This windowing safe guards of from late swaps inside a tick, if we have already rendered the tick
        // then we shouldn't perform another attack. We don't need to check for a valid target
        // (isPlayerAttacking) as this must have already been check to be in `DELAYED_FIRST_TICK`
        if (inPreAttackWindow() && weaponMisMatch)
        {
            logStateTrace("checkForLateWeaponSwaps");
            // "Perform an attack" this is overwrites the last attack since we now know the user swapped
            // "Something" this tick, the equipped weapon detection will pick up specific weapon swaps. Even
            // swapping more than 1 weapon inside a single tick.
            performAttack();
        }
    }

    /**
     * inPreAttackWindow returns true if and only if the plugin has computed an attack speed and
     * determined we are attacking an NPC, but the timer has not been rendered yet. Hence there is time
     * still to adjust the speed if new data would change the result.
     *
     * @return true if an attack is detected and the plugin has not yet rendered the timer for the
     *         current attack, false in every other case.
     */
    private boolean inPreAttackWindow()
    {
        return attackState == AttackState.DELAYED_FIRST_TICK && renderedState != attackState;
    }

}
