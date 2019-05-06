---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by mike.
--- DateTime: 21.03.19 22:57
---

local RPD = require "scripts/lib/commonClasses"

local spell = require "scripts/lib/spell"

return spell.init{
    desc  = function ()
        return {
            image         = 0,
            imageFile     = "spellsIcons/warrior.png",
            name          = "DieHardSpell_Name",
            info          = "DieHardSpell_Info",
            magicAffinity = "Combat",
            targetingType = "self",
            level         = 1,
            spellCost     = 5,
            cooldown      = 50,
            castTime      = 0.5
        }
    end,
    cast = function(self, spell, caster, cell)

        local buffLevel = math.min(3, caster:skillLevel()-self.level)

        local duration = buffLevel * 10 + 10

        local buff = RPD.affectBuff(caster,"DieHard", duration)
        buff:level(buffLevel)

        --attach visual stuff
        local emitter = caster:getSprite():emitter()
        emitter:pour(RPD.speckEffectFactory(RPD.Sfx.Speck.HEALING,RPD.Sfx.Speck.UP), duration)

        return true
    end
}
