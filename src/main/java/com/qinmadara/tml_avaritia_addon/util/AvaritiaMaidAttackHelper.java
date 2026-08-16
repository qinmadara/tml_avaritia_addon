package com.qinmadara.tml_avaritia_addon.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.Config;
import committee.nova.mods.avaritia.common.entity.ImmortalItemEntity;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinitySwordItem;
import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModDamageTypes;
import committee.nova.mods.avaritia.init.registry.ModTags;
import committee.nova.mods.avaritia.util.ToolUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 无尽武器特效复刻核心。
 * 原理：无尽剑玩家版特效入口 InfinitySwordItem#onLeftClickEntity(ItemStack, Player, Entity)
 * 第一个参数就是 Player，且只有 Player#attack 会调用它——女仆永远走不到。
 * 因此在 MaidHurtTarget.Pre 事件中复刻该逻辑，把 Player 上下文替换为 EntityMaid，
 * 并复用 Avaritia 的 public 方法（sword.hurt / sword.die / ToolUtils.isInfinite）。
 */
public final class AvaritiaMaidAttackHelper {

    private AvaritiaMaidAttackHelper() {
    }

    /**
     * 复刻 InfinitySwordItem#onLeftClickEntity 的单目标特效，并在每次挥剑后触发一次
     * Re-Avaritia 右键 AOE（女仆无法右键，故合并进普攻；是否触发由 Config.maidSwordAoe 控制）。
     */
    public static void infinitySwordAttack(EntityMaid maid, Entity target, ItemStack stack, InfinitySwordItem sword) {
        Level level = maid.level();
        boolean endless = ModConfig.isSwordAttackEndless.get();
        LivingEntity victim = resolveVictim(target);

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || victim == null) {
            return;
        }

        // 女仆无法右键切换模式，先按 Config.forceKillMode 同步 mode.infinity_sword_kill
        AvaritiaWeaponUtil.applyForcedMode(stack);

        // 无尽伤害源：direct = 受害者, causer = 女仆（与原版用 player 的语义一致）
        DamageSource damageSource = infinityDamage(level, victim, maid);

        // 横扫（ToolUtils#sweepAttack 内部强制 Player，这里做 LivingEntity 版复刻）
        maidSweepAttack(serverLevel, maid, victim);

        // 单目标结算；穿全套无尽甲的玩家只爆炸、不直接扣血，但本次挥剑的 AOE 仍照常触发
        boolean fullInfinitePlayer = false;
        if (victim instanceof EnderDragon dragon) {
            dragon.hurt(dragon.head, damageSource,
                    endless ? Float.MAX_VALUE : sword.getTier().getAttackDamageBonus());
        } else if (victim instanceof Player pvp) {
            if (ToolUtils.isInfinite(pvp)) {
                serverLevel.explode(maid, pvp.getX(), pvp.getY(), pvp.getZ(),
                        25.0F, Level.ExplosionInteraction.MOB);
                fullInfinitePlayer = true;
            } else {
                sword.hurt(victim, damageSource,
                        endless ? Float.MAX_VALUE : sword.getTier().getAttackDamageBonus());
            }
        } else {
            sword.hurt(victim, damageSource,
                    endless ? Float.MAX_VALUE : sword.getTier().getAttackDamageBonus());
        }

        // 无尽伤害结算：血量归零 + 修正死亡结算（sword.die 是 public 方法，直接复用）
        if (!fullInfinitePlayer && endless && victim.isDeadOrDying()) {
            victim.setHealth(0);
            sword.die(victim, damageSource);
        }

