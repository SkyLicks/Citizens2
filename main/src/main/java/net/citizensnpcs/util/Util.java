package net.citizensnpcs.util;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import net.citizensnpcs.api.event.NPCCollisionEvent;
import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.BoundingBox;
import net.citizensnpcs.api.util.SpigotUtil;
import net.citizensnpcs.npc.ai.NPCHolder;

public class Util {
    private Util() {
    }

    public static void callCollisionEvent(NPC npc, Entity entity) {
        if (NPCCollisionEvent.getHandlerList().getRegisteredListeners().length > 0) {
            Bukkit.getPluginManager().callEvent(new NPCCollisionEvent(npc, entity));
        }
    }

    public static Vector callPushEvent(NPC npc, double x, double y, double z) {
        if (npc == null) {
            return new Vector(x, y, z);
        }
        boolean allowed = !npc.isProtected()
                || (npc.data().has(NPC.COLLIDABLE_METADATA) && npc.data().<Boolean> get(NPC.COLLIDABLE_METADATA));
        if (NPCPushEvent.getHandlerList().getRegisteredListeners().length == 0) {
            return allowed ? new Vector(x, y, z) : null;
        }
        // when another entity collides, this method is called to push the NPC so we prevent it from
        // doing anything if the event is cancelled.
        Vector vector = new Vector(x, y, z);
        NPCPushEvent event = new NPCPushEvent(npc, vector);
        event.setCancelled(!allowed);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled() ? event.getCollisionVector() : null;
    }

    /**
     * Clamps the rotation angle to [-180, 180]
     */
    public static float clamp(float angle) {
        while (angle < -180.0F) {
            angle += 360.0F;
        }
        while (angle >= 180.0F) {
            angle -= 360.0F;
        }
        return angle;
    }

    public static void face(Entity entity, float yaw, float pitch) {
        double pitchCos = Math.cos(Math.toRadians(pitch));
        Vector vector = new Vector(Math.sin(Math.toRadians(yaw)) * -pitchCos, -Math.sin(Math.toRadians(pitch)),
                Math.cos(Math.toRadians(yaw)) * pitchCos).normalize();
        faceLocation(entity, entity.getLocation(AT_LOCATION).clone().add(vector));
    }

    public static void faceEntity(Entity entity, Entity to) {
        if (to == null || entity == null || entity.getWorld() != to.getWorld())
            return;
        if (to instanceof LivingEntity) {
            NMS.look(entity, to);
        } else {
            faceLocation(entity, to.getLocation(AT_LOCATION));
        }
    }

    public static void faceLocation(Entity entity, Location to) {
        faceLocation(entity, to, false);
    }

    public static void faceLocation(Entity entity, Location to, boolean headOnly) {
        faceLocation(entity, to, headOnly, true);
    }

    public static void faceLocation(Entity entity, Location to, boolean headOnly, boolean immediate) {
        if (to == null || entity.getWorld() != to.getWorld())
            return;
        NMS.look(entity, to, headOnly, immediate);
    }

