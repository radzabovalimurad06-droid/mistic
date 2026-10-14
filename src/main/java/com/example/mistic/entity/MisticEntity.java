package com.example.mistic.entity;

import com.example.mistic.registry.ModEntities;
import com.example.mistic.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MisticEntity extends HostileEntity {

    // ====== НАСТРОЙКИ ======
    private static final double DETECT_RANGE_STANDING = 12.0;   // стоя
    private static final double DETECT_RANGE_SNEAKING = 5.0;    // на шифте
    private static final double CLOSE_IGNORE_HEIGHT = 3.0;      // ближе 3 блоков - видит даже ниже
    private static final int LOSE_TARGET_TICKS = 40;            // 2 секунды (20 тиков = 1 сек)
    private static final double HEARTBEAT_RANGE = 15.0;         // радиус стука сердца

    // Таймер потери цели
    private int loseTargetTimer = 0;
    private Vec3d lastKnownPlayerPos = null;

    // Таймер для стука сердца
    private int heartbeatCooldown = 0;

    public MisticEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createMisticAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0)          // 25 сердец
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)       // ~2 блока/сек
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)        // 2 сердца
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 70.0)        // для pathfinding
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void initGoals() {
        // Приоритет 0 — плавание
        this.goalSelector.add(0, new SwimGoal(this));

        // Приоритет 1 — атака ближнего боя (догоняет и бьёт)
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));

        // Приоритет 2 — смотреть на игрока
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));

        // Приоритет 3 — случайное блуждание
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));

        // Приоритет 4 — смотреть по сторонам
        this.goalSelector.add(4, new LookAroundGoal(this));

        // === ЦЕЛИ (target goals) ===
        // Приоритет 1 — кастомная цель на игрока (наша логика видимости)
        this.targetSelector.add(1, new MisticTargetGoal(this));
        // Приоритет 2 — защита от других мобов
        this.targetSelector.add(2, new RevengeGoal(this));
    }

    // ====== КАСТОМНАЯ ПРОВЕРКА ВИДИМОСТИ ======
    /**
     * Проверяет, может ли Mistic увидеть игрока.
     * Учитывает: дистанцию, шифт, высоту (слепую зону), линию видимости.
     */
    public boolean canSeePlayer(PlayerEntity player) {
        if (player.isSpectator() || !player.isAlive()) return false;

        double distance = this.squaredDistanceTo(player);
        double dist = Math.sqrt(distance);

        // 1. Проверка дистанции в зависимости от шифта
        double maxRange = player.isSneaking() ? DETECT_RANGE_SNEAKING : DETECT_RANGE_STANDING;
        if (dist > maxRange) return false;

        // 2. Проверка "слепой зоны" по высоте
        // Если игрок на 1+ блок ниже уровня глаз Mistic — не видит,
        // НО если ближе 3 блоков — видит.
        double eyeY = this.getEyeY();
        double playerEyeY = player.getEyeY();
        double heightDiff = eyeY - playerEyeY;

        if (heightDiff >= 1.0 && dist >= CLOSE_IGNORE_HEIGHT) {
            return false;
        }

        // 3. Проверка линии видимости (нет ли стены)
        return this.canSee(player);
    }

    // ====== ТИК ======
    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) return;

        // Обработка стука сердца
        handleHeartbeat();

        // Логика потери цели
        LivingEntity target = this.getTarget();

        if (target instanceof PlayerEntity player) {
            if (canSeePlayer(player)) {
                // Видим — сбрасываем таймер, запоминаем позицию
                loseTargetTimer = 0;
                lastKnownPlayerPos = player.getPos();
            } else {
                // Не видим — запускаем таймер
                loseTargetTimer++;

                // Если игрок был виден недавно — бежим к последней точке
                if (lastKnownPlayerPos != null && loseTargetTimer < LOSE_TARGET_TICKS) {
                    this.getNavigation().startMovingTo(
                            lastKnownPlayerPos.x,
                            lastKnownPlayerPos.y,
                            lastKnownPlayerPos.z,
                            1.0
                    );
                }

                // Таймер вышел — теряем цель
                if (loseTargetTimer >= LOSE_TARGET_TICKS) {
                    this.setTarget(null);
                    lastKnownPlayerPos = null;
                    loseTargetTimer = 0;
                }
            }
        }
    }

    // ====== СТУК СЕРДЦА ======
    private void handleHeartbeat() {
        PlayerEntity nearest = this.getWorld().getClosestPlayer(this, HEARTBEAT_RANGE);

        if (nearest == null) return;
        if (heartbeatCooldown > 0) {
            heartbeatCooldown--;
            return;
        }

        double dist = this.distanceTo(nearest);

        // Чем ближе — тем чаще стук
        // 15 блоков → раз в 40 тиков (2 сек)
        // 3 блока  → раз в 10 тиков (0.5 сек)
        int interval = (int) Math.max(10, (dist / HEARTBEAT_RANGE) * 40);
        heartbeatCooldown = interval;

        this.getWorld().playSound(
                null,
                this.getBlockPos(),
                ModSounds.MISTIC_HEARTBEAT,
                this.getSoundCategory(),
                0.7f,
                1.0f
        );
    }

    // ====== ЗВУКИ ======
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MISTIC_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MISTIC_GROWL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MISTIC_GROWL;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.BlockState state) {
        this.playSound(ModSounds.MISTIC_STEP, 0.5f, 1.0f);
    }

    // ====== АТАКА ======
    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (result && target instanceof PlayerEntity) {
            // Дополнительный "прыжок страха" — можно добавить эффект слепоты
            // ((PlayerEntity) target).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0));
        }
        return result;
    }

    // ====== ЯВЛЯЕТСЯ ЛИ ПРЕСЛЕДОВАТЕЛЕМ ======
    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof PlayerEntity player) {
            return canSeePlayer(player);
        }
        return super.canTarget(target);
    }
}
