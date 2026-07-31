package com.gly091020.SableRagdollLib.api.event;

import com.gly091020.SableRagdollLib.api.Ragdoll;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public abstract class RagdollCollisionEvent extends Event {
    private final Ragdoll self;
    @Nullable
    private final Ragdoll target;

    public RagdollCollisionEvent(Ragdoll self, @Nullable Ragdoll target){
        this.self = self;
        this.target = target;
    }

    public Ragdoll getSelf() {
        return self;
    }

    public @Nullable Ragdoll getTarget() {
        return target;
    }

    public static class Pre extends RagdollCollisionEvent implements ICancellableEvent{
        public Pre(Ragdoll self, @Nullable Ragdoll target) {
            super(self, target);
        }
    }

    public static class Post extends RagdollCollisionEvent{
        public Post(Ragdoll self, @Nullable Ragdoll target) {
            super(self, target);
        }
    }
}
