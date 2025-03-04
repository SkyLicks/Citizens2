package net.citizensnpcs.nms.v1_11_R1.entity;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;

import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Inventory;
import net.citizensnpcs.nms.v1_11_R1.network.EmptyNetHandler;
import net.citizensnpcs.nms.v1_11_R1.network.EmptyNetworkManager;
import net.citizensnpcs.nms.v1_11_R1.network.EmptySocket;
import net.citizensnpcs.nms.v1_11_R1.util.NMSImpl;
import net.citizensnpcs.nms.v1_11_R1.util.PlayerControllerJump;
import net.citizensnpcs.nms.v1_11_R1.util.PlayerControllerMove;
import net.citizensnpcs.nms.v1_11_R1.util.PlayerNavigation;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.npc.skin.SkinPacketTracker;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.SkinTrait;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_11_R1.AttributeInstance;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.ChatComponentText;
import net.minecraft.server.v1_11_R1.DamageSource;
import net.minecraft.server.v1_11_R1.EntityHuman;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.EnumGamemode;
import net.minecraft.server.v1_11_R1.EnumItemSlot;
import net.minecraft.server.v1_11_R1.EnumProtocolDirection;
import net.minecraft.server.v1_11_R1.GenericAttributes;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.IChatBaseComponent;
import net.minecraft.server.v1_11_R1.ItemStack;
import net.minecraft.server.v1_11_R1.MinecraftServer;
import net.minecraft.server.v1_11_R1.NavigationAbstract;
import net.minecraft.server.v1_11_R1.NetworkManager;
import net.minecraft.server.v1_11_R1.Packet;
import net.minecraft.server.v1_11_R1.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_11_R1.PathType;
import net.minecraft.server.v1_11_R1.PlayerInteractManager;
import net.minecraft.server.v1_11_R1.WorldServer;

public class EntityHumanNPC extends EntityPlayer implements NPCHolder, SkinnableEntity {
    private final Map<PathType, Float> bz = Maps.newEnumMap(PathType.class);
    private PlayerControllerJump controllerJump;
    private PlayerControllerMove controllerMove;
    private final Map<EnumItemSlot, ItemStack> equipmentCache = Maps.newEnumMap(EnumItemSlot.class);
    private boolean isTracked = false;
    private int jumpTicks = 0;
    private PlayerNavigation navigation;
    private final CitizensNPC npc;
    private final Location packetLocationCache = new Location(null, 0, 0, 0);
    private final SkinPacketTracker skinTracker;
    private int updateCounter = 0;

    public EntityHumanNPC(MinecraftServer minecraftServer, WorldServer world, GameProfile gameProfile,
            PlayerInteractManager playerInteractManager, NPC npc) {
        super(minecraftServer, world, gameProfile, playerInteractManager);

        this.npc = (CitizensNPC) npc;
        if (npc != null) {
            skinTracker = new SkinPacketTracker(this);
            playerInteractManager.setGameMode(EnumGamemode.SURVIVAL);
            initialise(minecraftServer);
        } else {
            skinTracker = null;
        }
    }

    @Override
    protected void a(double d0, boolean flag, IBlockData block, BlockPosition blockposition) {
        if (npc == null || !npc.isFlyable()) {
            super.a(d0, flag, block, blockposition);
        }
    }

    @Override
    public boolean a(EntityPlayer entityplayer) {
        if (npc != null && !isTracked) {
            return false;
        }
        return super.a(entityplayer);
    }

    public float a(PathType pathtype) {
        return this.bz.containsKey(pathtype) ? this.bz.get(pathtype).floatValue() : pathtype.a();
    }

    public void a(PathType pathtype, float f) {
        this.bz.put(pathtype, Float.valueOf(f));
    }

    @Override
    public void A_() {
        super.A_();
        if (npc == null)
            return;
        this.noclip = isSpectator();
        if (updateCounter + 1 > Setting.PACKET_UPDATE_DELAY.asInt()) {
            updateEffects = true;
        }
        Bukkit.getServer().getPluginManager().unsubscribeFromPermission("bukkit.broadcast.user", bukkitEntity);

        boolean navigating = npc.getNavigator().isNavigating();
        updatePackets(navigating);

        npc.update();
    }

