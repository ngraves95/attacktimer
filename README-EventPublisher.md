# EventPublish reference
## Information for plugin developers

Attack Timer posts its current state to the RuneLite event bus so other plugins can react to it without re-implementing attack detection.

> **⚠️ Note: use the events as supplemental information only, your plugin should never depend on Attack Timer.**
>
> Attack Timer works by inferring attacks from animations, sounds, experience drops and worn equipment. RuneLite does not expose when the game actually considers you to have attacked. It is sometimes wrong, late or missing, for example with unusual when the game changes.
>
> Don't build anything where an incorrect or missing value would cause an issue in your plugin. Always have a sensible fallback when the data is absent or implausible.
> 
> Don't assume the events are posted at a specific moment in the game tick or in a timely manner. Events should never be assumed guaranteed to be delivered.

## The `attackTimerUpdate` message

Namespace: `attacktimer`, name: `attackTimerUpdate`. Each message is a full snapshot, and is sent when any of
the values below changes, so during an attack cooldown you will get one message per game tick.

Treat every key as optional (a missing key is `null`), future versions of Attack Timer may add, or change keys without notice.

| Key                    | Type      | Description                                                                                                                                          |
|------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ticksUntilNextAttack` | `Integer` | Game ticks until the player can attack again, `1` means they can attack on the next tick. `0` or lower means the cooldown elapsed without a new attack being detected: `0` is one tick late, `-1` two ticks late, and so on. It keeps counting down to a floor of `-19`. |
| `weaponPeriod`         | `Integer` | The attack speed in game ticks which the current cooldown started with (e.g. `4` for a whip, `5` for a Tumeken's shadow).                                    |
| `cooldownPending`      | `Boolean` | `true` while Attack Timer is showing an attack cooldown. When `false` the player isn't considered to be attacking, and the tick values are leftovers from the last attack. |
| `lastTargetName`       | `String`  | Name of the target of the last detected attack, `null` if there hasn't been one. It is not cleared when the fight ends.                              |
| `lastTargetNpcId`      | `Integer` | NPC id of that target, `-1` if there isn't one or it wasn't an NPC.                                                                                  |
| `lastTargetNpcIndex`   | `Integer` | NPC index of that target (distinguishes two NPCs with the same id/name), `-1` if there isn't one or it wasn't an NPC.                                |

## Event delivery

- Updates are only sent when something changes. During a cooldown that is every game tick, and `ticksUntilNextAttack` keeps counting down after the cooldown ends, so expect up to ~20 more messages after an attack sequence finishes, then silence until the next attack.
- Nothing is sent when Attack Timer is disabled. If `cooldownPending` was `true` and the messages stop arriving, treat the data as stale.
- Messages are queued. If your subscriber (or any other plugins that share the thread) are too slow, the **oldest** queued messages are dropped. You can miss intermediate ticks, but because each message is a full snapshot the latest one you receive is still complete.
- Only the messages posted after your plugin starts are received, there's no replay. To catch up, wait for the next message.

## How to subscribe to events

The data is sent as a [`PluginMessage`](https://static.runelite.net/runelite-client/apidocs/net/runelite/client/events/PluginMessage.html), the RuneLite event for plugin hub plugins to send data to each other.

You don't need to depend on Attack Timer, just subscribe to `PluginMessage` and filter on the namespace and name. See `TestSubscriber` in [`AttackTimerEventPublisher.java`](src/main/java/com/attacktimer/AttackTimerEventPublisher.java) for a code example, or set `TEST_SUBSCRIBER_ENABLED` true to test without the need of a second plugin.

Output example:
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
18:09:21: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-5, weaponPeriod=5}
18:09:21: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-6, weaponPeriod=5}
18:09:22: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-7, weaponPeriod=5}
18:09:23: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-8, weaponPeriod=5}
18:09:23: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-9, weaponPeriod=5}
18:09:24: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-10, weaponPeriod=5}
18:09:24: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-11, weaponPeriod=5}
18:09:25: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-12, weaponPeriod=5}
18:09:26: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-13, weaponPeriod=5}
18:09:26: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-14, weaponPeriod=5}
18:09:27: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-15, weaponPeriod=5}
18:09:27: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-16, weaponPeriod=5}
18:09:28: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-17, weaponPeriod=5}
18:09:29: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-18, weaponPeriod=5}
18:09:29: {cooldownPending=false, lastTargetName=null, lastTargetNpcId=-1, lastTargetNpcIndex=42259, ticksUntilNextAttack=-19, weaponPeriod=5}
```