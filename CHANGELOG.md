# Changelog

## 1.3.0
- Renamed to **Return to Zero** (id `return_to_zero`, package `dev.returntozero`).
- Commands are now `/zero`, `/zero voltar` (or `back`) and `/zero config ...` instead of `/cp`.
- Config file is now `config/return_to_zero.json`; checkpoints are saved in `<world>/return_to_zero.dat`.

## 1.2.0 (as Player Checkpoint)
- Renamed the project to Player Checkpoint.
- Replaced the custom sound and icon with a vanilla sound and an original icon.
- Config file is now `config/player_checkpoint.json`; checkpoints are saved in `<world>/player_checkpoint.dat`.

## 1.1.x (as `rbd`)
- Per-player checkpoints (position, dimension, health, hunger, XP, inventory); the world is no longer rewound for everyone.
- Undo only the blocks the dying player placed or broke by hand since their checkpoint.
- Sanity: Renewed integration (sanity loss and a minimum sanity on revive), written directly through reflection.
- Checkpoints survive closing the game.
- In-game config screen (Mod Menu + Cloth Config) and `/cp` commands (now `/zero`).

## 1.0.0 (original `rbd`, CC0)
- Original mod by "Me!" that this project is based on.
