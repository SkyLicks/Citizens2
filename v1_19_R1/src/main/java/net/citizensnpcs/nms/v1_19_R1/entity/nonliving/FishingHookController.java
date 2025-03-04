package net.citizensnpcs.nms.v1_19_R1.entity.nonliving;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftFishHook;
import org.bukkit.entity.FishHook;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_19_R1.entity.MobEntityController;
import net.citizensnpcs.nms.v1_19_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_19_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FishingHookController extends MobEntityController {
    public FishingHookController() {
        super(EntityFishingHookNPC.class);
    }

    @Override
    public FishHook getBukkitEntity() {
        return (FishHook) super.getBukkitEntity();
    }

    public static class EntityFishingHookNPC extends FishingHook implements NPCHolder {
        private final CitizensNPC npc;

        public EntityFishingHookNPC(EntityType<? extends FishingHook> types, Level level) {
            this(types, level, null);
        }

        public EntityFishingHookNPC(EntityType<? extends FishingHook> types, Level level, NPC npc) {
            super(new ServerPlayer(MinecraftServer.getServer(), (ServerLevel) level,
                    new GameProfile(UUID.randomUUID(), "dummyfishhook"), null) {
            }, level, 0, 0);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public double distanceToSqr(Entity entity) {
            if (entity == getPlayerOwner()) {
                return 0D;
            }
            return super.distanceToSqr(entity);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new FishingHookNPC(this));
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
                ((ServerPlayer) getPlayerOwner()).setHealth(20F);
                getPlayerOwner().unsetRemoved();
                ((ServerPlayer) getPlayerOwner()).getInventory().items.set(
                        ((ServerPlayer) getPlayerOwner()).getInventory().selected, new ItemStack(Items.FISHING_ROD, 1));
                NMSImpl.setLife(this, 0);
                npc.update();
            } else {
                super.tick();
            }
        }
    }

    public static class FishingHookNPC extends CraftFishHook implements ForwardingNPCHolder {
        public FishingHookNPC(EntityFishingHookNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
