# Blockbench rig

`player.bbmodel` / `player_slim.bbmodel` (Blockbench projects, for editing) and `player.geo.json` /
`player_slim.geo.json` (Bedrock/GeckoLib geometry) are the Even Mo' Bends player model, generated from
`ModelBendsPlayer` by `BlockbenchProjectTest` and `PlayerRigTest`. Tests prove that Salamander
renders them vertex for vertex like the in-game model, at rest and posed, UVs included, so an arm or elbow bent in
Blockbench is the same bend in game.

Do not edit these files by hand. After changing the player model, regenerate them:

```
MOBENDS_UPDATE_RIG=1 ./gradlew test
```

## Using it

1. Blockbench: *File → Open Model* and pick `player.bbmodel` (or `player_slim.bbmodel`). It is the rig as a
   Blockbench project, with the Molang placeholders already filled in.
2. Replace the grey placeholder skin with yours: drag your 64×64 skin onto the "skin (replace with yours)" texture in
   the texture panel (or right-click it and load a file).
3. Animate in the *Animate* tab. Bone rotations are the in-game part rotations in degrees.

Preview items: `rightItem` and `rightItemFree` each hold a *preview sword* and a *preview grass block* group, sized and
placed exactly as the game draws a held tool and a held block. Toggle them with the eye icon in the outliner (the
sword in the hand is on by default). They are only for looking at; the game reads nothing but the exported
animations. (`player.geo.json` is the same rig without them, as plain Bedrock geometry.)

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
- `item_test.animation.json`: `item_spin` and `item_throw`, examples of item bones. Salamander loads them through the resource manager, so a resource pack can replace any of
them at the same path and F3+T reloads it.

To edit one:

1. Open `player.bbmodel` as above, switch to the *Animate* tab and use *Animation → Import Animations* on the file.
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
`q.mobends_ticks_after_punch`.

## Trying an animation in game

`/evenmobends play <file> <animation>` loops an animation on your own player on top of everything else, e.g.
`/evenmobends play player/item_test item_throw` while holding a sword. Tab completes files and names;
`/evenmobends stop` ends it. After exporting over a resource pack's file, F3+T reloads it while it keeps playing.
