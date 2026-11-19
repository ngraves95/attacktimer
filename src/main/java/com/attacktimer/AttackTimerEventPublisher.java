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
import java.util.function.Function;
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
    // The name of a message is "<base name>.<version>", e.g. "attackTimerUpdate.v1", consumers pick the
    // version they were written against by matching the full name. See "Versioning" in README-EventPublisher.md
    static final String UPDATE_MESSAGE_BASE_NAME = "attackTimerUpdate";
    static final String UPDATE_V1_NAME = UPDATE_MESSAGE_BASE_NAME + ".v1";

    // v1 keys
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
    static final boolean TEST_SUBSCRIBER_ENABLED = false;

    // The state of the plugin which gets published, every version of the message is built from this
    static final class State
    {
        final int ticksUntilNextAttack;
        final int weaponPeriod;
        final boolean cooldownPending;
        @Nullable
        final Target lastTarget;

        State(final int ticksUntilNextAttack, final int weaponPeriod, final boolean cooldownPending,
              @Nullable final Target lastTarget)
        {
            this.ticksUntilNextAttack = ticksUntilNextAttack;
            this.weaponPeriod = weaponPeriod;
            this.cooldownPending = cooldownPending;
            this.lastTarget = lastTarget;
        }

        boolean matches(final int ticksUntilNextAttack, final int weaponPeriod, final boolean cooldownPending,
                        @Nullable final Target lastTarget)
        {
            return this.ticksUntilNextAttack == ticksUntilNextAttack
                    && this.weaponPeriod == weaponPeriod
                    && this.cooldownPending == cooldownPending
                    && Objects.equals(this.lastTarget, lastTarget);
        }
    }

    static final class Target
    {
        @Nullable
        final String name;
        final int npcId;
        final int npcIndex;

        Target(@Nullable final String name, final int npcId, final int npcIndex)
        {
            this.name = name;
            this.npcId = npcId;
            this.npcIndex = npcIndex;
        }

        @Nullable
        static Target of(@Nullable final Actor actor)
        {
            if (actor == null)
            {
                return null;
            }
            final boolean isNpc = actor instanceof NPC;
            return new Target(actor.getName(), isNpc ? ((NPC) actor).getId() : NOT_AN_NPC,
                    isNpc ? ((NPC) actor).getIndex() : NOT_AN_NPC);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Target))
            {
                return false;
            }
            final Target other = (Target) o;
            return npcId == other.npcId && npcIndex == other.npcIndex && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name, npcId, npcIndex);
        }
    }

    static final class MessageVersion
    {
        final String name;
        final Function<State, Map<String, Object>> encoder;

        MessageVersion(final String name, final Function<State, Map<String, Object>> encoder)
        {
            this.name = name;
            this.encoder = encoder;
        }
    }

    // Every version in here is posted on every change. To make a breaking change to the message add a new
    // version to this list (keeping the old one for the deprecation period), don't change an existing one
    static final List<MessageVersion> SUPPORTED_VERSIONS = Collections.unmodifiableList(Arrays.asList(
            new MessageVersion(UPDATE_V1_NAME, AttackTimerEventPublisher::encodeV1)));

    private final EventBus eventBus;
    private final List<MessageVersion> versions;
    private final TestSubscriber testSubscriber = new TestSubscriber();
    private ThreadPoolExecutor executor;

    private State lastState = null;
    private final Map<String, Map<String, Object>> lastPosted = new HashMap<>();

    @Inject
    AttackTimerEventPublisher(final EventBus eventBus)
    {
        this(eventBus, SUPPORTED_VERSIONS);
    }

    AttackTimerEventPublisher(final EventBus eventBus, final List<MessageVersion> versions)
    {
        this.eventBus = eventBus;
        this.versions = versions;
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
        resetPublished();
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

    // Post the given state to the bus if it differs from the last state which was posted, in every version
    synchronized void publish(final int ticksUntilNextAttack, final int weaponPeriod, final boolean cooldownPending,
                              @Nullable final Target lastTarget)
    {
        if (executor == null)
        {
            return;
        }
        try
        {
            if (lastState != null && lastState.matches(ticksUntilNextAttack, weaponPeriod, cooldownPending, lastTarget))
            {
                return;
            }
            final State state = new State(ticksUntilNextAttack, weaponPeriod, cooldownPending, lastTarget);

            final List<PluginMessage> messages = new ArrayList<>(versions.size());
            for (final MessageVersion version : versions)
            {
                final Map<String, Object> data = version.encoder.apply(state);
                if (data.equals(lastPosted.get(version.name)))
                {
                    continue;
                }
                lastPosted.put(version.name, data);
                messages.add(new PluginMessage(NAMESPACE, version.name, Collections.unmodifiableMap(data)));
            }
            lastState = state;

            if (!messages.isEmpty())
            {
                executor.execute(() -> post(messages));
            }
        }
        catch (RuntimeException e)
        {
            resetPublished();
            log.debug("Failed to publish attack timer update", e);
        }
    }

    private void resetPublished()
    {
        lastState = null;
        lastPosted.clear();
    }

    private static Map<String, Object> encodeV1(final State state)
    {
        // v1
        final Target target = state.lastTarget;
        final Map<String, Object> data = new HashMap<>();
        data.put(KEY_TICKS_UNTIL_NEXT_ATTACK, state.ticksUntilNextAttack);
        data.put(KEY_WEAPON_PERIOD, state.weaponPeriod);
        data.put(KEY_COOLDOWN_PENDING, state.cooldownPending);
        data.put(KEY_LAST_TARGET_NAME, target == null ? null : target.name);
        data.put(KEY_LAST_TARGET_NPC_ID, target == null ? NOT_AN_NPC : target.npcId);
        data.put(KEY_LAST_TARGET_NPC_INDEX, target == null ? NOT_AN_NPC : target.npcIndex);
        return data;
    }

    private void post(final List<PluginMessage> messages)
    {
        for (final PluginMessage message : messages)
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
    }

    public static final class TestSubscriber
    {
        @Subscribe
        public void onPluginMessage(final PluginMessage message)
        {
            if (!NAMESPACE.equals(message.getNamespace()) || !UPDATE_V1_NAME.equals(message.getName()))
            {
                return;
            }
            log.info("[attacktimer test subscriber] {}/{} on thread '{}': {}", message.getNamespace(),
                    message.getName(), Thread.currentThread().getName(), new TreeMap<>(message.getData()));
        }
    }
}