# Event publisher reference
## Information for plugin developers

Attack Timer posts its current state to the RuneLite event bus so other plugins can react to it without re-implementing attack detection.

> **⚠️ Note: use the events as supplemental information only, your plugin should never depend on Attack Timer.**
>
> Attack Timer works by inferring attacks from animations, sounds, experience drops and worn equipment. RuneLite does not expose when the game actually considers you to have attacked. It is sometimes wrong, late or missing, for example with unusual weapons, special attacks, lag or after the game changes.
>
> Don't build anything where an incorrect or missing value would cause an issue in your plugin. Always have a sensible fallback when the data is absent or implausible.
>
> Don't assume the events are posted at a specific moment in the game tick or in a timely manner. Events should never be assumed guaranteed to be delivered.

## The `attackTimerUpdate.v1` message

Namespace: `attacktimer`, name: `attackTimerUpdate.v1`. Each message is a full snapshot, and is sent when any of
the values below changes, so during an attack cooldown you will get one message per game tick.

Within a version, keys are never removed, renamed or given a different type or meaning, but new keys may be added at any time. Ignore keys you don't know, and treat every key as optional (a missing key is `null`).

| Key                    | Type      | Description                                                                                                                                          |
|------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ticksUntilNextAttack` | `Integer` | Game ticks until the player can attack again, `1` means they can attack on the next tick. `0` or lower means the cooldown elapsed without a new attack being detected: `0` is one tick late, `-1` two ticks late, and so on. It keeps counting down to a floor of `-19`. |
| `weaponPeriod`         | `Integer` | The attack speed in game ticks which the current cooldown started with (e.g. `4` for a whip, `5` for a Tumeken's shadow).                                    |
| `cooldownPending`      | `Boolean` | `true` while Attack Timer is showing an attack cooldown. When `false` the player isn't considered to be attacking, and the tick values are leftovers from the last attack. |
| `lastTargetName`       | `String`  | Name of the target of the last detected attack, `null` if there hasn't been one. It is not cleared when the fight ends, or when the target dies.     |
| `lastTargetNpcId`      | `Integer` | NPC id of that target, `-1` if there isn't one or it wasn't an NPC.                                                                                  |
| `lastTargetNpcIndex`   | `Integer` | NPC index of that target (distinguishes two NPCs with the same id/name), `-1` if there isn't one or it wasn't an NPC.                                |

## Versioning

The version is part of the message name (`attackTimerUpdate.v1`, `attackTimerUpdate.v2`, ...), so you choose the version your plugin is written for by matching that exact name. Subscribe to `PluginMessage` as usual and ignore any name you don't recognise, you won't be affected by newer versions.

- A new version is only made for a breaking change: removing or renaming a key, or changing the type, unit or meaning of one. Adding keys isn't breaking, it doesn't need a new version.
- Old versions are kept for a deprecation period. When a new version is added the previous one is still posted next to it, until the removal date listed in the table below.

| Version | Message name           | Status  | Removal |
|---------|------------------------|---------|---------|
| v1      | `attackTimerUpdate.v1` | Current | -       |

## Event delivery

- Updates are only sent when something changes. During a cooldown that is every game tick, and `ticksUntilNextAttack` keeps counting down after the cooldown ends, so expect up to ~20 more messages after an attack sequence finishes, then silence until the next attack.
- Nothing is sent when Attack Timer is disabled. If `cooldownPending` was `true` and the messages stop arriving, treat the data as stale.
- Messages are queued. If your subscriber (or any other plugins that share the thread) are too slow, the **oldest** queued messages are dropped. You can miss intermediate ticks, but because each message is a full snapshot the latest one you receive is still complete.
- Only the messages posted after your plugin starts are received, there's no replay. To catch up, wait for the next message.

## How to subscribe to events

The data is sent as a [`PluginMessage`](https://static.runelite.net/runelite-client/apidocs/net/runelite/client/events/PluginMessage.html), the RuneLite event for plugin hub plugins to send data to each other.

You don't need to depend on Attack Timer, just subscribe to `PluginMessage` and filter on the namespace and the full versioned name (see [Versioning](#versioning)). See `TestSubscriber` in [`AttackTimerEventPublisher.java`](src/main/java/com/attacktimer/AttackTimerEventPublisher.java) for a code example, or set `TEST_SUBSCRIBER_ENABLED` true to test without the need of a second plugin.

Output example (`attackTimerUpdate.v1`):
```
18:09:07: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=5, weaponPeriod=5}
18:09:08: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=4, weaponPeriod=5}
18:09:08: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=3, weaponPeriod=5}
18:09:09: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=2, weaponPeriod=5}
18:09:09: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=1, weaponPeriod=5}
18:09:10: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=5, weaponPeriod=5}
18:09:11: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=4, weaponPeriod=5}
18:09:11: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=7, weaponPeriod=5}
18:09:11: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=6, weaponPeriod=5}
18:09:12: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=5, weaponPeriod=5}
18:09:12: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=4, weaponPeriod=5}
18:09:13: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=3, weaponPeriod=5}
18:09:14: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=2, weaponPeriod=5}
18:09:14: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=1, weaponPeriod=5}
18:09:15: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=5, weaponPeriod=5}
18:09:15: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=4, weaponPeriod=5}
18:09:16: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=3, weaponPeriod=5}
18:09:17: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=2, weaponPeriod=5}
18:09:17: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=1, weaponPeriod=5}
18:09:18: {cooldownPending=true, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=0, weaponPeriod=5}
18:09:18: {cooldownPending=false, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=-1, weaponPeriod=5}
18:09:19: {cooldownPending=false, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=-2, weaponPeriod=5}
18:09:20: {cooldownPending=false, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=-3, weaponPeriod=5}
18:09:20: {cooldownPending=false, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=-4, weaponPeriod=5}
18:09:21: {cooldownPending=false, lastTargetName=Kalphite Queen, lastTargetNpcId=965, lastTargetNpcIndex=42259, ticksUntilNextAttack=-5, weaponPeriod=5}
...
```