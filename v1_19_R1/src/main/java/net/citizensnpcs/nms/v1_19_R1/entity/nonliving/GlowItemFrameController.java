package net.citizensnpcs.nms.v1_19_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftGlowItemFrame;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_19_R1.entity.MobEntityController;
import net.citizensnpcs.nms.v1_19_R1.entity.nonliving.ItemFrameController.EntityItemFrameNPC;
import net.citizensnpcs.nms.v1_19_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.level.Level;

public class GlowItemFrameController extends MobEntityController {
    public GlowItemFrameController() {
        super(EntityItemFrameNPC.class);
    }

    @Override
    protected org.bukkit.entity.Entity createEntity(Location at, NPC npc) {
        org.bukkit.entity.Entity e = super.createEntity(at, npc);
        GlowItemFrame item = (GlowItemFrame) ((CraftEntity) e).getHandle();
        item.setDirection(Direction.EAST);
        item.pos = new BlockPos(at.getX(), at.getY(), at.getZ());
        return e;
    }

    @Override
    public org.bukkit.entity.GlowItemFrame getBukkitEntity() {
        return (org.bukkit.entity.GlowItemFrame) super.getBukkitEntity();
    }

    public static class EntityGlowItemFrameNPC extends GlowItemFrame implements NPCHolder {
        private final CitizensNPC npc;

        public EntityGlowItemFrameNPC(EntityType<? extends GlowItemFrame> types, Level level) {
            this(types, level, null);
        }

        public EntityGlowItemFrameNPC(EntityType<? extends GlowItemFrame> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new GlowItemFrameNPC(this));
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
        public boolean survives() {
            return npc == null || !npc.isProtected() ? super.survives() : true;
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

    public static class GlowItemFrameNPC extends CraftGlowItemFrame implements NPCHolder {
        private final CitizensNPC npc;

        public GlowItemFrameNPC(EntityGlowItemFrameNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
            getItem().setAmount(npc.data().get(NPC.ITEM_AMOUNT_METADATA, 1));
            getItem().setType(Material.getMaterial(npc.data().<String> get(NPC.ITEM_ID_METADATA, "STONE"), false));
            getItem().setDurability(npc.data().<Short> get(NPC.ITEM_DATA_METADATA,
                    npc.data().<Short> get("falling-block-data", (short) 0)));
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        public void setType(Material material, int data) {
            npc.data().setPersistent(NPC.ITEM_ID_METADATA, material.name());
            npc.data().setPersistent(NPC.ITEM_DATA_METADATA, data);
            if (npc.isSpawned()) {
                npc.despawn(DespawnReason.PENDING_RESPAWN);
                npc.spawn(npc.getStoredLocation(), SpawnReason.RESPAWN);
            }
        }
    }
}
