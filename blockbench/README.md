# Blockbench rig

`player.geo.json` (classic arms) and `player_slim.geo.json` (slim arms) are the Even Mo' Bends player model in
Bedrock/GeckoLib format, generated from `ModelBendsPlayer` by `PlayerRigTest`. For animating, export a Blockbench
project from the game instead (below). Tests prove that Salamander
renders them vertex for vertex like the in-game model, at rest and posed, UVs included, so an arm or elbow bent in
Blockbench is the same bend in game.

Do not edit these files by hand. After changing the player model, regenerate them:

```
MOBENDS_UPDATE_RIG=1 ./gradlew test
```

## Using it

1. In game, run `/evenmobends blockbench` (add `slim` or `classic` to override the arm width detected from your
   skin). It writes `.minecraft/mobends/blockbench/player.bbmodel` (or `player_slim.bbmodel`): the rig with your own
   skin, the Molang placeholders filled in, and preview items made 1:1 from your game's textures.
2. Blockbench: *File → Open Model* and pick that file.
3. Animate in the *Animate* tab. Bone rotations are the in-game part rotations in degrees.

Preview items: `rightItem` and `rightItemFree` each hold a *preview sword* (an iron sword, extruded pixel by pixel
the way the game draws held items) and a *preview grass block*, sized and placed exactly as the game draws a held tool
and a held block. Toggle them with the eye icon in the outliner (the sword in the hand is on by default). They are
only for looking at; the game reads nothing but exported animations. Blockbench's Bedrock format allows one texture,
so the project's texture is your skin with the item textures below it.

`player.geo.json` / `player_slim.geo.json` are the bare rig as plain Bedrock geometry.

Bones: `root` → `body` → `neck` → `head`, `rightArmAim` → `rightArm` → `rightForeArm`,
`leftArmAim` → `leftArm` → `leftForeArm`; `root` → `rightLeg` → `rightForeLeg`, `leftLeg` → `leftForeLeg`.
Forearms and forelegs hinge at the elbow and knee exactly as in game.

Item bones: `rightForeArm` → `rightItem` (and `leftForeArm` → `leftItem`) sit at the grip where held items are
drawn; `root` → `rightItemFree` / `leftItemFree` sit at the same point at rest, for items that leave the hand. The
item is drawn at each of its bones whose scale isn't 0: the hand bone shows it unless scaled to 0, a free bone only
while an animation keys it (with a non-zero scale). Item bones start from the item's usual look in the hand; their
rotation and position turn and move it from there. In the Blockbench projects they hold the toggleable preview items.

The aim bones (`rightArmAim`, `leftArmAim`, `neck`) have no cubes. They turn the whole limb about its pivot, outside
its own rotation, so "point where the player looks" (aim X = `q.mobends_head_pitch`) combines cleanly with any arm
pose, such as the punch's twisted fist (`rightArm` = `[-90, 0, 120]`), and a head tilted back by `neck` still
follows the look with its own rotation.

## Animations

Player animations live in `src/main/resources/assets/mobends/geckolib/animations/player/`:

- `punch.animation.json`: the empty-hand punch.
- `swim.animation.json`: surface swimming. `swim` (moving) and `swim_idle` animate the upper body, including the held
  item's swing on `rightItem`; `swim_legs` and `swim_idle_legs` kick at their own speed and play at the same time.
  Time is the player's age, looping each animation; switching between moving and idle blends over 0.5 s.
  With something in the hand, `swim_holding` / `swim_idle_holding` (and `swim_legs_holding` /
  `swim_idle_legs_holding`) are used instead when the file has them: duplicate an animation in Blockbench and rename
  the copy with `_holding` to give the occupied hand its own stroke. Switching also blends over 0.5 s.
- `dive.animation.json`: swimming fully underwater. `dive` (upper body; `dive_holding` if present, for an occupied
  hand) leans with the travel direction through `q.mobends_travel_pitch`; the legs play `dive_legs_still` and
  `dive_legs` over it as strongly as the player moves (full kick from 0.18 blocks per tick).
- `item_test.animation.json`: `item_spin` and `item_throw`, examples of item bones. Salamander loads them through the resource manager, so a resource pack can replace any of
them at the same path and F3+T reloads it.

To edit one:

1. Open your exported `player.bbmodel` as above, switch to the *Animate* tab and use *Animation → Import Animations* on the file.
2. Change the *Variable Placeholders* (already listing the queries) to preview other head angles.
3. *Animation → Export Animations* into a resource pack, e.g.
   `resourcepacks/test/assets/mobends/geckolib/animations/player/punch.animation.json` (with a `pack.mcmeta`),
   enable the pack and press F3+T.

Time is seconds since the punch. `punch_right` / `punch_left` (3 s) snap the fist out, hold it until 0.5 s and
settle back into the stance by 1.1 s; the `_moving` versions (0.5 s) only animate the upper body, so the walk keeps
the legs.

Bones with motion (more than one keyframe, or Molang like following the look) are exactly where their keyframes put
them. Bones with a single keyframe hold
that pose and ease into it at Mo' Bends' usual rate, which is what blends a punch in from walking or standing.
Bones and axes an animation doesn't keyframe keep what earlier animations set. `root` moves and turns the whole
body.

Bone positions work on every bone: moving `rightForeArm` detaches the hand (held item included) from the elbow.
Positions an animation stops setting ease back to zero.

Queries: `q.mobends_head_pitch`, `q.mobends_head_yaw` (degrees, relative to the body), `q.mobends_ticks`,
`q.mobends_ticks_after_punch`, `q.mobends_holding` (1 with something in the main hand, else 0),
`q.mobends_travel_pitch` (degrees the dive leans below horizontal along the travel direction, within +-35, eased).

The swim and dive files were baked from the old Java animations (`SwimAnimationTest`, `DiveAnimationTest`, run with
`MOBENDS_BAKE_SWIM=1` / `MOBENDS_BAKE_DIVE=1`), whose parity checks keep them within 1.5 degrees of those. Once you
change them on purpose, update or drop the parity check.

## Trying an animation in game

`/evenmobends play <file> <animation>` loops an animation on your own player on top of everything else, e.g.
`/evenmobends play player/item_test item_throw` while holding a sword. Tab completes files and names;
`/evenmobends stop` ends it. After exporting over a resource pack's file, F3+T reloads it while it keeps playing.
