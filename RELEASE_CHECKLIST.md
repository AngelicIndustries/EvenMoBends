# 0.0.3 preparation

Status checked 2026-10-03 on `release-prep-0.0.3`. Publishing is a separate maintainer step.

## Implemented

* Salamander is required. Models, main-body rendering, attachment transforms and animation evaluation use it.
  Native model objects remain as pose/Forge/skin-mod adapters; skin-mod-owned first-person paths are preserved.
* Player, armor, zombie/villager and spider geometry is supplied as reloadable GeckoLib resources.
* A valid CEM pack can take over the whole player or supported mob. Removing it restores Mo' Bends.
* Eating/drinking animation addresses issue #6, including Backhand's off hand and a config switch.
* The expression editor exports the shared Just Expressions / Fresh Moves skin layout: all supported eye presets,
  paired/wide/joined and outer-layer brows, eyelashes, colors, undo/redo and blink/gaze previews. Old format imports
  are removed; ambiguous corner data and unsupported combinations are rejected. Exports clear stale pack texels.
  Duplicate player and pig blinking renderers are removed.
* README, Blockbench documentation, English/Chinese editor instructions and the 0.0.3 changelog are updated.

## Verified

* `./gradlew build`: 147 tests passed with `EXPRESSION_PACKS` pointing at both original pack ZIPs.
  Without those optional local ZIPs, the pack contract test is skipped.
* Geometry/UV preservation, randomized native-to-Salamander transforms, armor/skin-layer visibility,
  eating pose selection and mirroring, skin export/import, CEM selection and reload fallback.
* Original Just Expressions 1.2.1 and Fresh Moves 3.1.1 Animated Eyes: classic/slim models compile through
  Salamander, and their eye, brow, outer-layer brow and eyelash UV slots match the editor, including pixel orientation.
* Development client with Wawel Auth, Backhand, EFR and Angelica: player/zombie/spider rendering, real main/offhand
  eating, editor navigation, each player pack's takeover, and return to Mo' Bends after pack removal.
  Used a new local test world; no skin was uploaded. Client was shut down and resource-pack settings restored.
* Expression-editor follow-up used a separate game directory and isolated offline Wawel Auth profile. Checked
  rejection of Miku's incompatible old metadata, loading current pack data, presets, arbitrary color entry,
  undo/redo, eyebrows, eyelashes, closed-eye preview, PNG export and the 640x480 layout. The in-game export
  exactly matches the prepared Miku PNG, with only reserved expression pixels changed.
* The prepared Miku face was visually checked with both Just Expressions and Fresh Moves in the development
  client's player preview; the extra black facial artifacts are absent. Active account/skin and normal game files
  were not changed. Pack animations still control final movement; the editor preview is an approximation.

## Before publishing

* Settle the public Salamander version. This branch builds against `8e5a945-snapshot`; older Salamander builds
  without the public CEM/skin APIs will not work. Pin the chosen release and add its minimum version to Forge's
  dependency declaration when a release version is available.
* Play-test the optional combinations after this renderer migration: SimpleSkinBackport, Aqua Acrobatics,
  Smart Moving, Just A Shield, 3D skin layers/capes, and remote players. The unit suite covers relevant adapters,
  but these combinations were not all launched during this pass. Check slim skins, armor, first person,
  swimming/crawling/rowing, pack-owned mob variants and equipment in those environments.
* Configure platform dependency relations for Salamander and its prerequisites. The existing 67 project is
  configured; CurseForge and Modrinth project IDs and publication relations are still unset.
* Review the changelog, choose/tag the release version, publish, and close issue #6 once the fix is released.
  No release tag, push, publication or issue comment/closure was performed in this preparation pass.

Older local branches and the swimming backup stash are preserved. AET, PastFootsteps and Salamander source were
not changed by this migration.
