---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by mike.
--- DateTime: 25.03.19 0:06
---
local RPD  = require "scripts/lib/commonClasses"

local buff = require "scripts/lib/buff"


return buff.init{
    desc  = function ()
        return {
            icon          = 44,
            name          = "DieHardBuff_Name",
            info          = "DieHardBuff_Info",
        }
    end,

    attachTo = function(self, buff, target)
        return true
    end,

    detach = function(self, buff)
    end,

    regenerationBonus = function(self, buff)
        return buff:level()
    end,

    charAct = function(self,buff)
        local emitter = buff.target:getSprite():emitter()
        emitter:burst(RPD.speckEffectFactory(RPD.Sfx.Speck.HEALING,RPD.Sfx.Speck.UP),1)
    end
}
