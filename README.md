# Return to Zero

**English** | [Português](#português)

**Per-player death rewind for Fabric 1.20.1, inspired by *Re:Zero*.**

Per-player death checkpoints for Fabric **1.20.1**. When a player would die, they return to
their last checkpoint instead — and only the blocks **they** placed or broke by hand since then are undone. Other
players and the rest of the world are left alone.

> **Status: beta.** Built for small LAN/friends groups. Test it on a copy of your world first.

## Features

- **Per-player checkpoint:** position, dimension, health, hunger, XP and inventory.
- **Undo your own blocks:** blocks the player placed/broke by hand since the checkpoint are restored, but only if
  nobody changed them afterwards.
- **Automatic checkpoints** every N minutes (default 10) and on first join.
- **Saved to disk** (`<world>/return_to_zero.dat`), so checkpoints survive restarts.
- **Optional [Sanity: Renewed](https://modrinth.com/mod/sanity-renewed) integration:** the player loses some sanity on
  revive, with a configurable minimum sanity they come back with.
- **Config file and in-game screen** (Mod Menu + Cloth Config, both optional).

## Commands

| Command | Who | What it does |
|---|---|---|
| `/zero` | everyone | create your checkpoint now |
| `/zero voltar` (or `/zero back`) | everyone | go back to your checkpoint (no sanity loss) |
| `/zero config intervalo [min]` (`interval`) | op | automatic checkpoint interval (0 = off) |
| `/zero config sanidade [0-100]` (`sanity`) | op | sanity lost on revive |
| `/zero config piso [0-100]` (`floor`) | op | minimum sanity after revive (default 50) |
| `/zero config chance [0-100]` | op | chance for advancements to be granted (100 = normal) |
| `/zero config blocos [true/false]` (`blocks`) | op | enable/disable undoing the player's blocks |
| `/zero config recarregar` (`reload`) | op | reload the config file |

Run a `config` option without a value to see its current value.

`/kill` is intentionally **not** intercepted and still kills for real.

## Configuration

`config/return_to_zero.json` (created on first launch), or **Mod Menu → Return to Zero → Config**.
In a LAN game the **host's** configuration is the one that counts.

## Known limitations

- Only blocks placed/broken **by hand** are undone. Explosions, fire, water, pistons, redstone and mobs are not tracked.
- Dropped items on the ground are not removed (possible duplication if a player drops items before dying).
- For doors, beds and tall plants only one half may be undone.
- The block log is kept in memory only; it is lost when the game closes (the player checkpoint itself is saved).
- Every player on a LAN/server needs the same mod version.

## Building

Requires **JDK 17+** and **Gradle 8.8+** (internet needed on the first build).

```
gradle build
```

The jar is written to `build/libs/return_to_zero-<version>.jar` (use the one **without** `-sources`).
The included GitHub Actions workflow builds it automatically on every push.

## Credits and license

Based on **rbd 1.0.0** ("Return By Death") by "Me!", released under **CC0 1.0**. The code in this repository is a
rewrite, released under **CC0 1.0 Universal** as well — see [LICENSE](LICENSE).

---

## Português

**Retorno por morte, por jogador, para Fabric 1.20.1, inspirado em *Re:Zero*.**

Checkpoints de morte **por jogador** para Fabric **1.20.1**. Quando o jogador morreria, ele
volta ao último checkpoint, e só os blocos que **ele mesmo** colocou ou quebrou à mão desde então são desfeitos.
Os outros jogadores e o resto do mundo não são afetados.

> **Status: beta.** Feito para grupos pequenos de amigos em LAN. Teste primeiro numa cópia do seu mundo.

### Recursos

- **Checkpoint por jogador:** posição, dimensão, vida, fome, XP e inventário.
- **Desfaz os seus próprios blocos:** os que o jogador colocou ou quebrou à mão desde o checkpoint voltam ao que
  eram, mas só se ninguém mexeu neles depois.
- **Checkpoints automáticos** a cada N minutos (padrão 10) e na primeira entrada no mundo.
- **Salvos em arquivo** (`<mundo>/return_to_zero.dat`), então sobrevivem a fechar o jogo.
- **Integração opcional com o [Sanity: Renewed](https://modrinth.com/mod/sanity-renewed):** o jogador perde um pouco
  de sanidade ao reviver, e volta com uma sanidade mínima configurável.
- **Arquivo de config e tela dentro do jogo** (Mod Menu + Cloth Config, ambos opcionais).

### Comandos

`/zero` cria o checkpoint, `/zero voltar` volta a ele, e `/zero config ...` configura (só operador). Sem valor, cada opção mostra o valor atual. Veja a tabela em inglês acima. O `/kill` **não** é interceptado.

### Configuração

`config/return_to_zero.json` (criado na primeira vez que o jogo abre), ou **Mod Menu → Return to Zero →
Configurar**. Em LAN vale a configuração de quem é o **host**.

### Limites conhecidos

- Só blocos colocados/quebrados **à mão** são desfeitos. Explosões, fogo, água, pistões, redstone e criaturas, não.
- Itens soltos no chão não somem (pode haver duplicação se o jogador dropar itens antes de morrer).
- Em portas, camas e plantas altas, só uma das metades pode ser desfeita.
- O registro de blocos fica só na memória; o checkpoint do jogador é salvo, mas o registro de blocos se perde ao fechar.
- Todos os jogadores precisam ter a mesma versão do mod.

### Como compilar

Precisa de **JDK 17 ou mais novo** e **Gradle 8.8 ou mais novo** (com internet na primeira vez):

```
gradle build
```

O jar fica em `build/libs/return_to_zero-<versão>.jar` (use o que **não** tem `-sources`). O workflow do GitHub
Actions incluído compila automaticamente a cada push.

### Créditos e licença

Baseado no **rbd 1.0.0** ("Return By Death") de "Me!", sob **CC0 1.0**. O código deste repositório é uma reescrita,
também sob **CC0 1.0 Universal** — veja o [LICENSE](LICENSE).
