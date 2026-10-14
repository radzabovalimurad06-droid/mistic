package com.example.mistic.entity;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Кастомная цель: Mistic выбирает игрока целью,
 * если может его "увидеть" по нашей логике.
 */
public class MisticTargetGoal extends ActiveTargetGoal<PlayerEntity> {

    private final MisticEntity mistic;

    public MisticTargetGoal(MisticEntity mistic) {
        super(mistic, PlayerEntity.class, true);
        this.mistic = mistic;
    }

    @Override
    public boolean canStart() {
        // Не начинаем охоту, если цель уже есть
        if (this.mistic.getTarget() != null) return false;

        // Ищем ближайшего игрока в радиусе 12
        PlayerEntity nearest = this.mistic.getWorld().getClosestPlayer(this.mistic, 12.0);
        if (nearest == null) return false;

        // Проверяем нашу логику видимости
        if (!this.mistic.canSeePlayer(nearest)) return false;

        // Всё ок — назначаем целью
        this.target = nearest;
        return true;
    }

    @Override
    public void start() {
        this.mistic.setTarget(this.target);
        super.start();
    }
}
