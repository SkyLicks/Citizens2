package net.citizensnpcs.nms.v1_19_R1.entity;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftGhast;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_19_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_19_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;

public class GhastController extends MobEntityController {
    public GhastController() {
        super(EntityGhastNPC.class);
    }

    @Override
    public org.bukkit.entity.Ghast getBukkitEntity() {
        return (org.bukkit.entity.Ghast) super.getBukkitEntity();
    }

    public static class EntityGhastNPC extends Ghast implements NPCHolder {
        private final CitizensNPC npc;

        public EntityGhastNPC(EntityType<? extends Ghast> types, Level level) {
            this(types, level, null);
        }

        public EntityGhastNPC(EntityType<? extends Ghast> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                NMSImpl.clearGoals(npc, goalSelector, targetSelector);
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
        public void checkDespawn() {
            if (npc == null) {
                super.checkDespawn();
            }
        }

        @Override
        public void customServerAiStep() {
            if (npc != null) {
                npc.update();
                NMSImpl.updateMinecraftAIState(npc, this);
                if (!npc.useMinecraftAI()) {
                    NMSImpl.updateAI(this);
                }
            }
            super.customServerAiStep();
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
                NMSImpl.setBukkitEntity(this, new GhastNPC(this));
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
        public boolean isAutoSpinAttack() {
            return npc != null;
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
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
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
    }

    public static class GhastNPC extends CraftGhast implements ForwardingNPCHolder {
        public GhastNPC(EntityGhastNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
