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
  Both moving strokes retain a folded pull and an extended reach. The elbow opens before the shoulder returns
  inward, keeping the recovery away from the face, but retains a soft bend through the return instead of locking
  straight. The elbow opening follows the hand's inward path so it cannot briefly push the hand outward again
  during recovery. Extension finishes after the shoulder turns into the next outward pull. Beyond 35 degrees of
  head yaw, Molang gradually shortens the inward reach and opens the elbow further; looking ahead keeps the fuller stroke. `swim_holding` leaves extra
  room for the item, with its rotation following the shoulder and elbow at the original grip. The period, body
  motion, kick, and idle animations are unchanged.
- `dive.animation.json`: swimming fully underwater. `dive` (upper body; `dive_holding` if present, for an occupied
  hand) leans with the travel direction through `q.mobends_travel_pitch`; the legs play `dive_legs_still` and
  `dive_legs` over it as strongly as the player moves (full kick from 0.18 blocks per tick).
  Both `dive` and `dive_holding` use the same coordinated recovery, retaining the underwater stroke's extended
  reach, folded pull, and travel-dependent body tilt. The elbows ease into extension without bending backward:
  the forearm's edge-mounted hinge would otherwise expose an open gap at the elbow.
- `stand.animation.json`: `stand`, looping on the player's age (breathing, arm sway).
- `walk.animation.json` / `sprint.animation.json`: `walk` / `sprint` and `walk_swing` / `sprint_swing`. These are
  timed by the stride, not the clock: one loop of the animation is one stride (vanilla's limb swing times 0.6662
  radians), so it keeps pace with the player's speed whatever the animation's length (the lengths are the usual
  cadence, for previewing). `_swing` holds the arms' and legs' swing at full strength and plays over the main one as
  strongly as the limbs swing. Switching between stand, walk and sprint blends over 0.25 s.
- `sneak.animation.json`: `sneak_walk` / `sneak_walk_swing` replace the walk while sneaking on the move; `sneak` is
  the crouch over standing, held at the stride's phase where the player stopped, and uses `q.mobends_keep` for the
  axes it leaves to the stand.
- `fall.animation.json`: `fall`, looping on the player's age.
- `climb.animation.json`: `climb` (holding on) and `climb_move` over it as fast as the player climbs (full stroke
  from 0.12 blocks per tick).
- `fly.animation.json`: the hover `fly` (or `fly_elytra` gliding with Et Futurum elytra) and `fly_forward` /
  `fly_elytra_forward` over it as fast as the player flies (fully forward from 0.35 blocks per tick).
- `ride.animation.json`: `ride` sitting still; moving, `ride_move` with `ride_move_jiggle` over it by the limbs' swing.
- `jump.animation.json`: timed from liftoff and holding its last frame. `jump`; while walking, `jump_walk` (and
  `jump_walk_swing`) keep the legs striding over it; sprinting, `jump_sprint_right` / `jump_sprint_left` by the leg
  leading this jump.
- `sword.animation.json`: swinging anything but a pickaxe, axe or block, timed from the click (0.5 s, holding the
  last frame). The combo clicks through `sword_1`, `sword_2`, `sword_3` (the spin) and back to `sword_1`;
  `sword_1_moving` / `sword_2_moving` leave the legs to walking, and play (without `root`) while riding.
  `sword_2` and `sword_3` start where the swing before them was 7 ticks in (a quick click), so switches within the
  combo run as the swing is keyed. From anywhere else, where the body was fades out over its first 0.15 s: the
  swing keeps its shape without snapping to its first frame. After a swing, standing still settles into
  `sword_stance`, sprinting into `sword_stance_sprint` (walking has none). The sword trail follows the blade as it
  is drawn, so it traces whatever a swing does with the arm and the item.
- `bow.animation.json`: timed from when the player started drawing. `bow_draw` for the first 0.5 s, then `bow_aim`,
  which holds its last frame (the bow arm follows the look down, rising no higher than 120 degrees). Both fade in
  from where the body was, like the swings.