    public static void generateTeamFor(NPC npc, String name, String teamName) {
        Scoreboard scoreboard = getDummyScoreboard();
        Team team = scoreboard.getTeam(teamName);
        int mode = 2;
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            if (npc.requiresNameHologram()
                    || npc.data().<Object> get(NPC.Metadata.NAMEPLATE_VISIBLE, true).toString().equals("false")) {
                NMS.setTeamNameTagVisible(team, false);
            }
            mode = 0;
        }
        team.addEntry(name);
        npc.data().set(NPC.SCOREBOARD_FAKE_TEAM_NAME_METADATA, teamName);
        sendTeamPacketToOnlinePlayers(team, mode);
    }

    public static Location getCenterLocation(Block block) {
        Location bloc = block.getLocation(AT_LOCATION);
        Location center = new Location(bloc.getWorld(), bloc.getBlockX() + 0.5, bloc.getBlockY(),
                bloc.getBlockZ() + 0.5);
        BoundingBox bb = NMS.getCollisionBox(block);
        if (bb != null && (bb.maxY - bb.minY) < 0.6D) {
            center.setY(center.getY() + (bb.maxY - bb.minY));
        }
        return center;
    }

    /**
     * Returns the yaw to face along the given velocity (corrected for dragon yaw i.e. facing backwards)
     */
    public static float getDragonYaw(Entity entity, double motX, double motZ) {
        Location location = entity.getLocation(AT_LOCATION);
        double x = location.getX();
        double z = location.getZ();
        double tX = x + motX;
        double tZ = z + motZ;
        if (z > tZ)
            return (float) (-Math.toDegrees(Math.atan((x - tX) / (z - tZ))));
        if (z < tZ) {
            return (float) (-Math.toDegrees(Math.atan((x - tX) / (z - tZ)))) + 180.0F;
        }
        return location.getYaw();
    }

    public static Scoreboard getDummyScoreboard() {
        return DUMMY_SCOREBOARD;
    }

    public static Location getEyeLocation(Entity entity) {
        return entity instanceof LivingEntity ? ((LivingEntity) entity).getEyeLocation() : entity.getLocation();
    }

    public static Material getFallbackMaterial(String first, String second) {
        try {
            return Material.valueOf(first);
        } catch (IllegalArgumentException e) {
            return Material.valueOf(second);
        }
    }

    public static Random getFastRandom() {
        return new XORShiftRNG();
    }

    public static String getMinecraftRevision() {
        if (MINECRAFT_REVISION == null) {
            MINECRAFT_REVISION = Bukkit.getServer().getClass().getPackage().getName();
        }
        return MINECRAFT_REVISION.substring(MINECRAFT_REVISION.lastIndexOf('.') + 2);
    }

    public static String getTeamName(UUID id) {
        return "CIT-" + id.toString().replace("-", "").substring(0, 12);
    }

    public static boolean inBlock(Entity entity) {
        // TODO: bounding box aware?
        Location loc = entity.getLocation(AT_LOCATION);
        if (!Util.isLoaded(loc)) {
            return false;
        }
        Block in = loc.getBlock();
        Block above = in.getRelative(BlockFace.UP);
        return in.getType().isSolid() && above.getType().isSolid() && NMS.isSolid(in) && NMS.isSolid(above);
    }

    public static boolean isAlwaysFlyable(EntityType type) {
        if (type.name().toLowerCase().equals("vex") || type.name().toLowerCase().equals("parrot")
                || type.name().toLowerCase().equals("allay") || type.name().toLowerCase().equals("bee")
                || type.name().toLowerCase().equals("phantom"))
            // 1.8.8 compatibility
            return true;
        switch (type) {
            case BAT:
            case BLAZE:
            case ENDER_DRAGON:
            case GHAST:
            case WITHER:
                return true;
            default:
                return false;
        }
    }

    public static boolean isHorse(EntityType type) {
        String name = type.name();
        return type == EntityType.HORSE || name.contains("_HORSE") || name.equals("DONKEY") || name.equals("MULE")
                || name.equals("LLAMA") || name.equals("TRADER_LLAMA");
    }

    public static boolean isLoaded(Location location) {
        if (location.getWorld() == null)
            return false;
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return location.getWorld().isChunkLoaded(chunkX, chunkZ);
    }

    public static boolean isOffHand(PlayerInteractEntityEvent event) {
        try {
            return event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND;
        } catch (NoSuchMethodError e) {
            return false;
        } catch (NoSuchFieldError e) {
            return false;
        }
    }

    public static boolean isOffHand(PlayerInteractEvent event) {
        try {
            return event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND;
        } catch (NoSuchMethodError e) {
            return false;
        } catch (NoSuchFieldError e) {
            return false;
        }
    }

    public static String listValuesPretty(Enum<?>[] values) {
        return "<e>" + Joiner.on("<a>, <e>").join(values).toLowerCase();
    }

    public static boolean locationWithinRange(Location current, Location target, double range) {
        if (current == null || target == null)
            return false;
        if (current.getWorld() != target.getWorld())
            return false;
        return current.distance(target) <= range;
    }

    public static EntityType matchEntityType(String toMatch) {
        return matchEnum(EntityType.values(), toMatch);
    }

    public static <T extends Enum<?>> T matchEnum(T[] values, String toMatch) {
        toMatch = toMatch.toLowerCase().replace('-', '_').replace(' ', '_');
        for (T check : values) {
            if (toMatch.equals(check.name().toLowerCase())
                    || (toMatch.equals("item") && check == EntityType.DROPPED_ITEM)) {
                return check; // check for an exact match first
            }
        }
        for (T check : values) {
            String name = check.name().toLowerCase();
            if (name.replace("_", "").equals(toMatch) || name.startsWith(toMatch)) {
                return check;
            }
        }
        return null;
    }

    public static boolean matchesItemInHand(Player player, String setting) {
        String parts = setting;
        if (parts.contains("*") || parts.isEmpty())
            return true;
        for (String part : Splitter.on(',').split(parts)) {
            Material matchMaterial = SpigotUtil.isUsing1_13API() ? Material.matchMaterial(part, false)
                    : Material.matchMaterial(part);
            if (matchMaterial == null) {
                if (part.equals("280")) {
                    matchMaterial = Material.STICK;
                } else if (part.equals("340")) {
                    matchMaterial = Material.BOOK;
                }
            }
            if (matchMaterial == player.getInventory().getItemInHand().getType()) {
                return true;
            }
        }
        return false;
    }

    public static Set<EntityType> optionalEntitySet(String... types) {
        Set<EntityType> list = EnumSet.noneOf(EntityType.class);
        for (String type : types) {
            try {
                list.add(EntityType.valueOf(type));
            } catch (IllegalArgumentException e) {
            }
        }
        return list;
    }

    public static String prettyEnum(Enum<?> e) {
        return e.name().toLowerCase().replace('_', ' ');
    }

    public static String prettyPrintLocation(Location to) {
        return String.format("%s at %d, %d, %d (%d, %d)", to.getWorld().getName(), to.getBlockX(), to.getBlockY(),
                to.getBlockZ(), (int) to.getYaw(), (int) to.getPitch());
    }

    public static void removeTeamFor(NPC npc, String name) {
        String teamName = npc.data().get(NPC.SCOREBOARD_FAKE_TEAM_NAME_METADATA, "");
        if (teamName.isEmpty())
            return;
        Team team = getDummyScoreboard().getTeam(teamName);
        npc.data().remove(NPC.SCOREBOARD_FAKE_TEAM_NAME_METADATA);
        if (team == null)
            return;
        if (team.hasEntry(name)) {
            if (team.getSize() == 1) {
                sendTeamPacketToOnlinePlayers(team, 1);
                team.unregister();
            } else {
                team.removeEntry(name);
            }
        }
    }

    /**
     * @param mode
     *            0 for create, 1 for remove, 2 for update
     */
    public static void sendTeamPacketToOnlinePlayers(Team team, int mode) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            NMS.sendTeamPacket(player, team, mode);
        }
    }

    /**
     * Sets the entity's yaw and pitch directly including head yaw.
     */
    public static void setRotation(Entity entity, float yaw, float pitch) {
        NMS.look(entity, yaw, pitch);
    }

    public static void updateNPCTeams(Player toUpdate, int mode) {
        for (Player player : PlayerUpdateTask.getRegisteredPlayerNPCs()) {
            NPC npc = ((NPCHolder) player).getNPC();

            String teamName = npc.data().get(NPC.SCOREBOARD_FAKE_TEAM_NAME_METADATA, "");
            Team team = null;
            if (teamName.length() == 0 || (team = Util.getDummyScoreboard().getTeam(teamName)) == null)
                continue;

            NMS.sendTeamPacket(toUpdate, team, mode);
        }
    }

    private static final Location AT_LOCATION = new Location(null, 0, 0, 0);
    private static final Scoreboard DUMMY_SCOREBOARD = Bukkit.getScoreboardManager().getNewScoreboard();
    private static String MINECRAFT_REVISION;
}
