package net.citizensnpcs.nms.v1_15_R1.entity;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.entity.Entity;

import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_15_R1.util.NMSImpl;
import net.citizensnpcs.npc.AbstractEntityController;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.World;

public abstract class MobEntityController extends AbstractEntityController {
    private final Class<?> clazz;

    protected MobEntityController(Class<?> clazz) {
        super(clazz);
        this.clazz = clazz;
    }

    @Override
    protected Entity createEntity(Location at, NPC npc) {
        EntityTypes<?> type = NMSImpl.getEntityType(clazz);
        net.minecraft.server.v1_15_R1.Entity entity = createEntityFromClass(type,
                ((CraftWorld) at.getWorld()).getHandle(), npc);
        entity.setPositionRotation(at.getX(), at.getY(), at.getZ(), at.getYaw(), at.getPitch());

        // entity.onGround isn't updated right away - we approximate here so
        // that things like pathfinding still work *immediately* after spawn.
        org.bukkit.Material beneath = at.getBlock().getRelative(BlockFace.DOWN).getType();
        if (beneath.isSolid()) {
            entity.onGround = true;
        }
        try {
            UUID_FIELD.invoke(entity, npc.getUniqueId());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (Setting.USE_SCOREBOARD_TEAMS.asBoolean()) {
            Util.generateTeamFor(npc, npc.getUniqueId().toString(), Util.getTeamName(npc.getUniqueId()));
        }
        return entity.getBukkitEntity();
    }

    private net.minecraft.server.v1_15_R1.Entity createEntityFromClass(Object... args) {
        try {
            return (net.minecraft.server.v1_15_R1.Entity) getConstructor(clazz).newInstance(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static Constructor<?> getConstructor(Class<?> clazz) {
        Constructor<?> constructor = CONSTRUCTOR_CACHE.get(clazz);
        if (constructor != null)
            return constructor;
        try {
            CONSTRUCTOR_CACHE.put(clazz, constructor = clazz.getConstructor(EntityTypes.class, World.class, NPC.class));
            return constructor;
        } catch (Exception ex) {
            throw new IllegalStateException("unable to find an entity constructor");
        }
    }

    private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new WeakHashMap<Class<?>, Constructor<?>>();
    private static final MethodHandle UUID_FIELD = NMS.getSetter(net.minecraft.server.v1_15_R1.Entity.class,
            "uniqueID");
}