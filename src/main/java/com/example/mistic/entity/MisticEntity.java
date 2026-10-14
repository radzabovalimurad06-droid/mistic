package com.example.mistic.entity;

import com.example.mistic.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MisticEntity extends HostileEntity {

    // ====== НАСТРОЙКИ ======
    private static final double DETECT_RANGE_STANDING = 12.0;
    private static final double DETECT_RANGE_SNEAKING = 5.0;
    private static final double CLOSE_IGNORE_HEIGHT = 3.0;
    private static final int LOSE_TARGET_TICKS = 40; // 2 сек
    private static final double HEARTBEAT_RANGE = 15.0;

    // Скорости
    private static final double SPEED_NORMAL = 0.3;
    private static final double SPEED_HUNTING = 0.5;

    // Радиус начала охоты (если видит или просто близко)
    public static final double HUNT_TRIGGER_RANGE = 15.0;

    // ====== СОСТОЯНИЕ ======
    private boolean isHunting = false;
    private int loseTargetTimer = 0;
    private Vec3d lastKnownPlayerPos = null;
    private int heartbeatCooldown = 0;

    public MisticEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 50;
    }

    public static DefaultAttributeContainer.Builder createMisticAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 200.0)          // 100 сердец
                .add(EntityAttributes.MOVEMENT_SPEED, SPEED_NORMAL)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)          // 2 сердца
                .add(EntityAttributes.FOLLOW_RANGE, 70.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.9); // почти не отбрасывается
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new MisticTargetGoal(this));
        this.targetSelector.add(2, new RevengeGoal(this));
    }

    // ====== ОХОТА ======
    public boolean isHunting() {
        return isHunting;
    }

    public void startHunting(PlayerEntity player) {
        if (this.isHunting) return;
        this.isHunting = true;
        this.setTarget(player);
        this.lastKnownPlayerPos = player.getPos();
        this.loseTargetTimer = 0;

        // Меняем скорость на "буст"
        if (this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED) != null) {
            this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(SPEED_HUNTING);
        }

        // Сообщение в чат жертве
        if (player instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("§4§lОХОТА НАЧАЛАСЬ"), false);
        }
    }

    public void stopHunting() {
        this.isHunting = false;
        this.setTarget(null);
        this.lastKnownPlayerPos = null;
        this.loseTargetTimer = 0;

        // Возвращаем обычную скорость
        if (this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED) != null) {
            this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(SPEED_NORMAL);
        }
    }

    // ====== ВИДИМОСТЬ ======
    public boolean canSeePlayer(PlayerEntity player) {
        if (player.isSpectator() || !player.isAlive()) return false;

        // Во время охоты — видит всегда и везде
        if (this.isHunting) return true;

        double dist = Math.sqrt(this.squaredDistanceTo(player));

        double maxRange = player.isSneaking() ? DETECT_RANGE_SNEAKING : DETECT_RANGE_STANDING;
        if (dist > maxRange) return false;

        double eyeY = this.getEyeY();
        double playerEyeY = player.getEyeY();
        double heightDiff = eyeY - playerEyeY;

        if (heightDiff >= 1.0 && dist >= CLOSE_IGNORE_HEIGHT) {
            return false;
        }

        return this.canSee(player);
    }

    // ====== ТИК ======
    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) return;

        handleHeartbeat();

        LivingEntity target = this.getTarget();

        if (target instanceof PlayerEntity player) {
            if (canSeePlayer(player)) {
                loseTargetTimer = 0;
                lastKnownPlayerPos = player.getPos();
            } else {
                // Если охота не активна — теряем цель через 2 сек
                if (!isHunting) {
                    loseTargetTimer++;

                    if (lastKnownPlayerPos != null && loseTargetTimer < LOSE_TARGET_TICKS) {
                        this.getNavigation().startMovingTo(
                                lastKnownPlayerPos.x,
                                lastKnownPlayerPos.y,
                                lastKnownPlayerPos.z,
                                1.0
                        );
                    }

                    if (loseTargetTimer >= LOSE_TARGET_TICKS) {
                        stopHunting();
                    }
                }
            }
        } else if (isHunting) {
            // Охотимся, но цели уже нет (игрок вышел из мира / умер)
            stopHunting();
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
        int interval = (int) Math.max(6, (dist / HEARTBEAT_RANGE) * 40);
        heartbeatCooldown = interval;

        this.getWorld().playSound(
                null,
                this.getBlockPos(),
                ModSounds.MISTIC_HEARTBEAT,
                this.getSoundCategory(),
                0.8f,
                1.0f
        );
    }

    // ====== УРОН ТОЛЬКО ОТ ЖЕЛЕЗНОГО МЕЧА ======
    @Override
    public boolean damage(DamageSource source, float amount) {
        // Пропускаем только урон от игрока с железным мечом в руке
        if (source.getAttacker() instanceof PlayerEntity player) {
            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.getItem() == Items.IRON_SWORD) {
                return super.damage(source, amount);
            }
            // Всё остальное — не проходит
            return false;
        }

        // Урон не от игрока (падение, зелья, другие мобы) — не проходит
        return false;
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
        this.playSound(ModSounds.MISTIC_STEP, 0.6f, 1.0f);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof PlayerEntity player) {
            return canSeePlayer(player);
        }
        return super.canTarget(target);
    }
}
