package com.attacktimer;

import com.attacktimer.ClientUtils.Utils;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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

    @Inject
    private ClientThread clientThread;

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
    private int lastEquippingMonotonicValue = -1;
    private int soundEffectTick = -1;
    private int soundEffectId = -1;

    public int pendingEatDelayTicks = 0;

    private ArrayDeque<Integer> specialPercentageEvents = new ArrayDeque<Integer>();
    private Map<Skill, ArrayDeque<Integer>> combatExpEarned = Map.of(
        Skill.MAGIC, new ArrayDeque<Integer>(),
        Skill.RANGED, new ArrayDeque<Integer>(),
        Skill.DEFENCE, new ArrayDeque<Integer>(),
        Skill.STRENGTH, new ArrayDeque<Integer>(),
        Skill.ATTACK, new ArrayDeque<Integer>()
    );

    private static final int UI_HIDE_DEBOUNCE_TICKS_MAX = 1;
    private static final int ATTACK_DELAY_NONE = 0;
    public static final int DEFAULT_SIZE_UNIT_PX = 25;

    public static final int SALAMANDER_SET_ANIM_ID = 952; // Used by all 4 types of salamander https://oldschool.runescape.wiki/w/Salamander

    private static final int TWINFLAME_STAFF_WEAPON_ID = 30634;
    private static final int ECHO_VENATOR_BOW_WEAPON_ID = 30434;
    private static final int VENATOR_BOW_WEAPON_ID = 27610;

    // These animations are the ones which exceed the duration of their attack cooldown
    // so in this case DO NOT fall back the animation as it is un-reliable.
    private static final Set<AnimationData> UNRELIABLE_ANIMATIONS = new ImmutableSet.Builder<AnimationData>()
            .add(AnimationData.RANGED_BLOWPIPE)
            .add(AnimationData.RANGED_BLAZING_BLOWPIPE)
            .add(AnimationData.MAGIC_EYE_OF_AYAK )
            .add(AnimationData.MAGIC_EYE_OF_AYAK_SPEC)
            .build();


    private static final Map<Integer, Integer> NON_STANDARD_MAGIC_WEAPON_SPEEDS =
            new ImmutableMap.Builder<Integer, Integer>()
                    .put(TWINFLAME_STAFF_WEAPON_ID, 6)
                    .build();

    // Map of problematic itemIds to equivalent working ones.
    // The Echo Venator Bow's ItemStats are returning null, so use the regular bow instead.
    private static final Map<Integer, Integer> WEAPON_ID_MAPPING_WORKAROUNDS =
            new ImmutableMap.Builder<Integer, Integer>()
                    .put(ECHO_VENATOR_BOW_WEAPON_ID, VENATOR_BOW_WEAPON_ID)
                    .build();


    // https://oldschool.runescape.wiki/w/Food/Fast_foods#Food_Delays
    // These constants are not to be confused with eat delay.
    private final int DEFAULT_FOOD_ATTACK_DELAY_TICKS = 3;
    private final int FAST_EAT_ATTACK_DELAY_TICKS = 2;

    public static final int EQUIPPING_MONOTONIC = 384; // From empirical testing this clientint seems to always increase whenever the player equips an item
    public static final Dimension DEFAULT_SIZE = new Dimension(DEFAULT_SIZE_UNIT_PX, DEFAULT_SIZE_UNIT_PX);


    // region subscribers

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged)
    {
        if (varbitChanged.getVarbitId() == Varbits.SPELLBOOK)
        {
            currentSpellBook = Spellbook.fromVarbit(varbitChanged.getValue());
        }
        if (varbitChanged.getVarpId() == VarPlayerID.SA_ENERGY)
        {
            specialPercentageEvents.addLast(varbitChanged.getValue());
        }
    }

    // onVarbitChanged happens when the user causes some interaction therefore we can't rely on some fixed
    // timing relative to a tick. A player can swap many items in the duration of the a tick.
    @Subscribe
    public void onVarClientIntChanged(VarClientIntChanged varClientIntChanged)
    {
        if (!config.enableMetronome()) return;
        final int currentMagicVarBit = client.getVarcIntValue(EQUIPPING_MONOTONIC);
        if (currentMagicVarBit <= lastEquippingMonotonicValue)
        {
            return;
        }
        lastEquippingMonotonicValue = currentMagicVarBit;

        // This windowing safe guards of from late swaps inside a tick, if we have already rendered the tick
        // then we shouldn't perform another attack.
        boolean preAttackWindow = attackState == AttackState.DELAYED_FIRST_TICK && renderedState != attackState;
        if (preAttackWindow)
        {
            // "Perform an attack" this is overwrites the last attack since we now know the user swapped
            // "Something" this tick, the equipped weapon detection will pick up specific weapon swaps. Even
            // swapping more than 1 weapon inside a single tick.
            performAttack();
        }
    }

    // onSoundEffectPlayed used to track spell casts, for when the player casts a spell on first tick coming
    // off cooldown, in some cases (e.g. ice barrage) the player will have no animation. Also they don't have
    // a projectile to detect instead :/
    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed event)
    {
        if (!config.enableMetronome()) return;
        // event.getSource() will be null if the player cast a spell, it's only for area sounds.
        soundEffectTick = client.getTickCount();
        soundEffectId = event.getSoundId();
    }

    @Subscribe
    protected void onFakeXpDrop(FakeXpDrop event)
    {
        if (!combatExpEarned.containsKey(event.getSkill()))
        {
            return;
        }
        combatExpEarned.get(event.getSkill()).addLast(event.getXp());
        if (attackState == AttackState.DELAYED_FIRST_TICK)
        {
            performAttack();
        }
    }

    @Subscribe
    protected void onStatChanged(StatChanged event)
    {
        if (!combatExpEarned.containsKey(event.getSkill()))
        {
            return;
        }
        combatExpEarned.get(event.getSkill()).addLast(event.getXp());
        if (attackState == AttackState.DELAYED_FIRST_TICK)
        {
            performAttack();
        }
    }

    // endregion

    @Provides
    AttackTimerMetronomeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AttackTimerMetronomeConfig.class);
    }

    private int computeDamage(AttackStyle attackStyle, AttackProcedure atkType, AnimationData curAnimation)
    {
        switch (atkType) {
            case POWERED_STAVE:
                // TODO not needed for any variable speed
                return -1;
            case MANUAL_AUTO_CAST:
                if (attackStyle == AttackStyle.DEFENSIVE_CASTING || attackStyle == AttackStyle.DEFENSIVE) {
                    // just use the defense exp to compute the damage
                    System.out.println("computeDamage (DEFENCE): " + Utils.getLastDelta(combatExpEarned.get(Skill.DEFENCE)) + " | " + combatExpEarned.get(Skill.DEFENCE));
                    return Utils.getLastDelta(combatExpEarned.get(Skill.DEFENCE));
                } else {
                    // deduct the fixed exp based on the spell
                    // (for now this only works for dark demon bane which awkwardly gives fractional exp)
                    var mageExp = Utils.getLastDelta(combatExpEarned.get(Skill.MAGIC));
                    if (curAnimation != AnimationData.MAGIC_ARCEUUS_DEMONBANE)
                    {
                        return -1;
                    }
                    System.out.println("computeDamage (MAGIC): " + Utils.getLastDelta(combatExpEarned.get(Skill.MAGIC)) + " | " + combatExpEarned.get(Skill.MAGIC));
                    return (int) Math.ceil(((double) mageExp - 43.5D) / 2.0D);
                }
            case MELEE_OR_RANGE:
                // TODO not needed for any variable speed
                return -1;
        }
        return -1;
    }

    private int getItemIdFromContainer(ItemContainer container, int slotID)
    {
        if (container == null)
        {
            return -1;
        }
        final Item item = container.getItem(slotID);
        return (item != null) ? item.getId() : -1;
    }

    private int getWeaponId()
    {
        int weaponId = getItemIdFromContainer(
                client.getItemContainer(InventoryID.WORN),
                EquipmentInventorySlot.WEAPON.getSlotIdx()
        );

        return WEAPON_ID_MAPPING_WORKAROUNDS.getOrDefault(weaponId, weaponId);
    }

    private ItemStats getWeaponStats(int weaponId)
    {
        return itemManager.getItemStats(weaponId);
    }

    private boolean getSalamanderAttack()
    {
        return client.getLocalPlayer().hasSpotAnim(SALAMANDER_SET_ANIM_ID);
    }

    private void setAttackDelay()
    {
        int weaponId = getWeaponId();
        AnimationData curAnimation = AnimationData.fromId(client.getLocalPlayer().getAnimation());
        PoweredStaves stave = PoweredStaves.getPoweredStaves(weaponId, curAnimation);
        boolean matchesSpellbook = matchesSpellbook(curAnimation);
        attackDelayHoldoffTicks = getWeaponSpeed(weaponId, stave, curAnimation, matchesSpellbook);
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

    private int getWeaponSpeed(int weaponId, PoweredStaves stave, AnimationData curAnimation, boolean matchesSpellbook)
    {
        var specDelta = Utils.getLastDelta(specialPercentageEvents);
        int damageDealt = -1;
        if (stave != null && stave.getAnimations().contains(curAnimation))
        {
            damageDealt = computeDamage(Utils.getAttackStyle(client), AttackProcedure.POWERED_STAVE, curAnimation);
            // We are currently dealing with a staves in which case we can make decisions based on the
            // spellbook flag. We can only improve this by using a deprecated API to check the projectile
            // matches the stave rather than a manual spell, but this is good enough for now.
            return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.POWERED_STAVE, damageDealt, specDelta, 4);
        }

        if (matchesSpellbook && isManualCasting(curAnimation))
        {
            damageDealt = computeDamage(Utils.getAttackStyle(client), AttackProcedure.MANUAL_AUTO_CAST, curAnimation);
            // You can cast with anything equipped in which case we shouldn't look to invent for speed.
            return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.MANUAL_AUTO_CAST, damageDealt, specDelta,getMagicBaseSpeed(weaponId));
        }

        damageDealt = computeDamage(Utils.getAttackStyle(client), AttackProcedure.MELEE_OR_RANGE, curAnimation);
        ItemStats weaponStats = getWeaponStats(weaponId);
        if (weaponStats == null)
        {
            return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.MELEE_OR_RANGE, damageDealt, specDelta, 4); // Assume barehanded == 4t
        }
        // Deadline for next available attack.
        return VariableSpeed.computeSpeed(client, curAnimation, AttackProcedure.MELEE_OR_RANGE, damageDealt, specDelta, weaponStats.getEquipment().getAspeed());
    }

    private static final List<Integer> SPECIAL_NPCS = Arrays.asList(10507, 9435, 9438, 9441, 9444); // Combat Dummy + Nightmare Pillars

    private boolean isPlayerAttacking()
    {
        int animationId = client.getLocalPlayer().getAnimation();
        if (AnimationData.isBlockListAnimation(animationId))
        {
            return false;
        }

        // Not walking is either any player animation or the edge cases which don't trigger an animation, e.g Salamander.
        boolean notWalking = animationId != -1 || getSalamanderAttack();

        // Testing if we are attacking by checking the target is more future
        // proof to new weapons which don't need custom code and the weapon
        // stats are enough.
        Actor target = client.getLocalPlayer().getInteracting();
        if (target != null && (target instanceof NPC))
        {
            final NPC npc = (NPC) target;
            boolean containsAttackOption = Arrays.stream(npc.getComposition().getActions()).anyMatch("Attack"::equals);
            Integer health = npcManager.getHealth(npc.getId());
            boolean hasHealthAndLevel = health != null && health > 0 && target.getCombatLevel() > 0;
            boolean attackingNPC = hasHealthAndLevel || SPECIAL_NPCS.contains(npc.getId()) || containsAttackOption;
            // just having a target is not enough the player may be out of range, we must wait for any
            // animation which isn't running/walking/etc
            return attackingNPC && notWalking;
        }
        if (target != null && (target instanceof Player))
        {
            return notWalking;
        }

        AnimationData fromId = AnimationData.fromId(animationId);
        // Do not use any animations from this set
        if (UNRELIABLE_ANIMATIONS.contains(fromId))
        {
            return false;
        }
        // fall back to animations.
        return fromId != null;
    }

    private boolean isManualCasting(AnimationData curId)
    {
        // If you use a weapon like a blow pipe which has an animation longer than it's cool down then cast an
        // ancient attack it wont have an animation at all. We can therefore need to detect this with a list
        // of sounds instead. This obviously doesn't work if the player is muted. ATM I can't think of a way
        // to detect this type of attack as a cast, only sound is an indication that the player is on
        // cooldown, melee attacks, etc will trigger an animation overwriting the last frame of the blowpipe's
        // idle animation.
        boolean castingFromSound = client.getTickCount() == soundEffectTick ? CastingSoundData.isCastingSound(soundEffectId) : false;
        boolean castingFromAnimation = AnimationData.isManualCasting(curId);
        return castingFromSound || castingFromAnimation;
    }

    private void performAttack()
    {
        attackState = AttackState.DELAYED_FIRST_TICK;
        setAttackDelay();
        tickPeriod = attackDelayHoldoffTicks;
        uiHideDebounceTickCount = UI_HIDE_DEBOUNCE_TICKS_MAX;
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
    private static final String BARBARIAN_POTIONS = "You drink the lumpy potion"; // barbarian potions https://oldschool.runescape.wiki/w/Barbarian_Training#Barbarian_potions
    private static final String JUG_OF_WINE = "You drink the wine"; // Wine https://oldschool.runescape.wiki/w/Jug_of_wine

    // Match only the start of the line with `^` and the Pattern.MULTILINE
    private static final Pattern EAT_MESSAGE = Pattern
            .compile("^(" + GENERIC_EAT + "|" + BARBARIAN_POTIONS + "|" + JUG_OF_WINE + ")", Pattern.MULTILINE & Pattern.CASE_INSENSITIVE);

    // gnome foods are also fast eats (Note these are not the food names as the wiki lists them, but the name
    // as written in chat), also pre-made and handmade have the same chat message.
    private static final String FAST_GNOME_FOOD = "worm hole|tangled toads legs|veg ball|chocolate bomb|worm crunchies|toad crunchies|"
            + "choc chip crunchies|spicy crunchies|fruit batta|cheese and tomato batta|toad batta|vegetable batta|worm batta";
    private static final String FAST_FOOD = "karambwan|halibut";
    // TODO ^ find out the food messages for https://oldschool.runescape.wiki/w/Crystal_paddlefish and https://oldschool.runescape.wiki/w/Corrupted_paddlefish
    private static final Pattern FAST_EAT = Pattern.compile("(" + FAST_FOOD + "|" + FAST_GNOME_FOOD + ")", Pattern.CASE_INSENSITIVE);

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!config.enableMetronome()) return;
        final String message = event.getMessage();

        if (EAT_MESSAGE.matcher(message).find())
        {
            int attackDelay = FAST_EAT.matcher(message).find() ?
                      FAST_EAT_ATTACK_DELAY_TICKS
                    : DEFAULT_FOOD_ATTACK_DELAY_TICKS;

            // We should always add eat delay
            pendingEatDelayTicks += attackDelay;
        }
        VariableSpeed.onChatMessage(event);
    }

    // onInteractingChanged is the driver for detecting if the player attacked out side the usual tick window
    // of the onGameTick events.
    @Subscribe
    public void onInteractingChanged(InteractingChanged interactingChanged)
    {
        if (!config.enableMetronome()) return;
        Actor source = interactingChanged.getSource();
        Actor target = interactingChanged.getTarget();

        Player p = client.getLocalPlayer();

        if (source.equals(p) && (target instanceof NPC))
        {
            switch (attackState)
            {
                case NOT_ATTACKING:
                    // If not previously attacking, this action can result in a queued attack or
                    // an instant attack. If its queued, don't trigger the cooldown yet.
                    if (isPlayerAttacking())
                    {
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
        if (!config.enableMetronome()) return;
        VariableSpeed.onGameTick(client, tick);
        boolean isAttacking = isPlayerAttacking();
        switch (attackState)
        {
            case NOT_ATTACKING:
                if (isAttacking)
                {
                    performAttack(); // Sets state to DELAYED_FIRST_TICK.
                }
                else
                {
                    uiHideDebounceTickCount--;
                }
                break;
            case DELAYED_FIRST_TICK:
                // we stay in this state for one tick to allow for 0-ticking
                attackState = AttackState.DELAYED;
                // fallthrough
            case DELAYED:
                if (attackDelayHoldoffTicks <= 0)
                { // Eligible for a new attack
                    if (isAttacking)
                    {
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
        if (specialPercentageEvents.size() > 5)
        {
            specialPercentageEvents.removeFirst();
        }
        for (var q : combatExpEarned.values())
        {
            if (q.size() > 5)
            {
                q.removeFirst();
            }
        }
    }


    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("attacktimermetronome"))
        {
            attackDelayHoldoffTicks = 0;
        }
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
        StringBuilder sb = new StringBuilder();
        // @formatter:off
        sb.append("tickPeriod: "); sb.append(this.tickPeriod);sb.append(SEPARATOR);
        sb.append("uiHideDebounceTickCount: "); sb.append(this.uiHideDebounceTickCount);sb.append(SEPARATOR);
        sb.append("attackDelayHoldoffTicks: "); sb.append(this.attackDelayHoldoffTicks);sb.append(SEPARATOR);
        sb.append("attackState: "); sb.append(this.attackState);sb.append(SEPARATOR);
        sb.append("renderedState: "); sb.append(this.renderedState);sb.append(SEPARATOR);
        sb.append("pendingEatDelayTicks: "); sb.append(this.pendingEatDelayTicks);sb.append(SEPARATOR);
        sb.append("currentSpellBook: "); sb.append(this.currentSpellBook);sb.append(SEPARATOR);
        sb.append("lastEquippingMonotonicValue: "); sb.append(this.lastEquippingMonotonicValue);sb.append(SEPARATOR);
        sb.append("soundEffectTick: "); sb.append(this.soundEffectTick);sb.append(SEPARATOR);
        sb.append("soundEffectId: "); sb.append(this.soundEffectId);sb.append("\n");
        // @formatter:on
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        outChannel.write(bytes);
    }
    private static final String SEPARATOR = ", ";
}
