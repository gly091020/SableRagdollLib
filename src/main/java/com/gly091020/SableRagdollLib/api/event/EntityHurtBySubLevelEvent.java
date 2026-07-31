package com.gly091020.SableRagdollLib.api.event;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

public class EntityHurtBySubLevelEvent extends Event{
    private final SubLevel subLevel;
    private float damage;
    private final LivingEntity target;
    private final double magnitude;

    public EntityHurtBySubLevelEvent(SubLevel subLevel, LivingEntity target, double magnitude, float damage){
        this.subLevel = subLevel;
        this.target = target;
        this.damage = damage;
        this.magnitude = magnitude;
    }

    public SubLevel getSubLevel() {
        return subLevel;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }
}
