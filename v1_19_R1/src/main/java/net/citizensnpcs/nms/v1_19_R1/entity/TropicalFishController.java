package net.citizensnpcs.nms.v1_19_R1.entity;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftTropicalFish;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_19_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_19_R1.util.NMSImpl;
import net.citizensnpcs.nms.v1_19_R1.util.PlayerMoveControl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TropicalFishController extends MobEntityController {
    public TropicalFishController() {
        super(EntityTropicalFishNPC.class);
    }

    @Override
    public org.bukkit.entity.TropicalFish getBukkitEntity() {
        return (org.bukkit.entity.TropicalFish) super.getBukkitEntity();
    }

    public static class EntityTropicalFishNPC extends TropicalFish implements NPCHolder {
        private final CitizensNPC npc;
        private MoveControl oldMoveController;

        public EntityTropicalFishNPC(EntityType<? extends TropicalFish> types, Level level) {
            this(types, level, null);
        }

        public EntityTropicalFishNPC(EntityType<? extends TropicalFish> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                NMSImpl.clearGoals(npc, goalSelector, targetSelector);
                this.oldMoveController = this.moveControl;
                this.moveControl = new MoveControl(this);
            }
        }

        @Override
        public void aiStep() {
            boolean lastInWater = this.verticalCollision;
            if (npc != null) {
                this.verticalCollision = false;
            }
            super.aiStep();
            if (npc != null) {
                this.verticalCollision = lastInWater;
            }
        }

        @Override
        protected boolean canRide(Entity entity) {
            if (npc != null && (entity instanceof Boat || entity instanceof AbstractMinecart)) {
                return !npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true);
            }
            return super.canRide(entity);
        }

        @Override
        public boolean causeFallDamage(float f, float f1, DamageSource damagesource) {
            if (npc == null || !npc.isFlyable()) {
                return super.causeFallDamage(f, f1, damagesource);
            }
            return false;
        }

        @Override
        public void checkDespawn() {
            if (npc == null) {
                super.checkDespawn();
            }
        }

        @Override
        protected void checkFallDamage(double d0, boolean flag, BlockState iblockdata, BlockPos blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.checkFallDamage(d0, flag, iblockdata, blockposition);
            }
        }

        @Override
        public void customServerAiStep() {
            if (npc != null) {
                if (!npc.useMinecraftAI()) {
                    NMSImpl.setNotInSchool(this);
                }
                NMSImpl.updateMinecraftAIState(npc, this);
                if (npc.useMinecraftAI() && this.moveControl != this.oldMoveController) {
                    this.moveControl = this.oldMoveController;
                }
                if (!npc.useMinecraftAI() && this.moveControl == this.oldMoveController) {
                    this.moveControl = new PlayerMoveControl(this);
                }
            }
            super.customServerAiStep();
            if (npc != null) {
                npc.update();
            }
        }

        @Override
        public void dismountTo(double d0, double d1, double d2) {
            if (npc == null) {
                super.dismountTo(d0, d1, d2);
                return;
            }
            NPCEnderTeleportEvent event = new NPCEnderTeleportEvent(npc);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                super.dismountTo(d0, d1, d2);
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return NMSImpl.getSoundEffect(npc, super.getAmbientSound(), NPC.AMBIENT_SOUND_METADATA);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new TropicalFishNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return NMSImpl.getSoundEffect(npc, super.getDeathSound(), NPC.DEATH_SOUND_METADATA);
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.getHurtSound(damagesource), NPC.HURT_SOUND_METADATA);
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public boolean isLeashed() {
            if (npc == null)
                return super.isLeashed();
            boolean protectedDefault = npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true);
            if (!protectedDefault || !npc.data().get(NPC.LEASH_PROTECTED_METADATA, protectedDefault))
                return super.isLeashed();
            if (super.isLeashed()) {
                dropLeash(true, false); // clearLeash with client update
            }
            return false; // shouldLeash
        }

        @Override
        protected InteractionResult mobInteract(Player entityhuman, InteractionHand enumhand) {
            if (npc == null || !npc.isProtected())
                return super.mobInteract(entityhuman, enumhand);
            ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.getItem() == Items.WATER_BUCKET && isAlive()) {
                return InteractionResult.FAIL;
            }
            return super.mobInteract(entityhuman, enumhand);
        }

        @Override
        public boolean onClimbable() {
            if (npc == null || !npc.isFlyable()) {
                return super.onClimbable();
            } else {
                return false;
            }
        }

        @Override
        public void push(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.push(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public void push(Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.push(entity);
            if (npc != null)
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
        }

        @Override
        public boolean save(CompoundTag save) {
            return npc == null ? super.save(save) : false;
        }

        @Override
        public Entity teleportTo(ServerLevel worldserver, BlockPos location) {
            if (npc == null)
                return super.teleportTo(worldserver, location);
            return NMSImpl.teleportAcrossWorld(this, worldserver, location);
        }

        @Override
        public void travel(Vec3 vec3d) {
            if (npc == null || !npc.isFlyable()) {
                if (!NMSImpl.moveFish(npc, this, vec3d)) {
                    super.travel(vec3d);
                }
            } else {
                NMSImpl.flyingMoveLogic(this, vec3d);
            }
        }
    }

    public static class TropicalFishNPC extends CraftTropicalFish implements ForwardingNPCHolder {
        public TropicalFishNPC(EntityTropicalFishNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
