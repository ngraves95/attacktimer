# Attack Timer 
A plugin to show an attack cooldown timer, like in other MMOs.

The plugin will automatically start a timer on attack.

Ticks until next attack may be enabled over your player's head.

![attack_timer_screenshot.png](attack_timer_screenshot.png)

## Updates

## 1.2.5

* Blood moon rises support (Hallowed Flail, Sunspear)
* Dragon crossbow in LMS
* Venator Bow Kit

## 1.2

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