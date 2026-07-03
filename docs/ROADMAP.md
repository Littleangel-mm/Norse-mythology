# Norse Mythology Mod Roadmap

## Project Name

Norse Mythology / 北欧神话模组

## Completed Features

### Gungnir / 冈格尼尔

- Creative mode obtainable weapon.
- Gold-colored item name and custom tooltip.
- Throwable spear weapon with loyalty-style return for players.
- Applies permanent bleeding on hit.
- Calls down crimson divine lightning on hit.
- Crimson lightning uses custom damage/effects and does not trigger vanilla lightning mutation behavior.
- Homing/guaranteed-hit behavior against nearby valid targets.
- Can be equipped and used by Einherjar.

### Fenrir / 芬里尔

- Boss entity with spawn egg.
- 750 max health.
- Large wolf-based body, approximately five times normal wolf scale.
- Custom model, texture, glow/enraged visuals, and reference assets.
- Does not naturally despawn.
- Does not attack players by default.
- Enters enraged state only after being attacked by a player.
- Chases and attacks the player while enraged.
- Hunts hostile mobs at night without entering enraged state.

### Einherjar / 英魂

- Humanoid warrior entity with spawn egg.
- 20 max health.
- Starts with an iron sword.
- Starts with food and eats to recover health.
- Attacks hostile mobs.
- Protects and assists players in combat.
- Supports player command targeting: when the player attacks a target, nearby Einherjar focus that target.
- Can pick up weapons, armor, and food.
- Internal weapon inventory keeps backup weapons instead of throwing them away.
- Uses the highest-priority weapon available.
- Supports swords, axes, bows, tridents, and Gungnir.
- Bow shots and thrown tridents/Gungnir do not friendly-fire other Einherjar.
- Equipment preservation logic: weapons, backup weapons, and armor drop intact on death instead of being randomly damaged by vanilla mob equipment drops.

## Planned Features

### Valkyrie / 女武神

- Flying or semi-flying warrior unit.
- Uses spear, sword, or bow.
- Can act as an elite commander for Einherjar.
- Potential support abilities: heal allies, buff Einherjar, rescue low-health warriors.
- Needs model, texture, spawn egg, animations, and AI.

### Hel / 海拉

- Underworld-themed boss.
- Can summon undead minions.
- Applies anti-healing or life-drain effects.
- May create a death-domain aura around the battlefield.
- Needs model, texture, boss skills, boss bar, and loot table.

### Jormungandr / 耶梦加得

- World Serpent boss.
- Extremely large serpent entity.
- Poison breath, constriction, area poison clouds, and large-scale body attacks.
- Technical challenge: segmented body model, long-body animation, and collision handling.

### Ymir / 冰霜巨人尤尼尔

- Frost giant boss or elite giant.
- High health and heavy knockback.
- Frost attacks, freezing aura, ice spikes, or snowstorm ability.
- Needs giant model, frost texture, combat skills, and loot.

### Yggdrasil / 世界树

- Large world structure or divine tree system.
- Could provide teleportation, blessings, revival, quests, or progression.
- Needs structure files, custom blocks, interaction logic, and possibly UI.

### Nidhogg / 毒龙尼德霍格

- Poison dragon boss.
- Poison mist, corrosion, flying/crawling movement, and root/world-tree corruption theme.
- Can be connected to the Yggdrasil progression system.
- Needs dragon model, animations, poison area logic, and boss behavior.

### Surtr / 火焰巨人苏尔特尔

- Endgame fire giant boss.
- Flaming sword, lava field, burning charge, and apocalypse-themed attacks.
- Could serve as a late-game raid or final boss.
- Needs fire effects, model, boss bar, loot, and arena behavior.

### Loki / 诡计之神洛基

- Trickster boss or special NPC.
- Illusions, clones, invisibility, target confusion, and deception mechanics.
- Can summon monsters or manipulate the battlefield.
- Technical focus: clone AI, fake targets, special skill logic, and encounter scripting.

## Near-Term Development Tasks

- Verify Einherjar command targeting against Fenrir and other custom bosses.
- Add boss bars for Fenrir and future boss entities.
- Improve Fenrir animations and combat feedback.
- Add proper loot tables for Gungnir, Fenrir, and Einherjar-related progression.
- Decide whether Valkyrie or Hel should be developed next.
