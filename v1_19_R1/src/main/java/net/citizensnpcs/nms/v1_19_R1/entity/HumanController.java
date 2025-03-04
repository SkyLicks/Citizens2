package net.citizensnpcs.nms.v1_19_R1.entity;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.mojang.authlib.GameProfile;

import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.AbstractEntityController;
import net.citizensnpcs.npc.skin.Skin;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class HumanController extends AbstractEntityController {
    public HumanController() {
        super();
    }

    @Override
    protected Entity createEntity(final Location at, final NPC npc) {
        final ServerLevel nmsWorld = ((CraftWorld) at.getWorld()).getHandle();
        String coloredName = npc.getFullName();
        String name = coloredName.length() > 16 ? coloredName.substring(0, 16) : coloredName;
        UUID uuid = npc.getUniqueId();
        if (uuid.version() == 4) { // clear version
            long msb = uuid.getMostSignificantBits();
            msb &= ~0x0000000000004000L;
            msb |= 0x0000000000002000L;
            uuid = new UUID(msb, uuid.getLeastSignificantBits());
        }

        String teamName = Util.getTeamName(uuid);
        if (npc.requiresNameHologram()) {
            name = teamName;
        }

        if (Setting.USE_SCOREBOARD_TEAMS.asBoolean()) {
            Util.generateTeamFor(npc, name, teamName);
        }

        final GameProfile profile = new GameProfile(uuid, name);
        final EntityHumanNPC handle = new EntityHumanNPC(MinecraftServer.getServer(), nmsWorld, profile, null, npc);

        Skin skin = handle.getSkinTracker().getSkin();
        if (skin != null) {
            skin.apply(handle);
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(CitizensAPI.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if (getBukkitEntity() == null || !getBukkitEntity().isValid()
                        || getBukkitEntity() != handle.getBukkitEntity())
                    return;
                boolean removeFromPlayerList = npc.data().get(NPC.REMOVE_FROM_PLAYERLIST_METADATA,
                        Setting.REMOVE_PLAYERS_FROM_PLAYER_LIST.asBoolean());
                NMS.addOrRemoveFromPlayerList(getBukkitEntity(), removeFromPlayerList);
            }
        }, 20);

        handle.getBukkitEntity().setSleepingIgnored(true);

        return handle.getBukkitEntity();
    }

    @Override
    public Player getBukkitEntity() {
        return (Player) super.getBukkitEntity();
    }

    @Override
    public void remove() {
        Player entity = getBukkitEntity();
        if (entity != null) {
            if (Setting.USE_SCOREBOARD_TEAMS.asBoolean()) {
                Util.removeTeamFor(NMS.getNPC(entity), entity.getName());
            }
            NMS.removeFromWorld(entity);
            SkinnableEntity npc = entity instanceof SkinnableEntity ? (SkinnableEntity) entity : null;
            npc.getSkinTracker().onRemoveNPC();
        }
        NMS.remove(entity);
        setEntity(null);
    }
}
