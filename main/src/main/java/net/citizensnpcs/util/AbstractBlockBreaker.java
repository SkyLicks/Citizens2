package net.citizensnpcs.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.BlockBreaker;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.ai.NPCHolder;

public abstract class AbstractBlockBreaker extends BlockBreaker {
    protected final BlockBreakerConfiguration configuration;
    private int currentDamage;
    private int currentTick;
    protected final Entity entity;
    private boolean isDigging = true;
    private final Location location;
    private boolean setTarget;
    private int startDigTick;
    protected final int x;
    protected final int y;
    protected final int z;

    public AbstractBlockBreaker(org.bukkit.entity.Entity entity, org.bukkit.block.Block target,
            BlockBreakerConfiguration config) {
        this.entity = entity;
        this.x = target.getX();
        this.y = target.getY();
        this.z = target.getZ();
        this.location = target.getLocation();
        this.startDigTick = (int) (System.currentTimeMillis() / 50);
        this.configuration = config;
    }

    private double distance() {
        Location loc = entity.getLocation();
        return Math.sqrt(Math.pow(loc.getX() - x, 2) + Math.pow(loc.getZ() - z, 2));
    }

    protected abstract float getDamage(int tickDifference);

    protected ItemStack getItemStack() {
        return configuration.item() != null ? configuration.item()
                : entity instanceof LivingEntity ? ((LivingEntity) entity).getEquipment().getItemInHand() : null;
    }

    @Override
    public void reset() {
        if (setTarget && entity instanceof NPCHolder) {
            NPC npc = ((NPCHolder) entity).getNPC();
            if (npc != null && npc.getNavigator().isNavigating()) {
                npc.getNavigator().cancelNavigation();
            }
        }
        setTarget = false;
        if (configuration.callback() != null) {
            configuration.callback().run();
        }
        isDigging = false;
        setBlockDamage(currentDamage = -1);
    }

    @Override
    public BehaviorStatus run() {
        if (!entity.isValid()) {
            return BehaviorStatus.FAILURE;
        }
        if (!isDigging) {
            return BehaviorStatus.SUCCESS;
        }
        currentTick = (int) (System.currentTimeMillis() / 50);
        if (configuration.radius() > 0 && distance() >= configuration.radius()) {
            startDigTick = currentTick;
            if (entity instanceof NPCHolder) {
                NPC npc = ((NPCHolder) entity).getNPC();
                if (npc != null && !npc.getNavigator().isNavigating()) {
                    npc.getNavigator().setTarget(location.clone().add(0, 1, 0));
                    npc.getNavigator().getLocalParameters().distanceMargin(configuration.radius() - 1);
                    setTarget = true;
                }
            }
            return BehaviorStatus.RUNNING;
        }
        Util.faceLocation(entity, location);
        if (entity instanceof Player && currentTick % 5 == 0) {
            PlayerAnimation.ARM_SWING.play((Player) entity);
        }
        if (entity.getWorld().getBlockAt(x, y, z).isEmpty()) {
            return BehaviorStatus.SUCCESS;
        } else {
            int tickDifference = currentTick - startDigTick;
            float damage = getDamage(tickDifference);
            if (damage >= 1F) {
                configuration.blockBreaker().accept(entity.getWorld().getBlockAt(x, y, z), getItemStack());
                return BehaviorStatus.SUCCESS;
            }
            int modifiedDamage = (int) (damage * 10.0F);
            if (modifiedDamage != currentDamage) {
                setBlockDamage(modifiedDamage);
                currentDamage = modifiedDamage;
            }
        }
        return BehaviorStatus.RUNNING;
    }

    protected abstract void setBlockDamage(int damage);

    @Override
    public boolean shouldExecute() {
        return !entity.getWorld().getBlockAt(x, y, z).isEmpty();
    }

}