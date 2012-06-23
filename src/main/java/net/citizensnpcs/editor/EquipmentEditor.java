package net.citizensnpcs.editor;

import net.citizensnpcs.api.abstraction.EventHandler;
import net.citizensnpcs.api.abstraction.entity.Player;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.bukkit.BukkitConverter;
import net.citizensnpcs.bukkit.BukkitPlayer;
import net.citizensnpcs.util.Messaging;

import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class EquipmentEditor extends Editor {
    private final NPC npc;
    private final Player player;

    public EquipmentEditor(Player player, NPC npc) {
        this.player = player;
        this.npc = npc;
    }

    @Override
    public void begin() {
        Messaging.send(player, "<b>Entered the equipment editor!");
        Messaging.send(player, "<e>Right click <a>to equip the NPC!");
    }

    @Override
    public void end() {
        Messaging.send(player, "<a>Exited the equipment editor.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && Editor.hasEditor(event.getPlayer().getName()))
            event.setUseItemInHand(Result.DENY);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!npc.equals(BukkitConverter.toNPC(event.getRightClicked())) || !event.getPlayer().equals(player))
            return;

        if (npc instanceof Equipable) {
            ((Equipable) npc).equip(new BukkitPlayer(event.getPlayer()));
        }
    }
}