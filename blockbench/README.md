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

Bones: `root` → `body` → `head`, `rightArm` → `rightForeArm`, `leftArm` → `leftForeArm`;
`root` → `rightLeg` → `rightForeLeg`, `leftLeg` → `leftForeLeg`. Forearms and forelegs hinge at the elbow and
knee exactly as in game.
