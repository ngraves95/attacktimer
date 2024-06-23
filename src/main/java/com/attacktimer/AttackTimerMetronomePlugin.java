package com.attacktimer;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

import java.awt.*;

@PluginDescriptor(
        name = "Attack Timer Metronome",
        description = "Shows a visual cue on an overlay every game tick to help timing based activities",
        tags = {"timers", "overlays", "tick", "skilling"}
)
public class AttackTimerMetronomePlugin extends Plugin
{
    public enum AttackState {
        NOT_ATTACKING,
        DELAYED,
        //PENDING,
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

    public int tickPeriod = 0;

    final int ATTACK_DELAY_NONE = 0;

    private int uiUnshowDebounceTickCount = 0;
    private int uiUnshowDebounceTicksMax = 1;

    public int attackDelayHoldoffTicks = ATTACK_DELAY_NONE;

    public AttackState attackState = AttackState.NOT_ATTACKING;

    public Color CurrentColor = Color.WHITE;

    public int DEFAULT_SIZE_UNIT_PX = 25;

    private final int DEFAULT_FOOD_ATTACK_DELAY_TICKS = 3;
    private final int KARAMBWAN_ATTACK_DELAY_TICKS = 2;
    public Dimension DEFAULT_SIZE = new Dimension(DEFAULT_SIZE_UNIT_PX, DEFAULT_SIZE_UNIT_PX);

