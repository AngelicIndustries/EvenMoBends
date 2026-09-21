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

Bones: `root` → `body` → `head`, `rightArmAim` → `rightArm` → `rightForeArm`,
`leftArmAim` → `leftArm` → `leftForeArm`; `root` → `rightLeg` → `rightForeLeg`, `leftLeg` → `leftForeLeg`.
Forearms and forelegs hinge at the elbow and knee exactly as in game.

The aim bones have no cubes. They turn the whole arm about the shoulder, outside the arm's own rotation, so
"point where the player looks" (aim X = `q.mobends_head_pitch`) combines cleanly with any arm pose, such as the
punch's twisted fist (`rightArm` = `[-90, 0, 120]`). In game they are folded into the arm's rotation.

## Animations

Player animations live in `src/main/resources/assets/mobends/geckolib/animations/player/` (currently only
`punch.animation.json`). Salamander loads them through the resource manager, so a resource pack can replace any of
them at the same path and F3+T reloads it.

To edit one:

1. Open `player.geo.json` as above, switch to the *Animate* tab and use *Animation → Import Animations* on the file.
2. In the animation panel's *Variable Placeholders*, add the queries the file uses so the preview moves, e.g.
   `q.mobends_head_pitch = 0; q.mobends_head_yaw = 0;`
3. *Animation → Export Animations* into a resource pack, e.g.
   `resourcepacks/test/assets/mobends/geckolib/animations/player/punch.animation.json` (with a `pack.mcmeta`),
   enable the pack and press F3+T.

Time is seconds since the animation started: the punch plays for 0.5 s from the hit, then the stance plays from
0 until 2.5 s. An animation gives the target pose; the parts ease toward it with Mo' Bends' usual smoothing. Bones and axes an
animation doesn't keyframe keep what earlier animations set. `root` moves and turns the whole body.

Queries: `q.mobends_head_pitch`, `q.mobends_head_yaw` (degrees, relative to the body), `q.mobends_ticks`,
`q.mobends_ticks_after_punch`.
