package net.citizensnpcs.nms.v1_19_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftShulkerBullet;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_19_R1.entity.MobEntityController;
import net.citizensnpcs.nms.v1_19_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_19_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;

public class ShulkerBulletController extends MobEntityController {
    public ShulkerBulletController() {
        super(EntityShulkerBulletNPC.class);
    }

    @Override
    public org.bukkit.entity.ShulkerBullet getBukkitEntity() {
        return (org.bukkit.entity.ShulkerBullet) super.getBukkitEntity();
    }

    public static class EntityShulkerBulletNPC extends ShulkerBullet implements NPCHolder {
        private final CitizensNPC npc;

        public EntityShulkerBulletNPC(EntityType<? extends ShulkerBullet> types, Level level) {
            this(types, level, null);
        }

        public EntityShulkerBulletNPC(EntityType<? extends ShulkerBullet> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new ShulkerBulletNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
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

        @Override
        public void tick() {
            if (npc != null) {
                npc.update();
            } else {
                super.tick();
            }
        }
    }

    public static class ShulkerBulletNPC extends CraftShulkerBullet implements ForwardingNPCHolder {
        public ShulkerBulletNPC(EntityShulkerBulletNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
