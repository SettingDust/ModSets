# Mod Sets

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/mod-sets?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/mod-sets) [![Modrinth Version](https://img.shields.io/modrinth/v/mod-sets?style=for-the-badge&logo=modrinth&label=Latest)](https://modrinth.com/mod/mod-sets/versions) [![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/mod-sets?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/mod-sets/versions)
[![](https://cf.way2muchnoise.eu/full_890349_Downloads.svg?badge_style=for_the_badge)](https://www.curseforge.com/minecraft/mc-mods/mod-sets) [![](https://cf.way2muchnoise.eu/versions/890349.svg?badge_style=for_the_badge)](https://www.curseforge.com/minecraft/mc-mods/mod-sets)
[![GitHub](https://img.shields.io/github/license/SettingDust/ModSets?style=for-the-badge&logo=github)](https://github.com/SettingDust/ModSets) [![GitHub issues](https://img.shields.io/github/issues-raw/SettingDust/ModSets?style=for-the-badge&logo=github)](https://github.com/SettingDust/ModSets/issues)
[![Discord](https://img.shields.io/discord/1134045695020642334?style=for-the-badge&logo=discord&label=Discord)](https://discord.gg/exzXHb4aQj)

This project heavily inspired by [ThatOrThis](https://github.com/EZForever/ThatOrThis)
The mod is using for Minecraft Fabric/Quilt/Forge for managing mod loading in game.
And useful for modpack developer

## Feature

- Enable/Disable mods in game with defined mod sets/subdirectory name/mod id(Need restart)
- Using https://github.com/isXander/YetAnotherConfigLib for config screen
- Custom mod loading rules for modpack developer to make the pack with more user defined options

## Usage

The rules files should be json in `config/modsets/rules`.
Add rule and enter the config screen through Mod Menu
Full example: https://github.com/SettingDust/ModSets/blob/main/example_rules.json

## Mod Set

Mod set is entry defined in `config/modsets/modsets.json` that is string to set of mod id map.
Specifically, sub folder name in `mods` folder will be mod sets the mods in it. And mod id will be the mod set only
contains itself.
Notice, the defined mod sets by config will override the folder/mod id with the same name.
Example:
```json
{
    // The `sodium` will target the two mods here.
  // If disable this mod set, the mod in list won't load if it is exist
  "sodium": {
    "text": "Sodium",
    "description": "Performance: +++++",
    "mods": [
      "sodium",
      "indium"
    ]
  }
}
```

## Rule
![img_1.png](https://github.com/SettingDust/ModSets/raw/main/img_1.png)
Every rule hold a text and description for displaying the info you want.
And a controller for YACL user interface
Example:

```json
{
  "text": {
    "text": "My first rule set",
    "bold": true,
    "color": "green"
  },
  "description": "Awwww",
  "rules": [
    {
      "text": "text",
      "description": "description",
      "controller": {
        "type": "type" // Notice, this isn't valid type of controller. Take the type from controllers below
      }
    }
  ]
}
```

## Controllers

### Label

For displaying text on screen
Example:

```json
{
  "text": "The label",
  "description": "Text Text",
  "controller": {
    "type": "label"
  }
}
```

### Boolean

For switching a single mod set on/off
Example:
```json
{
  "text": "The boolean",
  "description": "Text Text",
  "controller": {
    "type": "boolean",
      "id": "sodium"
  }
}
```

### Cycling

For switching between mod sets in a list.
Useful when there is conflicting mods such as sodium and optifabric. Or switching the pack difficult
Example:
```json
{
  "text": "The cycling",
  "description": "Text Text",
  "controller": {
    "type": "cycling",
      "ids": [
          // Don't empty
      "sodium",
      "optifabric"
    ]
  }
}
```

### Mods Group
Assign a simple boolean controller option to every mods in the mod sets.
Example:

```json
{
  "text": "The mods group",
  "description": "Text Text",
  "controller": {
    "type": "mods_group",
    // Default is true
    "collapsed": false,
      "ids": [
          // Don't empty
      "sodium",
      "optifabric"
    ]
  }
}
```
### Rules Group
Group the rules so that it's able to collapse the rules
Example:

```json
{
  "text": "The rules group",
  "description": "Text Text",
  "controller": {
    "type": "rules_group",
    // Default is true
    "collapsed": true,
    "rules": [ // Don't empty
      {
        "text": "text",
        "description": "description",
        "controller": {
          "type": "label"
        }
      },
      {
        "text": "text",
        "description": "description",
        "controller": {
          "type": "boolean",
            "id": "sodium"
        }
      }
    ]
  }
}
```

Notice: Every text and description is text raw json.
Using https://www.minecraftjson.com/ for generating text.
