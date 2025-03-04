package net.citizensnpcs.nms.v1_19_R1.util;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.util.Set;

import com.mojang.datafixers.DataFixer;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.util.NMS;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class EmptyAdvancementDataPlayer extends PlayerAdvancements {
    public EmptyAdvancementDataPlayer(DataFixer datafixer, PlayerList playerlist,
            ServerAdvancementManager advancementdataworld, File file, ServerPlayer entityplayer) {
        super(datafixer, playerlist, advancementdataworld, CitizensAPI.getDataFolder(), entityplayer);
        this.save();
    }

    @Override
    public boolean award(Advancement advancement, String s) {
        return false;
    }

    @Override
    public void flushDirty(ServerPlayer entityplayer) {
    }

    @Override
    public AdvancementProgress getOrStartProgress(Advancement advancement) {
        return new AdvancementProgress();
    }

    @Override
    public boolean revoke(Advancement advancement, String s) {
        return false;
    }

    @Override
    public void save() {
        clear(this);
    }

    @Override
    public void setPlayer(ServerPlayer entityplayer) {
    }

    @Override
    public void setSelectedTab(Advancement advancement) {
    }

    @Override
    public void stopListening() {
    }

    public static void clear(PlayerAdvancements data) {
        data.stopListening();
        data.advancements.clear();
        try {
            ((Set<?>) I.invoke(data)).clear();
            ((Set<?>) J.invoke(data)).clear();
            ((Set<?>) K.invoke(data)).clear();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static final MethodHandle I = NMS.getGetter(PlayerAdvancements.class, "i");
    private static final MethodHandle J = NMS.getGetter(PlayerAdvancements.class, "j");
    private static final MethodHandle K = NMS.getGetter(PlayerAdvancements.class, "k");
}
