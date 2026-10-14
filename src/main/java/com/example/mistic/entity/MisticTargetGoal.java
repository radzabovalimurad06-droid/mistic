package com.example.mistic.entity;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Кастомная цель: Mistic выбирает игрока целью и начинает охоту,
 * если игрок находится в радиусе HUNT_TRIGGER_RANGE (15 блоков).
 * Охота запускается СРАЗУ, без таймера, даже если Mistic не видит игрока.
 */
public class MisticTargetGoal extends ActiveTargetGoal<PlayerEntity> {

    private final MisticEntity mistic;

    public MisticTargetGoal(MisticEntity mistic) {
        super(mistic, PlayerEntity.class, true);
        this.mistic = mistic;
    }

    @Override
    public boolean canStart() {
        // Если уже охотимся — не перезапускаем
        if (this.mistic.isHunting()) return false;

        // Ищем ближайшего игрока в радиусе охоты (15 блоков)
        PlayerEntity nearest = this.mistic.getWorld().getClosestPlayer(
                this.mistic, MisticEntity.HUNT_TRIGGER_RANGE);

        if (nearest == null) return false;

        // Игнорируем креатив/спектатор
        if (nearest.isCreative() || nearest.isSpectator()) return false;

        // Живой?
        if (!nearest.isAlive()) return false;

        // Запускаем охоту
        this.mistic.startHunting(nearest);
        return true;
    }

    @Override
    public void start() {
        // Охота уже запущена в canStart() — здесь только финализируем
        if (this.mistic.getTarget() == null) {
            this.mistic.setTarget(this.target);
        }
        super.start();
    }
}
