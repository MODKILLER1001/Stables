Stables - The Minecraft Plugin
=============================

[For a full description and tutorial, visit the Bukkit Dev page](http://dev.bukkit.org/bukkit-plugins/stables/)

***What does it do?***
Stables is a plugin that assists with Horse ownership, protection, and other horse related things. Vanilla has no ownership or tracking of horses, only if it has been broken or not. Stables changes this behavior. 

***Requirements***

* Virtual Stables (the storage portion of this plugin) Requires SQLite or MySQL! The plugin will still function without SQL, but this feature will not be enabled.
* To use COST based virtual stables, Vault is required. Free storage does not require any additional plugins.

***Current Features***
* Protect Horses from configurable outside damages
* Add recipes for Horse Armor (Barding), Name Tags and Saddles
* Add 'ownership' to horses so others cannot steal them - Use a name tag on a horse to claim it!
* Store horses in virtual stables, and recover them!
* Ability to find, summon or even teleport to your horse!
* Abilities for Admin/Staff to Remove Ownership
* Sets a max amount of horses one player can own
* Anyone with the stables.admin permission can use horses without permission
* Allow a friend to ride a claimed horse
* Lure a horse from the wild using an item (defaults Golden Carrot - Other suggestions would be Emeralds, Golden Apples)
* Horse Spawning, including ZOMBIE and SKELETON horses!
* Teleport, Summon and Locate your claimed Horses!
* Allows staff to rename horses without changing ownership



***How does it work?***
Name a horse to claim it as your own. Hit it (as the owner) with a new Name Tag (un-renamed) to free it!
* Apparently naming a horse is confusing to some people. To name a horse, you have to get a NAME TAG. Use the NAME TAG in an ANVIL and change it's name to what you want the horse to be called. Then USE (Right Click) The Horse with the NAMED NAME TAG to name it. This is a VANILLA MINECRAFT feature - it is not included with Stables. This action, however, is what will claim a horse with the Stables plugin.

* You can also set the config to 'AutoOwn = true' - This will automatically claim a horse as soon as it is tamed!

Stables also prevents horses from being killed by players, mobs, environmental, or any combination of these. All options are 100% toggle-able through the config.

***Virtual Stables***
This feature can be disabled by the admins by setting the 'allowCommand' config option to false, and then just not creating any stable signs.

To create a stable, simply place a sign with [stables] as the first line. Stables will take over from there. If 'allowCommand' is disabled, you can ONLY use the Virtual Stables feature with a sign. Punch the sign, or type /stables store to store a horse.
Please note: Storage does *NOT* save chests right now. This is a known bug.

* Please note: The stables does *NOT* save Horse Speed. This is a Minecraft/Bukkit shortfall, and will be addressed as soon as there is a way to do so.


=====

Copyright (c) 2013, All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of the The Multiverse Team nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.