# Attack Timer 
A plugin to show an attack cooldown timer, like in other MMOs.

The plugin will automatically start a timer on attack.

Ticks until next attack may be enabled over your player's head.

![attack_timer_screenshot.png](attack_timer_screenshot.png)

## Lost-tick display

Enable **Only Count Lost Ticks** to hide the cooldown number and display only late attack ticks,
including when **Show Attack Cooldown Ticks** is off. The existing overhead font, size and position
settings still apply. The attack bar is controlled separately; turn **Show Attack Bar** off for a
number-only warning.

**Maximum Lost Ticks** defaults to 2 and can be set from 1 to 20. With the default, a missed attack
opportunity displays `+1`, followed by `+2` on the next tick, then hides until another attack is
detected. This is an upper display limit, not a minimum warning threshold. **Lost Tick Color**
defaults to red. Zero-based cooldown counting does not change the lost-tick numbers.

For example, after a five-tick Shadow attack, the number stays hidden through the cooldown. If no
new attack is detected on the first available attack tick, it shows `+1`. An on-time attack or
resuming attacks clears the warning. Known food delays extend the cooldown rather than counting as
lost ticks. Target death/despawn, player death, loading/logout, and plugin shutdown clear tracking.

This uses the plugin's existing client-side attack and cooldown detection. It measures potential
attack uptime lost, not whether waiting was a mistake: moving for mechanics, invulnerable targets,
and other intentional pauses can also produce a warning. It does not automate attacks.

## Updates

## 1.3.0 - 1.3.3

* Support for sailing cannons (7 tick)
* Fix bug with Guantlet Staves or Eye of Ayak being 5 tick when on the Ancient spellbook
* Support for Maggot King
  * Larvae (overwrite attack cooldown if attacked with a bow whilst flying)
  * Bile slowing attacks
  * Screech slowing attacks
* Support for Doom of Mokhaiotl
  * Larvae (overwrite attack cooldown if non-demonbane)
  * Volatile Earth (overwrite attack cooldown if non-demonbane)
  * Melee Punish (overwrite attack cooldown)

## 1.2.11 - 1.2.12

* Support for Royal Titans attack cooldown speedup when killing elementals
* Support for Amoxliatl Unstable Ice special being 1 tick attacks

## 1.2.8 - 1.2.10

* Fixes for Wyrmscraig (Hallowfell)
* Fix regression in 1.1, greater corruption triggering cooldown
* Red Keris Special Attack will now only be 8 ticks when landing the hit

## 1.2.3 - 1.2.7

* Blood moon rises support (Hallowed Flail, Kisten, Sunspear) - Stymphike Tartare
* Dragon crossbow in LMS
* Venator Bow Kit
* Demonic Pacts trident Kits

## 1.2.0

Overlay improvements!

* Option to have a different color for the last tick number (Fully customizable, White by default)
* Better defaults, Bold Font choice and more height choice
* HD bar support

Zero-based tick counting option: This means that for a 3-tick weapon, the count will be 2-1-0-2-1-0 instead of 3-2-1-3-2-1.

#### Speed Changing Support:

The plugin should now correctly account for some more cases where your speed changes:

* Scurrius bone weapons
* Tombs Of Amascut P4 warden skulls
* Tormented demons punish mechanic when only fighting 1 demon. More than 1 demon isn't supported.
* Eye of Ayak special attack
* Yama 🎉 The plugin should now correctly detect correctly executed shadow crash dodges and purging staff spec kills (might be broken in contracts)

**Bug fixes**:
* Added more animations to the block list which don't trigger the plugin to think your "attacking"
* Twinflame staff now 6t not 5t.
* Fixes some eats being missed and not accounted for. Including new Halibut fast food.
* Fix black gem keris and Infernal Tecpatl

## 1.1

Added support for a bunch more weapons:

* Dual Macuahuitl
* Blood moon set effect (1 tick reduction)
* Blue Moon Spear
* Atlat
* Tonalztics
* Elder maul
* Blister wood
* 4 Tick staves:
  * Sang & Kit
  * Trident enchanced (Swamp too)
  * Accursed Sceptre
  * CG staves
* Dhin's
* Misc specs:
    * Web weaver spec, Ursine chainmace spec, Bone dagger spec, Dscim spec, D2H spec, Ancient Mace spec,
      Arclight spec, Sara sword spec, Red keris, Bludgeon spec, Barrel chest anchor spec, Rune claws, Ballista
      spec (shared with heavy and light), Rune thrown axe

In theory if a weapon doesn't work just let us know
[here](https://github.com/ngraves95/attacktimer/issues/new), all weapons should work even after a game update
as the plugin now uses Runelite's API to get a weapons attack speed.

Add support for manual casting.

**Bug fixes**:
* Improved detection of equipped items
* Timer no longer going negative with no weapon equipped

## 1.0

The original version of the plugin.