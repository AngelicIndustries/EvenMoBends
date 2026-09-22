# Even Mo' Bends

Maintainable Minecraft 1.7.10 fork of Mo' Bends 0.20.1 with Wawel Auth and Smart Moving compat.

<!-- ![logo](images/logo.png) -->

[![hub](images/badges/github.png)](https://github.com/JackOfNoneTrades/EvenMoBends/releases)
[![67](images/badges/67.png)](https://67.fentanylsolutions.org/mod/even-mo-bends)
![forge](images/badges/forge.png)
[![cord](images/badges/cord.png)](https://discord.gg/xAWCqGrguG)

<!--
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/even-mo-bends)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/even-mo-bends)
[![maven](images/badges/maven.png)](https://maven.fentanylsolutions.org/#/releases/net/gobbob/mobends/EvenMoBends)
-->

## Compatibility

| Installed mods | Player renderer | Mob animations |
| --- | --- | --- |
| Even Mo' Bends only | Mo' Bends | Enabled |
| WawelAuth + Even Mo' Bends | Mo' Bends with WawelAuth skins and modern-skin support | Enabled |
| SimpleSkinBackport + Even Mo' Bends | Mo' Bends with modern skins, slim arms and segmented skin layers | Enabled |
| Backhand + Just A Shield + Even Mo' Bends | Mo' Bends with per-hand blocking poses, including passive shield blocking | Enabled |
| Aqua Acrobatics + Even Mo' Bends | Mo' Bends limbs with AA's swimming/crawling poses and whole-body transforms | Enabled |
| GTNH Et Futurum Requiem + Even Mo' Bends | Mo' Bends with EFR-aware Elytra flight and rowing poses | Enabled |
| Smart Moving (with or without WawelAuth) | Smart Moving / Smart Render; Mo' Bends player features disabled | Enabled |

EFR boat drivers follow each paddle independently, including turns and coasting. Boats, chest boats,
and rafts use their own handle and seat positions; rear passengers sit without rowing.
The paddles settle into an advanced resting position for a more relaxed arm pose, with eased starts and stops.
Set `animations.player.rowing=false` in `mobends.cfg` to use the ordinary riding pose instead.

Aqua Acrobatics supplies the swimming/crawling state and body rotation; Mo' Bends animates the segmented limbs.
Stopping or touching the bottom keeps AA's prone pose, and ordinary movement in water uses upright treading.
Set `animations.player.crawling=false` to disable the crawl limb cycle while keeping the low pose needed to fit.
The `swimming` animation switch also controls AA's swimming limb cycle.

## Blinking

Player skins can opt in to blinking with the embedded blink marker and frames documented by ETF's
[player skin guide](https://github.com/Traben-0/Entity_Texture_Features/blob/ETF-Main/.github/README-assets/SKIN_GUIDE.md#blinking---v230).
Even Mo' Bends implements only the blinking part of that convention. Sleeping and blindness keep the eyes closed.
By default, intermediate eyelid frames are generated and eased closed and open for a smoother, Fresh Moves-style
blink without adding a second player body rig. Set `blinking.smoothEyelids=false` to use the authored frames directly.

Currently only pigs blink. A resource pack can override a pig's generated
closed-eye texture by placing `_blink` before `.png`, with an optional `_blink2.png` half-closed frame. When Angelica
Random Mobs is enabled, these companions are resolved after the pig variant, so `pig2.png` can use `pig2_blink.png`.

Player blinking, smooth eyelids, pig blinking, blink frequency, and blink length are independently configurable in
`mobends.cfg`.

An ingame editor accessible from the Even Mo' Bends menu allows to easily create blinking-capable skins.

## Building

```sh
./gradlew build
```

It can additionally load makamys' Smart Moving stack without making it a published dependency:

```sh
./gradlew -PsmartMoving runClient25
```

To test SimpleSkinBackport instead of Wawel Auth, supply an SSB development jar:

```sh
./gradlew -PsimpleSkinBackportJar=/path/to/simpleskinbackport-dev.jar runClient25
```

Wawel Auth remains available for compilation and tests, but is excluded from this client's runtime.

Backhand is always in the development client. To test it with Just A Shield, supply a Just A Shield development jar:

```sh
./gradlew -PjustAShieldJar=/path/to/targaseule-dev.jar runClient25
```

This can be combined with `-PsimpleSkinBackportJar=...`.

To test Aqua Acrobatics, supply its development jar. This can be combined with the options above:

```sh
./gradlew -PaquaAcrobaticsJar=/path/to/aquaacrobatics-dev.jar runClient25
```

## Credits

* [Iwo Plaza / GobBob](https://www.curseforge.com/minecraft/mc-mods/mo-bends), creator of Mo' Bends.
* [makamys](https://github.com/makamys/SmartMoving), maintainer of the Minecraft 1.7.10 Smart Moving fork used for compatibility testing.
* [GTNH Et Futurum Requiem](https://github.com/GTNewHorizons/Et-Futurum-Requiem), whose optional API supplies Elytra flight and boat paddle state.
* [Entity Texture Features](https://github.com/Traben-0/Entity_Texture_Features), whose documented player-skin blink convention is supported.
* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10).

See [UPSTREAM.md](UPSTREAM.md) for exact source and binary provenance.

## License

Mixed MIT and GPL-3.0-or-later; see [LICENSE](LICENSE) for details.

## Buy me some creatine

* [ko-fi.com](https://ko-fi.com/jackisasubtlejoke)
* Monero: `893tQ56jWt7czBsqAGPq8J5BDnYVCg2tvKpvwTcMY1LS79iDabopdxoUzNLEZtRTH4ewAcKLJ4DM4V41fvrJGHgeKArxwmJ`
