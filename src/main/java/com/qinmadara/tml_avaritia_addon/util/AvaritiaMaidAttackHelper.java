package com.qinmadara.tml_avaritia_addon.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.qinmadara.tml_avaritia_addon.Config;
import committee.nova.mods.avaritia.common.item.tools.infinity.InfinitySwordItem;
import committee.nova.mods.avaritia.init.config.ModConfig;
import committee.nova.mods.avaritia.init.registry.ModDamageTypes;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
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
     * 复刻 InfinitySwordItem#onLeftClickEntity 的完整近战特效：
     * 强制斩杀模式 → 单目标伤害（无尽伤害源）→ 横扫 → 斩杀结算 / PVP 无尽甲爆炸。
     */
    public static void infinitySwordAttack(EntityMaid maid, Entity target, ItemStack stack, InfinitySwordItem sword) {
        Level level = maid.level();
        boolean endless = ModConfig.isSwordAttackEndless.get();
        LivingEntity victim = resolveVictim(target);

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || victim == null) {
            return;
        }

        // 强制斩杀模式（女仆无法右键切换模式；applyForcedMode 内部已检查 Config.forceKillMode）
        AvaritiaWeaponUtil.applyForcedMode(stack);

        // 无尽伤害源：direct = 受害者, causer = 女仆（与原版用 player 的语义一致）
        DamageSource damageSource = infinityDamage(level, victim, maid);

        // 横扫（ToolUtils#sweepAttack 内部强制 Player，这里做 LivingEntity 版复刻）
        maidSweepAttack(serverLevel, maid, victim);

        if (victim instanceof EnderDragon dragon) {
            dragon.hurt(dragon.head, damageSource,
                    endless ? Float.MAX_VALUE : sword.getTier().getAttackDamageBonus());
        } else if (victim instanceof Player pvp) {
            if (ToolUtils.isInfinite(pvp)) {
                // 穿全套无尽甲：只爆炸、不造成伤害
                // （Re-Avaritia 1.4.0 release 中 ModConfig 无 isSwordAttackExplode 字段，故无条件触发）
                serverLevel.explode(maid, pvp.getX(), pvp.getY(), pvp.getZ(),
                        25.0F, Level.ExplosionInteraction.MOB);
                return;
            } else {
                sword.hurt(victim, damageSource,
                        endless ? Float.MAX_VALUE : sword.getTier().getAttackDamageBonus());
            }
        } else {
            sword.hurt(victim, damageSource,
                    endless ? Float.MAX_VALUE : sword.getTier().getAttackDamageBonus());
        }

        // 斩杀模式：血量归零 + 修正死亡结算（sword.die 是 public 方法，直接复用）
        if (endless && victim.isDeadOrDying()) {
            victim.setHealth(0);
            sword.die(victim, damageSource);
        }

        // 可选：配置开启时，每次剑击附带范围伤害（原版右键 AOE 的简化女仆版，默认关）
        if (Config.maidSwordAoe) {
            aoeAttack(serverLevel, maid,
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
     * 范围伤害（简化女仆版，复刻 ToolUtils#aoeAttack 的核心逻辑；
     * 原版参数强制 Player，此处 LivingEntity 化。闪电/物品/弹射物过滤等细节后续可补）。
     */
    public static void aoeAttack(ServerLevel level, EntityMaid maid, float range, float damage) {
        AABB aabb = maid.getBoundingBox().inflate(range);
        DamageSource src = infinityDamage(level, maid, maid);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != maid && !(e instanceof Player) && !maid.isAlliedTo(e));
        for (LivingEntity e : list) {
            if (e.isAlive()) {
                e.hurt(src, damage);
            }
        }
    }

    /**
     * 复刻 DamageSources#source(ResourceKey, Entity, Entity)。
     * 1.20.1 中该三参重载为 private（vanilla 内部使用），Re-Avaritia 靠自身 jar 内 AT 运行时放行，
     * 但不作用于本附属的编译期 classpath（MDG LegacyForge 2.0.91 未自动应用 META-INF/accesstransformer.cfg）。
     * 这里用公共 API 等价构造，语义完全一致：
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
