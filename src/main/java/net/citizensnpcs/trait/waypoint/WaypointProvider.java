package net.citizensnpcs.trait.waypoint;

import net.citizensnpcs.api.abstraction.entity.Player;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.editor.Editor;

public interface WaypointProvider {

    /**
     * Creates an {@link Editor} with the given {@link Player}.
     * 
     * @param player
     *            The player to link the editor with
     * @return The editor
     */
    public Editor createEditor(Player player);

    /**
     * Loads from the specified {@link DataKey}.
     * 
     * @param key
     *            The key to load from
     */
    public void load(DataKey key);

    /**
     * Saves to the specified {@link DataKey}.
     * 
     * @param key
     *            The key to save to
     */
    public void save(DataKey key);

    public void onAttach();
}