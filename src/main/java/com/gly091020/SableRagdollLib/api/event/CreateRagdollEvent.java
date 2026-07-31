package com.gly091020.SableRagdollLib.api.event;

import com.gly091020.SableRagdollLib.api.Ragdoll;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class CreateRagdollEvent extends Event {
    public static class Pre extends CreateRagdollEvent implements ICancellableEvent{
        private final ServerLevel serverLevel;
        private Vec3 pos;
        private Vec3 rotation;
        private ResourceLocation id;

        public Pre(ServerLevel serverLevel, Vec3 pos, Vec3 rotation, ResourceLocation id){
            this.serverLevel = serverLevel;
            this.pos = pos;
            this.rotation = rotation;
            this.id = id;
        }

        public ServerLevel getServerLevel() {
            return serverLevel;
        }

        public void setId(ResourceLocation id) {
            this.id = id;
        }

        public void setPos(Vec3 pos) {
            this.pos = pos;
        }

        public void setRotation(Vec3 rotation) {
            this.rotation = rotation;
        }

        public ResourceLocation getId() {
            return id;
        }

        public Vec3 getPos() {
            return pos;
        }

        public Vec3 getRotation() {
            return rotation;
        }
    }

    public static class Post extends CreateRagdollEvent{
        private final Ragdoll ragdoll;
        public Post(Ragdoll ragdoll){
            this.ragdoll = ragdoll;
        }

        public Ragdoll getRagdoll() {
            return ragdoll;
        }
    }
}
