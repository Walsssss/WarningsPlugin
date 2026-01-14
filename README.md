# Walson WarningsPlugin
A lightweight warnings system for Paper servers with a focus on **staff usability**. 
Warn players, keep a history of their infractions, and review server-wide warning statistics in-game.

Author: Walson

## COMMANDS
| Command                               | Description                                       |
|---------------------------------------|---------------------------------------------------|
| `/warn <player> <reason>`             | Warn a player.                                    |
| `/warns <player>`                     | View a player's warns (GUI for players, text for console). |
| `/removewarn <player> <number\|all>`  | Remove one or all warnings from a player.        |
| `/walsonwarn panel`                   | Open the main warnings panel GUI.                |
| `/walsonwarn reload`                  | Reload configuration and messages.               |
| `/walsonwarn help`                    | Show plugin command help.                        |

Permissions can be found on [here](https://github.com/Walsssss/WarningsPlugin/blob/main/src/main/resources/plugin.yml)

# Walson Warnings Plugin



- No automatic punishments – **purely informational** for staff and players.
- GUI-based browsing for staff, console-friendly text output for logs.
- All data stored in a single, readable `warnings.yml` file. No Database needed. 

## Info
- **Server:** Paper (or forks)  
  - Compiled against `paper-api:1.20.4-R0.1-SNAPSHOT`  
  - Intended for use on **1.20.4+**, including **1.21**.
- **Java:** 17 (server and build)
- **Build tool:** Gradle (included `build.gradle`)