        // 每次剑击附带范围伤害（默认开启；范围/伤害沿用 Re-Avaritia 配置）
        if (Config.maidSwordAoe) {
            aoeAttack(serverLevel, maid, stack,
                    ModConfig.swordAttackRange.get().floatValue(),
                    ModConfig.swordRangeDamage.get().floatValue());
        }
    }

    /**
     * 横扫复刻：原版 getSweepHitBox 只接受 Player，这里用等价 AABB 替代。
     * 效果：AABB 内非盟友实体击退 + PLAYER_ATTACK_SWEEP 音效 + SWEEP_ATTACK 粒子。
     */
    public static void maidSweepAttack(ServerLevel level, EntityMaid maid, Entity victim) {
        AABB box = maid.getBoundingBox().inflate(1.0D, 0.25D, 1.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e != maid && e != victim && !maid.isAlliedTo(e)) {
                e.knockback(0.6F,
                        Mth.sin(maid.getYRot() * Mth.DEG_TO_RAD),
                        -Mth.cos(maid.getYRot() * Mth.DEG_TO_RAD));
            }
        }
        level.playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.0F);
        double dx = -Mth.sin(maid.getYRot() * Mth.DEG_TO_RAD);
        double dz = Mth.cos(maid.getYRot() * Mth.DEG_TO_RAD);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                maid.getX() + dx, maid.getY(0.5D), maid.getZ() + dz, 0, dx, 0.0D, dz, 0.0D);
    }

    /**
     * 女仆版范围伤害：逐项复刻 ToolUtils#aoeAttack 的 Re-Avaritia 判定。
     * - 范围/伤害/闪电/物品/弹射物开关全部沿用 Re-Avaritia ModConfig；
     * - kill mode 关闭（默认）时只攻击 Enemy 且排除 forge:neutral_creatures；
     * - kill mode 开启时不区分敌我（与原版 hurtAnimal=true 一致）。
     */
    public static void aoeAttack(ServerLevel level, EntityMaid maid, ItemStack stack, float range, float damage) {
        boolean killMode = Config.forceKillMode || AvaritiaWeaponUtil.isModeActive(stack, "infinity_sword_kill");
        boolean lightOn = ModConfig.isSwordAttackLightning.get();
        boolean attackItems = ModConfig.isSwordAttackItemEntity.get();
        boolean attackProjectiles = ModConfig.isSwordAttackProjectile.get();

        AABB aabb = maid.getBoundingBox().inflate(range);
        DamageSource src = infinityDamage(level, maid, maid);
        List<Entity> toAttack = level.getEntities(maid, aabb);
        for (Entity entity : toAttack) {
            if (!attackItems && entity instanceof ItemEntity) {
                continue;
            }
            if (!attackProjectiles && entity instanceof Projectile) {
                continue;
            }
            if (entity instanceof ImmortalItemEntity) {
                continue;
            }
            if (!killMode && (!(entity instanceof Enemy) || entity.getType().is(ModTags.NEUTRAL_CREATURES))) {
                continue;
            }

            if (entity instanceof EnderDragon dragon) {
                dragon.setHealth(0);
            } else if (entity instanceof WitherBoss wither) {
                wither.setInvulnerableTicks(0);
                wither.hurt(src, damage);
            } else if (entity instanceof LivingEntity living) {
                living.hurt(src, damage);
            } else if (entity instanceof ExperienceOrb || entity instanceof AbstractArrow) {
                entity.discard();
            } else if (entity instanceof Projectile) {
                entity.discard();
            } else {
                entity.hurt(src, damage);
            }

            if (lightOn) {
                ToolUtils.trySummonLightning(level, 1, entity.blockPosition(), null);
            }
        }
    }

    /**
     * 复刻 DamageSources#source(ResourceKey, Entity, Entity)。
     * 1.20.1 中该三参重载为 private（vanilla 内部使用），因此这里用公共 API 等价构造，语义完全一致：
     * direct = 直接实体（受害者），causing = 伤害来源者（女仆，用于击杀归属）。
     */
    private static DamageSource infinityDamage(Level level, Entity direct, Entity causing) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.INFINITY);
        return new DamageSource(holder, direct, causing);
    }

    /** 解析真正受击目标：支持末影龙 PartEntity（头）等复合实体。 */
    @Nullable
    private static LivingEntity resolveVictim(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
