package com.gly091020.SableRagdollLib.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RagdollManager {
    private static final Map<UUID, Ragdoll> RAGDOLLS = new HashMap<>();

    public static void add(Ragdoll ragdoll){
        RAGDOLLS.put(ragdoll.getUuid(), ragdoll);
    }

    public static Ragdoll get(UUID uuid){
        return RAGDOLLS.get(uuid);
    }

    public static void tick(){
        ArrayList<UUID> removes = new ArrayList<>();
        RAGDOLLS.forEach((k, v) -> {
            if(!v.isAlive())removes.add(k);
        });
        removes.forEach(RAGDOLLS::remove);
    }

    public static void reset(){
        RAGDOLLS.clear();
    }
}
