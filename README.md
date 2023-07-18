# Mod Sets

This project heavily inspired by [ThatOrThis](https://github.com/EZForever/ThatOrThis)  
The mod is using for Minecraft Fabric/~~Quilt~~ for managing mod loading in game.   
And useful for modpack developer

## Feature

- Enable/Disable mods in game with defined mod sets/subdirectory name/mod id(Need restart)
- Using https://github.com/isXander/YetAnotherConfigLib for config screen
- Custom mod loading rules for modpack developer to make the pack with more user defined options

## Usage

The rules files should be json in `config/modsets/rules`.  
Add rule and enter the config screen through Mod Menu

## Mod Set
Mod set is entry defined in `config/modsets/modsets.json` that is string to set of mod id map.  
Specifically, sub folder name in `mods` folder will be mod sets the mods in it. And mod id will be the mod set only contains itself.   
Notice, the defined mod sets by config will override the folder/mod id with the same name.  
Example: 
```json
{
  // The `sodium` will target the two mods here. 
  // If disable this mod set, the mod in list won't load if it is exist
  "sodium": {
    "text": "Sodium",
    "tooltip": "Performance: +++++",
    "mods": [
      "sodium",
      "indium"
    ]
  }
}
```

## Rule
![img_1.png](https://github.com/SettingDust/ModSets/raw/main/img_1.png)
Every rule hold a text and tooltip for displaying the info you want.  
And a controller for YACL user interface
Example:

```json
{
  "text": {
    "text": "My first rule set",
    "bold": true,
    "color": "green"
  },
  "tooltip": "Awwww",
  "rules": [
    {
      "text": "text",
      "tooltip": "tooltip",
      "controller": {
        "type": "type"
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
  "tooltip": "Text Text",
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
  "tooltip": "Text Text",
  "controller": {
    "type": "boolean",
    "mod": "sodium"
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
  "tooltip": "Text Text",
  "controller": {
    "type": "cycling",
    "mods": [
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
  "tooltip": "Text Text",
  "controller": {
    "type": "mods_group",
    // Default is true
    "collapsed": false,
    "mods": [
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
  "tooltip": "Text Text",
  "controller": {
    "type": "rules_group",
    // Default is true
    "collapsed": true,
    "rules": [
      {
        "text": "text",
        "tooltip": "tooltip",
        "controller": {
          "type": "label"
        }
      },
      {
        "text": "text",
        "tooltip": "tooltip",
        "controller": {
          "type": "boolean",
          "mod": "sodium"
        }
      }
    ]
  }
}
```
  
Notice: Every text and tooltip is text raw json.
Using https://www.minecraftjson.com/ for generating text.
