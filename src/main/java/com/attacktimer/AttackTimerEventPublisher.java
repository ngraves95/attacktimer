package com.attacktimer;

/*
 * Copyright (c) 2026, 40 Cakes <https://github.com/40Cakes>
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

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 // AttackTimerEventPublisher shares the plugin's attack timer state with other plugins by posting
 // PluginMessages to the RuneLite event bus. See README-EventPublisher.md for more information.
@Singleton
class AttackTimerEventPublisher
{
    private static final Logger log = LoggerFactory.getLogger(AttackTimerEventPublisher.class);

    static final String NAMESPACE = "attacktimer";
    static final String UPDATE_MESSAGE_NAME = "attackTimerUpdate";
    static final String KEY_TICKS_UNTIL_NEXT_ATTACK = "ticksUntilNextAttack";
    static final String KEY_WEAPON_PERIOD = "weaponPeriod";
    static final String KEY_COOLDOWN_PENDING = "cooldownPending";
    static final String KEY_LAST_TARGET_NAME = "lastTargetName";
    static final String KEY_LAST_TARGET_NPC_ID = "lastTargetNpcId";
    static final String KEY_LAST_TARGET_NPC_INDEX = "lastTargetNpcIndex";

    static final int NOT_AN_NPC = -1;
    static final int QUEUE_CAPACITY = 8;
    private static final String THREAD_NAME = "attacktimer-event-publisher";

    // Debug: set true to enable a subscriber inside this plugin that listens to what is posted and logs it
    static final boolean TEST_SUBSCRIBER_ENABLED = true;

    private final EventBus eventBus;
    private final TestSubscriber testSubscriber = new TestSubscriber();
    private ThreadPoolExecutor executor;

    private boolean hasPublished = false;
    private int lastTicksUntilNextAttack;
    private int lastWeaponPeriod;
    private boolean lastCooldownPending;
    private String lastTargetName;
    private int lastTargetNpcId;
    private int lastTargetNpcIndex;

    @Inject
    AttackTimerEventPublisher(final EventBus eventBus)
    {
        this.eventBus = eventBus;
    }

     // Create the worker thread. Until it is called, `publish` is a no-op
    synchronized void start()
    {
        if (executor != null)
        {
            return;
        }

        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable ->
                {
                    final Thread thread = new Thread(runnable, THREAD_NAME);
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
        hasPublished = false;
        if (TEST_SUBSCRIBER_ENABLED)
        {
            eventBus.register(testSubscriber);
        }
    }


     // Discard anything still queued and interrupt the worker
    synchronized void stop()
    {
        if (executor == null)
        {
            return;
        }
        if (TEST_SUBSCRIBER_ENABLED)
        {
            eventBus.unregister(testSubscriber);
        }
        executor.shutdownNow();
        executor = null;
    }

     // Post the given state to the bus if it differs from the last state which was posted
    synchronized void publish(final int ticksUntilNextAttack, final int weaponPeriod, final boolean cooldownPending,
                              @Nullable final Actor lastTarget)
    {
        if (executor == null)
        {
            return;
        }
        try
        {
            final String targetName = lastTarget == null ? null : lastTarget.getName();
            final int npcId = lastTarget instanceof NPC ? ((NPC) lastTarget).getId() : NOT_AN_NPC;
            final int npcIndex = lastTarget instanceof NPC ? ((NPC) lastTarget).getIndex() : NOT_AN_NPC;

            if (hasPublished
                    && ticksUntilNextAttack == lastTicksUntilNextAttack
                    && weaponPeriod == lastWeaponPeriod
                    && cooldownPending == lastCooldownPending
                    && npcId == lastTargetNpcId
                    && npcIndex == lastTargetNpcIndex
                    && Objects.equals(targetName, lastTargetName))
            {
                return;
            }
            hasPublished = true;
            lastTicksUntilNextAttack = ticksUntilNextAttack;
            lastWeaponPeriod = weaponPeriod;
            lastCooldownPending = cooldownPending;
            lastTargetName = targetName;
            lastTargetNpcId = npcId;
            lastTargetNpcIndex = npcIndex;

            final Map<String, Object> data = new HashMap<>();
            data.put(KEY_TICKS_UNTIL_NEXT_ATTACK, ticksUntilNextAttack);
            data.put(KEY_WEAPON_PERIOD, weaponPeriod);
            data.put(KEY_COOLDOWN_PENDING, cooldownPending);
            data.put(KEY_LAST_TARGET_NAME, targetName);
            data.put(KEY_LAST_TARGET_NPC_ID, npcId);
            data.put(KEY_LAST_TARGET_NPC_INDEX, npcIndex);

            final PluginMessage message = new PluginMessage(NAMESPACE, UPDATE_MESSAGE_NAME,
                    Collections.unmodifiableMap(data));

            executor.execute(() -> post(message));
        }
        catch (RuntimeException e)
        {
            log.debug("Failed to publish attack timer update", e);
        }
    }

    private void post(final PluginMessage message)
    {
        try
        {
            eventBus.post(message);
        }
        catch (RuntimeException e)
        {
            log.warn("Failed to post attack timer update", e);
        }
    }

    public static final class TestSubscriber
    {
        @Subscribe
        public void onPluginMessage(final PluginMessage message)
        {
            if (!NAMESPACE.equals(message.getNamespace()) || !UPDATE_MESSAGE_NAME.equals(message.getName()))
            {
                return;
            }
            log.info("[attacktimer test subscriber] {}/{} on thread '{}': {}", message.getNamespace(),
                    message.getName(), Thread.currentThread().getName(), new TreeMap<>(message.getData()));
        }
    }
}