- `mine.animation.json` / `axe.animation.json`: the stance `mine` / `axe` while holding a pickaxe or a block / an
  axe, and `mine_swing` / `axe_swing` over it while swinging, looping on the player's age (1.8 swings a second).
  In both axe animations the left hand holds the handle a hand's width below the right, rolling the shoulder
  forward where the arm can't reach (baked by `AxeGrip`; `ToolAnimationTest` checks it stays on the handle).
- With something in Backhand's off hand, punches only use the main hand, and punches, sword swings and stances,
  mining and the axe play without the left arm's bones (`leftArmAim`, `leftArm`, `leftForeArm`, `leftItem`,
  `leftItemFree`), so that arm keeps holding its item.
- `block.animation.json`: `block_right` / `block_left`, over everything else while that hand blocks.
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

Axes with motion (keyframes that change, or Molang like following the look) are exactly where their keyframes put
them. Axes whose keyframes all hold one value ease into it, even when the bone's other axes move. Bones with a single keyframe hold
that pose and ease into it at Mo' Bends' usual rate, which is what blends a punch in from walking or standing.
Bones and axes an animation doesn't keyframe keep what earlier animations set. `root` moves and turns the whole
body.

Bone positions work on every bone: moving `rightForeArm` detaches the hand (held item included) from the elbow.
Positions an animation stops setting ease back to zero.

Queries: `q.mobends_head_pitch`, `q.mobends_head_yaw` (degrees, relative to the body), `q.mobends_ticks`,
`q.mobends_ticks_after_punch`, `q.mobends_holding` (1 with something in the main hand, else 0),
`q.mobends_offhand_holding` (1 with something in Backhand's off hand, else 0),
`q.mobends_travel_pitch` (degrees the dive leans below horizontal along the travel direction, within +-35, eased).
An axis set to `q.mobends_keep` isn't applied: it keeps what the animations before this one set (e.g. a crouch that
bends the legs forward but leaves their sideways turn to the walk underneath).

`MovementAnimationTest` keeps stand, walk, sprint and sneak within 1.5 degrees of their Java originals (bake with
`MOBENDS_BAKE_MOVEMENT=1`, which overwrites those four files); `AirAnimationTest` does the same for fall, climb,
fly, ride and jump (`MOBENDS_BAKE_AIR=1`), and `CombatAnimationTest` for the sword swings, stances and blocking
(`MOBENDS_BAKE_COMBAT=1`), `ToolAnimationTest` for the bow, mining and axe (`MOBENDS_BAKE_TOOLS=1`), which also checks a whole combo against the Java: within 45 degrees through the
switches (where the fade eases differently from the Java's per-part rates), and within 1.5 once back in the
stance. Bakes derive their tracks from what the Java animation sets: axes it leaves alone become `q.mobends_keep`,
and axes that follow the look get it as Molang. A Java pre-rotation becomes the limb's aim bone (`neck`,
`rightArmAim`, `leftArmAim`), which turns the limb at the same pivot just before its own rotation.

The swim and dive files started as bakes of the old Java animations. Their moving arms are now deliberately
reauthored. `SwimAnimationTest` and `DiveAnimationTest` retain the 1.5-degree parity checks for the unchanged
parts; idle swimming still has full parity. `HoldingSwimAnimationTest` checks hand/item clearance, loop closure,
that the stroke still bends for the pull and extends for the reach, a continuous inward hand recovery, and dive
elbows that do not hyperextend.
Do not re-bake with `MOBENDS_BAKE_SWIM=1` or `MOBENDS_BAKE_DIVE=1`: baking replaces the entire file and discards
both the edited empty-hand strokes and the `_holding` variants.

## Trying an animation in game

`/evenmobends play <file> <animation>` loops an animation on your own player on top of everything else, e.g.
`/evenmobends play player/item_test item_throw` while holding a sword. Tab completes files and names;
`/evenmobends stop` ends it. After exporting over a resource pack's file, F3+T reloads it while it keeps playing.
