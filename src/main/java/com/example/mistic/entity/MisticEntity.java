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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MisticEntity extends HostileEntity {

    private static final double DETECT_RANGE_STANDING = 12.0;
    private static final double DETECT_RANGE_SNEAKING = 5.0;
    private static final double CLOSE_IGNORE_HEIGHT = 3.0;
    private static final int LOSE_TARGET_TICKS = 40;
    private static final double HEARTBEAT_RANGE = 15.0;

    private static final double SPEED_NORMAL = 0.3;
    private static final double SPEED_HUNTING = 0.5;

    public static final double HUNT_TRIGGER_RANGE = 15.0;

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
                .add(EntityAttributes.MAX_HEALTH, 200.0)
                .add(EntityAttributes.MOVEMENT_SPEED, SPEED_NORMAL)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.FOLLOW_RANGE, 70.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // Приоритет 1 - атака. Важно: high speed multiplier чтобы догонял
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.5, true)); // true = pauseWhenMobIdle
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(4, new LookAroundGoal(this));

        // Цели: только RevengeGoal, охоту ведём через tick()
        this.targetSelector.add(1, new RevengeGoal(this));
    }

    public boolean isHunting() {
        return isHunting;
    }

    public void startHunting(PlayerEntity player) {
        if (this.isHunting) return;
        this.isHunting = true;
        this.setTarget(player);
        this.lastKnownPlayerPos = player.getPos();
        this.loseTargetTimer = 0;

        if (this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED) != null) {
            this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(SPEED_HUNTING);
        }

        if (player instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("§4§lОХОТА НАЧАЛАСЬ"), false);
        }
    }

    public void stopHunting() {
        this.isHunting = false;
        this.setTarget(null);
        this.lastKnownPlayerPos = null;
        this.loseTargetTimer = 0;

        if (this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED) != null) {
            this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(SPEED_NORMAL);
        }
    }

    public boolean canSeePlayer(PlayerEntity player) {
        if (player.isSpectator() || !player.isAlive()) return false;
        if (this.isHunting) return true;

        double dist = Math.sqrt(this.squaredDistanceTo(player));
        double maxRange = player.isSneaking() ? DETECT_RANGE_SNEAKING : DETECT_RANGE_STANDING;
        if (dist > maxRange) return false;

        double heightDiff = this.getEyeY() - player.getEyeY();
        if (heightDiff >= 1.0 && dist >= CLOSE_IGNORE_HEIGHT) return false;

        return this.canSee(player);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) return;

        handleHeartbeat();

        PlayerEntity nearest = this.getWorld().getClosestPlayer(this, HUNT_TRIGGER_RANGE);

        if (!isHunting) {
            // Пытаемся начать охоту
            if (nearest != null
                    && !nearest.isCreative()
                    && !nearest.isSpectator()
                    && nearest.isAlive()) {
                startHunting(nearest);
            }
        } else {
            // Охота активна
            LivingEntity target = this.getTarget();

            if (!(target instanceof PlayerEntity player) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
                stopHunting();
                return;
            }

            // Продолжаем "видеть" игрока сквозь всё
            this.lastKnownPlayerPos = player.getPos();
            this.loseTargetTimer = 0;

            // Бежим к игроку каждый тик
            this.getNavigation().startMovingTo(player, 1.5);
            this.getLookControl().lookAt(player, 30.0f, 30.0f);

            // Бьём если близко
            double distSq = this.squaredDistanceTo(player);
            if (distSq < 4.0) { // 2 блока
                // MeleeAttackGoal обычно сам вызывает tryAttack,
                // но на всякий случай можно и вручную
            }
        }
    }

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

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity player) {
            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.getItem() == Items.IRON_SWORD) {
                return super.damage(world, source, amount);
            }
            return false;
        }
        return false;
    }

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
        if (target instanceof PlayerEntity) {
            return true; // во время охоты цель всегда валидна
        }
        return super.canTarget(target);
    }
}