    @Provides
    AttackTimerMetronomeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AttackTimerMetronomeConfig.class);
    }

    private int getItemIdFromContainer(ItemContainer container, int slotID)
    {
        if (container == null) {
            return -1;
        }
        final Item item = container.getItem(slotID);
        return (item != null) ? item.getId() : -1;
    }

    private ItemStats getItemStatsFromContainer(ItemContainer container, int slotID)
    {
        if (container == null) {
            return null;
        }
        final Item item = container.getItem(slotID);
        return item != null ? itemManager.getItemStats(item.getId(), false) : null;
    }
    private ItemStats getWeaponStats()
    {
        return getItemStatsFromContainer(client.getItemContainer(InventoryID.EQUIPMENT),
                EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    private int getWeaponId() {
        return getItemIdFromContainer(client.getItemContainer(InventoryID.EQUIPMENT),
                EquipmentInventorySlot.WEAPON.getSlotIdx());
    }

    private AttackStyle getAttackStyle()
    {
        final int currentAttackStyleVarbit = client.getVarpValue(VarPlayer.ATTACK_STYLE);
        final int currentEquippedWeaponTypeVarbit = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        AttackStyle[] attackStyles = WeaponType.getWeaponType(currentEquippedWeaponTypeVarbit).getAttackStyles();

        if (currentAttackStyleVarbit < attackStyles.length) {
            return attackStyles[currentAttackStyleVarbit];
        }

        return AttackStyle.ACCURATE;
    }

    private int applyRangedAndMeleeRelicSpeed(int baseSpeed)
    {
        if (baseSpeed >= 4) {
            return baseSpeed / 2;
        } else {
            return (baseSpeed + 1) / 2;
        }
    }

    static final int BloodMoonSetAnimId = 2792;

    private boolean getBloodMoonProc()
    {
        return client.getLocalPlayer().hasSpotAnim(BloodMoonSetAnimId);
    }


    private int adjustSpeedForLeaguesIfApplicable(int baseSpeed)
    {
        int leagueRelicVarbit = 0;
        if (client.getWorldType().contains(WorldType.SEASONAL)) {
            leagueRelicVarbit = client.getVarbitValue(Varbits.LEAGUE_RELIC_4);
        }

        AttackStyle attackStyle = getAttackStyle();

        switch (leagueRelicVarbit) {
            case 0:
                // No league relic active - player does not have t4 relic or is not in leagues.
                return baseSpeed;
            case 1:
                // Archer's Embrace (ranged).
                if (attackStyle == AttackStyle.RANGING ||
                        attackStyle == AttackStyle.LONGRANGE) {
                    return applyRangedAndMeleeRelicSpeed(baseSpeed);
                }
                break;
            case 2:
                // Brawler's Resolve (melee)
                if (attackStyle == AttackStyle.ACCURATE ||
                        attackStyle == AttackStyle.AGGRESSIVE ||
                        attackStyle == AttackStyle.CONTROLLED ||
                        attackStyle == AttackStyle.DEFENSIVE) {
                    return applyRangedAndMeleeRelicSpeed(baseSpeed);
                }
                break;
            case 3:
                // Superior Sorcerer (magic)
                if (attackStyle == AttackStyle.CASTING ||
                        attackStyle == AttackStyle.DEFENSIVE_CASTING) {
                    return 2;
                }
                break;
        }

        return baseSpeed;
    }

    private int getWeaponSpeed()
    {
        boolean isPlayerCasting = isPlayerCasting();
        int itemId = getWeaponId();
        // We cannot use the weapon type to determine this as the shadow is also
        // a powered stave which has a 5-tick attack. Though weapon stats might
        // be better.
        boolean isFourTick = WeaponIds.FourTickStaves().contains(itemId);
        if (isPlayerCasting && !isFourTick)
        {
            // Unfortunately, the trident and toxic trident share animations with wave-tier spells.
            //
            // Assume that if the user is not wielding a trident and isCasting, they are autocasting
            // or manual casting from a spellbook.
            //
            // This assumption breaks if the user is manually casting a spell while wielding a trident
            // or toxic trident (unlikely).
            return adjustSpeedForLeaguesIfApplicable(5);
        }

        ItemStats weaponStats = getWeaponStats();
        if (weaponStats == null) {
            return adjustSpeedForLeaguesIfApplicable(4); // Assume barehanded == 4t
        }
        ItemEquipmentStats e = weaponStats.getEquipment();
        int speed = e.getAspeed();
        if (getAttackStyle() == AttackStyle.RANGING &&
            client.getVarpValue(VarPlayer.ATTACK_STYLE) == 1) { // Hack for index 1 => rapid
            speed -= 1; // Assume ranging == rapid.
        }

        if (getBloodMoonProc())
        { // Similar hack as rapid, blood moon saves a tick when it proc's
            speed -= 1;
        }

        return adjustSpeedForLeaguesIfApplicable(speed); // Deadline for next available attack.
    }

    private boolean isPlayerAttacking()
    {
        int animationId = client.getLocalPlayer().getAnimation();
        // Testing if we are attacking by checking the target is more future
        // proof to new weapons which don't need custom code and the weapon
        // stats are enough.
        Actor target = client.getLocalPlayer().getInteracting();
        if (target != null)
        {
            boolean isTargetDummy = target.getHealthRatio() == -1 && target.getCombatLevel() == 0;
            boolean attackingNPC = target.getCombatLevel() > 0 || isTargetDummy;
            boolean notWalking = animationId != -1;
            // just having a target is not enough the player may be out of
            // range, we must wait for any animation which isn't
            // running/walking/etc
            return attackingNPC && notWalking;
        }
        // fallback - might as well see if the hard-coded list has this animation
        return AnimationData.fromId(animationId) != null;
    }

    private boolean isPlayerCasting() {
        return AnimationData.isCasting(AnimationData.fromId(client.getLocalPlayer().getAnimation()));
    }

    private void performAttack()
    {
        attackState = AttackState.DELAYED;
        attackDelayHoldoffTicks = getWeaponSpeed();
        tickPeriod = attackDelayHoldoffTicks;
        uiUnshowDebounceTickCount = uiUnshowDebounceTicksMax;
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
        return (attackState == AttackState.DELAYED) || uiUnshowDebounceTickCount > 0;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        final String message = event.getMessage();

        if (message.startsWith("You eat") ||
                message.startsWith("You drink the wine")) {
            int attackDelay = (message.toLowerCase().contains("karambwan")) ?
                    KARAMBWAN_ATTACK_DELAY_TICKS :
                    DEFAULT_FOOD_ATTACK_DELAY_TICKS;

            if (attackState == AttackState.DELAYED) {
                attackDelayHoldoffTicks += attackDelay;
            }
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged interactingChanged)
    {
        Actor source = interactingChanged.getSource();
        Actor target = interactingChanged.getTarget();

        Player p = client.getLocalPlayer();

        if (source.equals(p) && (target instanceof NPC)) {

            switch (attackState) {
                case NOT_ATTACKING:
                    // If not previously attacking, this action can result in a queued attack or
                    // an instant attack. If its queued, don't trigger the cooldown yet.
                    if (isPlayerAttacking()) {
                       performAttack();
                    }
                    break;

                //case PENDING:
                case DELAYED:
                    // Don't reset tick counter or tick period.
                    break;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        boolean isAttacking = isPlayerAttacking(); // Heuristic for attacking based on animation.

        switch (attackState) {
            case NOT_ATTACKING:
                if (isAttacking) {
                    performAttack(); // Sets state to DELAYED.
                } else {
                    uiUnshowDebounceTickCount--;
                }
                break;
            case DELAYED:
                if (attackDelayHoldoffTicks <= 0) { // Eligible for a new attack
                    if (isAttacking) {
                        // Found an attack animation. Assume auto attack triggered.
                        performAttack();
                    } else {
                        // No attack animation; assume no attack.
                        attackState = AttackState.NOT_ATTACKING;
                    }
                }
        }

        attackDelayHoldoffTicks--;
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
}
