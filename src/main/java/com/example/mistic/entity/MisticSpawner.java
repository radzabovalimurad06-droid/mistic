package com.example.mistic.entity;

import com.example.mistic.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class MisticSpawner {

    private static final int SPAWN_INTERVAL_TICKS = 20 * 30;   // раз в 30 сек
    private static final double SPAWN_DISTANCE = 20.0;         // радиус 20 блоков
    private static final double SPAWN_DISTANCE_MIN = 12.0;     // не ближе 12 блоков (чтоб не спавнился в упор)
    private static final double WORLD_LIMIT_RADIUS = 96.0;     // 96 блоков ~ 12 чанков (1 чанк = 16 блоков * 12 / 2)
    private static final double FOV_DOT_THRESHOLD = 0.5;       // ~60° угол поля зрения

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MisticSpawner::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < SPAWN_INTERVAL_TICKS) return;
        tickCounter = 0;

        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey() != World.OVERWORLD) continue;

            // Только ночь
            long time = world.getTimeOfDay() % 24000L;
            if (time < 13000L || time > 23000L) continue;

            // Для каждого игрока
            List<ServerPlayerEntity> players = world.getPlayers();
            for (ServerPlayerEntity player : players) {
                // Уже есть Mistic в радиусе 12 чанков от этого игрока?
                if (hasMisticNearby(world, player)) continue;

                trySpawnNearPlayer(world, player);
            }
        }
    }

    /**
     * Проверяет, есть ли уже Mistic в радиусе 12 чанков (~96 блоков) от игрока.
     */
    private static boolean hasMisticNearby(ServerWorld world, PlayerEntity player) {
        List<MisticEntity> nearby = world.getEntitiesByClass(
                MisticEntity.class,
                player.getBoundingBox().expand(WORLD_LIMIT_RADIUS),
                e -> true
        );
        return !nearby.isEmpty();
    }

    private static void trySpawnNearPlayer(ServerWorld world, PlayerEntity player) {
        Random random = world.getRandom();

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = SPAWN_DISTANCE_MIN + random.nextDouble() * (SPAWN_DISTANCE - SPAWN_DISTANCE_MIN);

            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;

            // Проверка "вне поля зрения"
            if (isInPlayerFOV(player, x, z)) continue;

            BlockPos surfacePos = world.getTopPosition(
                    net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos((int) x, 0, (int) z)
            );

            if (!world.isAir(surfacePos)) continue;
            if (!world.isAir(surfacePos.up())) continue;

            int light = world.getLightLevel(net.minecraft.world.LightType.BLOCK, surfacePos);
            if (light > 7) continue;

            double actualDist = Math.sqrt(surfacePos.getSquaredDistance(player.getPos()));
            if (actualDist < SPAWN_DISTANCE_MIN || actualDist > SPAWN_DISTANCE) continue;

            MisticEntity mistic = ModEntities.MISTIC.create(world, SpawnReason.NATURAL);
            if (mistic == null) continue;

            mistic.refreshPositionAndAngles(
                    surfacePos.getX() + 0.5,
                    surfacePos.getY(),
                    surfacePos.getZ() + 0.5,
                    random.nextFloat() * 360f,
                    0f
            );

            world.spawnEntity(mistic);
            return;
        }
    }

    /**
     * Проверяет, находится ли точка (x, z) в поле зрения игрока.
     * Используем dot product между вектором взгляда игрока и вектором на точку.
     */
    private static boolean isInPlayerFOV(PlayerEntity player, double x, double z) {
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Vec3d toTarget = new Vec3d(x - player.getX(), 0, z - player.getZ()).normalize();

        double dot = look.x * toTarget.x + look.z * toTarget.z;

        // dot > 0.5 ~ угол < 60° — точка в поле зрения
        return dot > FOV_DOT_THRESHOLD;
    }
}
