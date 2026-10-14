package com.example.mistic.entity;

import com.example.mistic.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

/**
 * Спавнит Mistic на расстоянии 55-70 блоков от игрока
 * в тёмное время суток (ночь) и в темноте.
 */
public class MisticSpawner {

    private static final int SPAWN_INTERVAL_TICKS = 20 * 30; // раз в 30 сек проверяем
    private static final double MIN_DIST = 55.0;
    private static final double MAX_DIST = 70.0;
    private static final int MAX_MISTIC_PER_WORLD = 3; // чтобы не было толпы

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MisticSpawner::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < SPAWN_INTERVAL_TICKS) return;
        tickCounter = 0;

        for (ServerWorld world : server.getWorlds()) {
            // Только Оверворлд
            if (world.getRegistryKey() != World.OVERWORLD) continue;

            // Только ночь
            long time = world.getTimeOfDay() % 24000L;
            if (time < 13000L || time > 23000L) continue;

            // Считаем текущее количество Mistic в мире
            int count = world.getEntitiesByType(ModEntities.MISTIC, e -> true).size();
            if (count >= MAX_MISTIC_PER_WORLD) continue;

            // Пробуем заспавнить у каждого игрока
            List<ServerPlayerEntity> players = world.getPlayers();
            for (ServerPlayerEntity player : players) {
                trySpawnNearPlayer(world, player);
            }
        }
    }

    private static void trySpawnNearPlayer(ServerWorld world, PlayerEntity player) {
        Random random = world.getRandom();

        // 10 попыток найти подходящую точку
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = MIN_DIST + random.nextDouble() * (MAX_DIST - MIN_DIST);

            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;

            // Ищем высоту поверхности
            BlockPos surfacePos = world.getTopPosition(
                    net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos((int) x, 0, (int) z)
            );

            // Проверяем, что это тёмное место и валидный спавн
            if (!world.isSpaceEmpty(ModEntities.MISTIC.createSimpleBoundingBox(
                    surfacePos.getX() + 0.5, surfacePos.getY(), surfacePos.getZ() + 0.5))) {
                continue;
            }

            // Проверка света — спавн только в темноте (свет < 7)
            int light = world.getLightLevel(net.minecraft.world.LightType.BLOCK, surfacePos);
            if (light > 7) continue;

            // Проверка расстояния ещё раз (на случай если высота уехала)
            double actualDist = Math.sqrt(surfacePos.getSquaredDistance(player.getPos()));
            if (actualDist < MIN_DIST || actualDist > MAX_DIST) continue;

            // Спавним
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
            return; // одного за тик хватит
        }
    }
}