    @Override
    public void collide(net.minecraft.server.v1_11_R1.Entity entity) {
        // this method is called by both the entities involved - cancelling
        // it will not stop the NPC from moving.
        super.collide(entity);
        if (npc != null) {
            Util.callCollisionEvent(npc, entity.getBukkitEntity());
        }
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        // knock back velocity is cancelled and sent to client for handling when
        // the entity is a player. there is no client so make this happen
        // manually.
        boolean damaged = super.damageEntity(damagesource, f);
        if (damaged && velocityChanged) {
            velocityChanged = false;
            Bukkit.getScheduler().runTask(CitizensAPI.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    EntityHumanNPC.this.velocityChanged = true;
                }
            });
        }
        return damaged;
    }

    @Override
    public void die(DamageSource damagesource) {
        // players that die are not normally removed from the world. when the
        // NPC dies, we are done with the instance and it should be removed.
        if (dead) {
            return;
        }
        super.die(damagesource);
        Bukkit.getScheduler().runTaskLater(CitizensAPI.getPlugin(), new Runnable() {
            @Override
            public void run() {
                world.removeEntity(EntityHumanNPC.this);
            }
        }, 15); // give enough time for death and smoke animation
    }

    @Override
    public void e(float f, float f1) {
        if (npc == null || !npc.isFlyable()) {
            super.e(f, f1);
        }
    }

    @Override
    public void enderTeleportTo(double d0, double d1, double d2) {
        if (npc == null)
            super.enderTeleportTo(d0, d1, d2);
        NPCEnderTeleportEvent event = new NPCEnderTeleportEvent(npc);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            super.enderTeleportTo(d0, d1, d2);
        }
    }

    @Override
    public void f(double x, double y, double z) {
        Vector vector = Util.callPushEvent(npc, x, y, z);
        if (vector != null) {
            super.f(vector.getX(), vector.getY(), vector.getZ());
        }
    }

    @Override
    public void g(float f, float f1) {
        if (npc == null || !npc.isFlyable()) {
            super.g(f, f1);
        } else {
            NMSImpl.flyingMoveLogic(this, f, f1);
        }
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        if (npc != null && !(bukkitEntity instanceof NPCHolder)) {
            bukkitEntity = new PlayerNPC(this);
        }
        return super.getBukkitEntity();
    }

    public PlayerControllerJump getControllerJump() {
        return controllerJump;
    }

    public PlayerControllerMove getControllerMove() {
        return controllerMove;
    }

    public NavigationAbstract getNavigation() {
        return navigation;
    }

    @Override
    public NPC getNPC() {
        return npc;
    }

    @Override
    public IChatBaseComponent getPlayerListName() {
        if (npc.data().get(NPC.REMOVE_FROM_PLAYERLIST_METADATA, Setting.REMOVE_PLAYERS_FROM_PLAYER_LIST.asBoolean())) {
            return new ChatComponentText("");
        }
        return super.getPlayerListName();
    }

    @Override
    public String getSkinName() {
        String skinName = npc.getOrAddTrait(SkinTrait.class).getSkinName();
        if (skinName == null) {
            skinName = npc.getName();
        }
        return skinName.toLowerCase();
    }

    @Override
    public SkinPacketTracker getSkinTracker() {
        return skinTracker;
    }

    @Override
    public boolean inBlock() {
        if (npc == null || noclip || isSleeping()) {
            return super.inBlock();
        }
        return Util.inBlock(getBukkitEntity());
    }

    private void initialise(MinecraftServer minecraftServer) {
        Socket socket = new EmptySocket();
        NetworkManager conn = null;
        try {
            conn = new EmptyNetworkManager(EnumProtocolDirection.CLIENTBOUND);
            playerConnection = new EmptyNetHandler(minecraftServer, conn, this);
            conn.setPacketListener(playerConnection);
            socket.close();
        } catch (IOException e) {
            // swallow
        }

        AttributeInstance range = getAttributeInstance(GenericAttributes.FOLLOW_RANGE);
        if (range == null) {
            range = getAttributeMap().b(GenericAttributes.FOLLOW_RANGE);
        }
        range.setValue(Setting.DEFAULT_PATHFINDING_RANGE.asDouble());

        controllerJump = new PlayerControllerJump(this);
        controllerMove = new PlayerControllerMove(this);
        navigation = new PlayerNavigation(this, world);
        invulnerableTicks = 0;
        NMS.setStepHeight(getBukkitEntity(), 1); // the default (0) breaks step climbing

        setSkinFlags((byte) 0xFF);
    }

    @Override
    public boolean isCollidable() {
        return npc == null ? super.isCollidable()
                : npc.data().has(NPC.COLLIDABLE_METADATA) ? npc.data().<Boolean> get(NPC.COLLIDABLE_METADATA)
                        : !npc.isProtected();
    }

    public boolean isNavigating() {
        return npc.getNavigator().isNavigating();
    }

    @Override
    public boolean m_() {
        if (npc == null || !npc.isFlyable()) {
            return super.m_();
        } else {
            return false;
        }
    }

    private void moveOnCurrentHeading() {
        if (bd) {
            if (onGround && jumpTicks == 0) {
                cm();
                jumpTicks = 10;
            }
        } else {
            jumpTicks = 0;
        }
        be *= 0.98F;
        bf *= 0.98F;
        bg *= 0.9F;
        moveWithFallDamage(be, bf); // movement method
        NMS.setHeadYaw(getBukkitEntity(), yaw);
        if (jumpTicks > 0) {
            jumpTicks--;
        }
    }

    private void moveWithFallDamage(float mx, float my) {
        double y = this.locY;

        g(mx, my);
        if (!npc.isProtected()) {
            a(this.locY - y, onGround);
        }
    }

    @Override
    public void playerTick() {
        if (npc == null) {
            super.playerTick();
            return;
        }
        cA();
        boolean navigating = npc.getNavigator().isNavigating();
        if (!navigating && getBukkitEntity() != null
                && (!npc.hasTrait(Gravity.class) || npc.getOrAddTrait(Gravity.class).hasGravity())
                && Util.isLoaded(getBukkitEntity().getLocation(LOADED_LOCATION))) {
            moveWithFallDamage(0, 0);
        }
        if (Math.abs(motX) < EPSILON && Math.abs(motY) < EPSILON && Math.abs(motZ) < EPSILON) {
            motX = motY = motZ = 0;
        }
        if (navigating) {
            if (!NMSImpl.isNavigationFinished(navigation)) {
                NMSImpl.updateNavigation(navigation);
            }
            moveOnCurrentHeading();
        }
        NMSImpl.updateAI(this);

        if (npc.data().get(NPC.Metadata.COLLIDABLE, !npc.isProtected())) {
            ct();
        }
    }

    public void setMoveDestination(double x, double y, double z, double speed) {
        controllerMove.a(x, y, z, speed);
    }

    public void setShouldJump() {
        controllerJump.a();
    }

    @Override
    public void setSkinFlags(byte flags) {
        // set skin flag byte
        getDataWatcher().set(EntityHuman.bq, flags);
    }

    @Override
    public void setSkinName(String name) {
        npc.getOrAddTrait(SkinTrait.class).setSkinName(name);
    }

    @Override
    public void setSkinName(String name, boolean forceUpdate) {
        npc.getOrAddTrait(SkinTrait.class).setSkinName(name, forceUpdate);
    }

    @Override
    public void setSkinPersistent(String skinName, String signature, String data) {
        npc.getOrAddTrait(SkinTrait.class).setSkinPersistent(skinName, signature, data);
    }

    public void setTracked() {
        isTracked = true;
    }

    public void updateAI() {
        controllerMove.c();
        controllerJump.b();
    }

    private void updatePackets(boolean navigating) {
        if (updateCounter++ <= npc.data().<Integer> get(NPC.Metadata.PACKET_UPDATE_DELAY,
                Setting.PACKET_UPDATE_DELAY.asInt()))
            return;
        updateCounter = 0;
        boolean itemChanged = false;
        for (EnumItemSlot slot : EnumItemSlot.values()) {
            ItemStack equipment = getEquipment(slot);
            ItemStack cache = equipmentCache.get(slot);
            if (!(cache == null && equipment == null)
                    && (cache == null ^ equipment == null || !ItemStack.equals(cache, equipment))) {
                itemChanged = true;
            }
            equipmentCache.put(slot, equipment);
        }
        if (!itemChanged)
            return;

        Location current = getBukkitEntity().getLocation(packetLocationCache);
        Packet<?>[] packets = new Packet[EnumItemSlot.values().length];
        int i = 0;
        for (EnumItemSlot slot : EnumItemSlot.values()) {
            packets[i++] = new PacketPlayOutEntityEquipment(getId(), slot, getEquipment(slot));
        }
        NMSImpl.sendPacketsNearby(getBukkitEntity(), current, packets);
    }

    public void updatePathfindingRange(float pathfindingRange) {
        this.navigation.setRange(pathfindingRange);
    }

    public static class PlayerNPC extends CraftPlayer implements NPCHolder, SkinnableEntity {
        private final CraftServer cserver;
        private final CitizensNPC npc;

        private PlayerNPC(EntityHumanNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
            this.cserver = (CraftServer) Bukkit.getServer();
            npc.getOrAddTrait(Inventory.class);
        }

        @Override
        public Player getBukkitEntity() {
            return this;
        }

        @Override
        public EntityHumanNPC getHandle() {
            return (EntityHumanNPC) this.entity;
        }

        @Override
        public List<MetadataValue> getMetadata(String metadataKey) {
            return cserver.getEntityMetadata().getMetadata(this, metadataKey);
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public String getSkinName() {
            return ((SkinnableEntity) this.entity).getSkinName();
        }

        @Override
        public SkinPacketTracker getSkinTracker() {
            return ((SkinnableEntity) this.entity).getSkinTracker();
        }

        @Override
        public boolean hasMetadata(String metadataKey) {
            return cserver.getEntityMetadata().hasMetadata(this, metadataKey);
        }

        @Override
        public void removeMetadata(String metadataKey, Plugin owningPlugin) {
            cserver.getEntityMetadata().removeMetadata(this, metadataKey, owningPlugin);
        }

        @Override
        public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
            cserver.getEntityMetadata().setMetadata(this, metadataKey, newMetadataValue);
        }

        @Override
        public void setSkinFlags(byte flags) {
            ((SkinnableEntity) this.entity).setSkinFlags(flags);
        }

        @Override
        public void setSkinName(String name) {
            ((SkinnableEntity) this.entity).setSkinName(name);
        }

        @Override
        public void setSkinName(String skinName, boolean forceUpdate) {
            ((SkinnableEntity) this.entity).setSkinName(skinName, forceUpdate);
        }

        @Override
        public void setSkinPersistent(String skinName, String signature, String data) {
            ((SkinnableEntity) this.entity).setSkinPersistent(skinName, signature, data);
        }
    }

    private static final float EPSILON = 0.003F;
    private static final Location LOADED_LOCATION = new Location(null, 0, 0, 0);
}
