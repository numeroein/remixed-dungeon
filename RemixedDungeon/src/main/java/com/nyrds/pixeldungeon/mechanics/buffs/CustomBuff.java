package com.nyrds.pixeldungeon.mechanics.buffs;

import androidx.annotation.Keep;

import com.nyrds.Packable;
import com.nyrds.android.util.ModError;
import com.nyrds.pixeldungeon.mechanics.LuaScript;
import com.watabou.noosa.StringsManager;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.utils.Bundle;

import org.luaj.vm2.LuaTable;

public class CustomBuff extends Buff {

    private String name;
    private String info;

    private int icon;

    @Packable
    private String scriptFile;

    private LuaScript script;


    @Keep
    public CustomBuff() {
    }

    public CustomBuff(String scriptFile) {
        initFromFile(scriptFile);
    }

    private void initFromFile(String scriptFile) {
        try {
            this.scriptFile = scriptFile;

            script = new LuaScript("scripts/buffs/" + scriptFile, this);

            LuaTable desc = script.run("buffDesc").checktable();

            icon = desc.rawget("icon").checkint();
            name = StringsManager.maybeId(desc.rawget("name").checkjstring());
            info = StringsManager.maybeId(desc.rawget("info").checkjstring());
        } catch (Exception e){
            throw new ModError("Buff",e);
        }
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        initFromFile(scriptFile);
    }

    @Override
    public int icon() {
        return icon;
    }

    @Override
    public String getEntityKind() {
        return scriptFile;
    }

    @Override
    public boolean attachTo(Char target) {
        try {
            if (super.attachTo(target)) {
                return script.run("attachTo", target).checkboolean();
            }
            return false;
        } catch (Exception e) {
            throw new ModError("Error in "+scriptFile+" attachTo", e);
        }
    }

    @Override
    public void detach() {
        super.detach();
        script.run("detach");
    }

    @Override
    public boolean act() {
        script.run("act");
        return true;
    }

    @Override
    public int drBonus() {
        return script.runOptional("drBonus",0).checkint();
    }

    @Override
    public int stealthBonus() {
        return script.runOptional("stealthBonus",0).checkint();
    }

    @Override
    public float speedMultiplier() {
        return (float) script.runOptional("speedMultiplier",1.f).checkdouble();
    }

    @Override
    public int regenerationBonus() {
        return script.runOptional("regenerationBonus",0).checkint();
    }

    @Override
    public void charAct() {
        script.runOptional("charAct");
    }

    @Override
    public String name() {
        return name;
    }
}
