# Blockbench rig

`player.geo.json` (classic arms) and `player_slim.geo.json` (slim arms) are the Even Mo' Bends player model in
Bedrock/GeckoLib format, generated from `ModelBendsPlayer` by `PlayerRigTest`. Tests prove that Salamander
renders them vertex for vertex like the in-game model, at rest and posed, UVs included, so an arm or elbow bent in
Blockbench is the same bend in game.

Do not edit these files by hand. After changing the player model, regenerate them:

```
MOBENDS_UPDATE_RIG=1 ./gradlew test
```

## Using it

1. Blockbench: *File → Open Model* and pick `player.geo.json`, then drag a 64×64 skin onto the texture panel.
2. Animate in the *Animate* tab. Bone rotations are the in-game part rotations in degrees.

Bones: `root` → `body` → `neck` → `head`, `rightArmAim` → `rightArm` → `rightForeArm`,
`leftArmAim` → `leftArm` → `leftForeArm`; `root` → `rightLeg` → `rightForeLeg`, `leftLeg` → `leftForeLeg`.
Forearms and forelegs hinge at the elbow and knee exactly as in game.

Item bones: `rightForeArm` → `rightItem` (and `leftForeArm` → `leftItem`) sit at the grip where held items are
drawn; `root` → `rightItemFree` / `leftItemFree` sit at the same point at rest, for items that leave the hand. The
item is drawn at each of its bones whose scale isn't 0: the hand bone shows it unless scaled to 0, a free bone only
while an animation keys it (with a non-zero scale). Item bones start from the item's usual look in the hand; their
rotation and position turn and move it from there. They have no cubes; to see an item while animating, add a cube
under the bone in Blockbench (only the exported animation matters to the game).

The aim bones (`rightArmAim`, `leftArmAim`, `neck`) have no cubes. They turn the whole limb about its pivot, outside
its own rotation, so "point where the player looks" (aim X = `q.mobends_head_pitch`) combines cleanly with any arm
pose, such as the punch's twisted fist (`rightArm` = `[-90, 0, 120]`), and a head tilted back by `neck` still
follows the look with its own rotation.

## Animations

Player animations live in `src/main/resources/assets/mobends/geckolib/animations/player/`: `punch.animation.json`
(the empty-hand punch) and `item_test.animation.json` (`item_spin`, `item_throw`: examples of item bones). Salamander loads them through the resource manager, so a resource pack can replace any of
them at the same path and F3+T reloads it.

To edit one:

1. Open `player.geo.json` as above, switch to the *Animate* tab and use *Animation → Import Animations* on the file.
2. In the animation panel's *Variable Placeholders*, add the queries the file uses so the preview moves, e.g.
   `q.mobends_head_pitch = 0; q.mobends_head_yaw = 0;`
3. *Animation → Export Animations* into a resource pack, e.g.
   `resourcepacks/test/assets/mobends/geckolib/animations/player/punch.animation.json` (with a `pack.mcmeta`),
   enable the pack and press F3+T.

Time is seconds since the punch. `punch_right` / `punch_left` (3 s) snap the fist out, hold it until 0.5 s and
settle back into the stance by 1.1 s; the `_moving` versions (0.5 s) only animate the upper body, so the walk keeps
the legs.

Bones with motion (more than one keyframe, or Molang like following the look) follow their keyframes as authored. Bones with a single keyframe hold
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
