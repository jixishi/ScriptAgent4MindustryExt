![For Mindustry](https://img.shields.io/badge/For-Mindustry-orange) ![Lang US](https://img.shields.io/badge/Lang-EN--US-blue) ![Support 6.0](https://img.shields.io/badge/Support_Version-123.-success) ![GitHub Releases](https://img.shields.io/github/downloads/jixishi/ScriptAgent4MindustryExt-English-Version/latest/total)
# ScriptAgent for Mindustry
[zh-CN](https://github.com/way-zer/ScriptAgent4MindustryExt)
A strong script plugin for Mindustry by kts

## features
- Powerful, based on **kotlin**, can access all Java interfaces (all plugins can do it, scripts can do it)
- Fast, after the script is loaded, it is converted to jvm byteCode, and there is no performance gap with plugin written in java
- Flexible, modules and scripts have a complete life cycle, and can be hot-loaded and hot-reloaded at any time
- Fast, a lot of helper functions commonly used for development, can be quickly deployed to the server without compilation
- Smart, with IDEA (or AndroidStudio) smart completion
- Customizable, except for the core loader, the plugin is implemented by scripts, which can be modified according to your own needs.  
    In addition, the module definition script can extend content scripts (DSL,library,manager,defaultImport)
## Install This Plugin
1. Download the **jar**, see **Release** Description
2. Install the plugin in server(Placed under config/mods)
3. Install the scripts and place them (files and folders in /src) directly into the plugin configuration directory (config/)
## Base Commands
>- ### Console Commands
> 1. **madmin** # Add plugin administrator
> 2. ***sa \<help>*** # Manage scripts or modules
>- ### **Permissions Node**

```json
{
    "wayzer" : {
        "permission" : {
            # Permission settings
            # Special groups are: @default,@admin,@lvl0,@lvl1, etc., user qq can be a separate group
            # The value is the permission, @ starts with the group, support the end wildcard. *
            "groups" : {
                "@admin" : [
                    "wayzer.admin.ban",
                    "wayzer.info.other",
                    "wayzer.vote.ban",
                    "wayzer.maps.host",
                    "wayzer.maps.load",
                    "wayzer.ext.team.change"
                ],
                "@default" : [
                    "wayzer.ext.observer",
                    "wayzer.lang.setDefault",
                    "wayzer.lang.reload",
                    "wayzer.ext.history"
                ]
            }
        }
    }
}

```
## How to develop scripts
1. Copy this repository (or configure gradle yourself, see build.gradle.kts)
2. Import the project in IDEA (recommended to import as Project to avoid interference)
3. Synchronous Gradle
## Directory Structure
- scripts.init.kts (Module definition script)
- scripts(Module root directory)
    - lib(Module library directory, write in **.kt**, shared by all scripts of the module, the same life cycle as the module)
    - .metadata(Module metadata for IDE and other compilers to analyze and compile, and can be generated when the plugin is run)
    - manager.content.kts(Script to implement your logic)
### Precautions
1. After reloading the script, the same class is not necessarily the same, pay attention to controlling the life cycle  
    If you need similar operations, you can set up an abstract interface to store variables in a longer life cycle(less frequent reloading)
## Existing modules
- scripts(Main module with basic extensions,you can write simple scripts there)
- wayzer(Server basic management module, Rewritten from [MyMindustryPlugin](https://github.com/way-zer/MyMindustryPlugin))
## Specific functions
Check out the [Wiki](https://github.com/jixishi/ScriptAgent4MindustryExt-English-Version/wiki)
## copyright
- Plugin：Reprinting and other uses (including decompiling not for use) are prohibited without permission
- Script: belongs to the script maker, the reprint of scripts in this repository needs to indicate the link to this page(or this repository)