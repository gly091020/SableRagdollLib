package com.gly091020.SableRagdollLib.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScheduleManager {
    private static final List<DelayedTask> DELAYED_TASKS = new CopyOnWriteArrayList<>();

    private record DelayedTask(long targetTick, Runnable runnable) {}
    public static void scheduleDelayed(ServerLevel level, int delayTicks, Runnable runnable) {
        long target = level.getServer().getTickCount() + delayTicks;
        DELAYED_TASKS.add(new DelayedTask(target, runnable));
    }

    public static void tick(MinecraftServer server){
        long now = server.getTickCount();
        for (DelayedTask task : DELAYED_TASKS) {
            if (task.targetTick() <= now) {
                task.runnable().run();
                DELAYED_TASKS.remove(task);
            }
        }
    }
}
