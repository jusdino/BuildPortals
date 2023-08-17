package space.frahm.buildportals;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


public class Cloner {
    public <T extends Entity> T clone(T entity, Location destination) {
        World world = destination.getWorld();
        if ( world == null ) {
            return null;
        }
        EntityAttributeCopier copier;
        if (entity instanceof Minecart) {
            copier = new MinecartAttributeCopier();
        } else if (entity instanceof Boat) {
            copier = new BoatAttributeCopier();
        } else if (entity instanceof Villager) {
            copier = new VillagerAttributeCopier();
        } else if (entity instanceof Horse) {
            copier = new HorseAttributeCopier();
        } else if (entity instanceof Llama) {
            copier = new LlamaAttributeCopier();
        } else if (entity instanceof Sheep) {
            copier = new SheepAttributeCopier();
        } else if (entity instanceof Pig) {
            copier = new PigAttributeCopier();
        } else if (entity instanceof Camel) {
            copier = new CamelAttributeCopier();
        } else if (entity instanceof AbstractHorse) {
            copier = new AbstractHorseAttributeCopier();
        } else if (entity instanceof Animals) {
            copier = new AnimalAttributeCopier();
        } else {
            copier = new EntityAttributeCopier();
        }
        // Unchecked cast here - we're depending on entity.getClass() returning T
        T clonedEntity = (T)world.spawn(destination, (Class<T>)entity.getClass());
        try {
            copier.copyAttributes(entity, clonedEntity, destination);
        } catch (Exception exc) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Error cloning " + entity + ": " + exc);
            clonedEntity.remove();
            return null;
        }
        return clonedEntity;
    }

    private interface AttributeCopier {
        public void copyAttributes(Entity entity, Entity destEntity, Location destination);
    }

    private class VehicleAttributeCopier implements AttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering VehicleAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Vehicle) || !(entity instanceof Vehicle)) {
                throw new RuntimeException("destEntity and entity must both be Vehicles");
            }
            Vehicle destVehicle = (Vehicle) destEntity;
            Vehicle vehicle = (Vehicle) entity;

            Vector speedVec = vehicle.getVelocity();
            double speed = Math.sqrt(speedVec.getX()*speedVec.getX() + speedVec.getY()*speedVec.getY() + speedVec.getZ()*speedVec.getZ());
            //Set minimum exit velocity
            if (speed < 0.1) {
                speed = 0.1;
            }
            Vector destVec = destination.getDirection().multiply(speed);
            destVehicle.setVelocity(destVec);
        }
    }

    private class InventoryHolderAttributeCopier implements AttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering InventoryHolderAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof InventoryHolder) || !(entity instanceof InventoryHolder)) {
                throw new RuntimeException("destEntity and entity must both be InventoryHolders");
            }
            InventoryHolder destInventoryHolder = (InventoryHolder) destEntity;
            InventoryHolder inventoryHolder = (InventoryHolder) entity;
            // Sometimes, Inventories don't seem to be the size expected, so we do this little dance to be
            // sure we're only copying over the expected items
            Inventory inventory = ((InventoryHolder)inventoryHolder).getInventory();
            BuildPortals.logger.log(BuildPortals.logLevel, "Inventory size: " + inventory.getSize());
            BuildPortals.logger.log(BuildPortals.logLevel, "Inventory contents length: " + inventory.getContents().length);
            BuildPortals.logger.log(BuildPortals.logLevel, "Inventory type: " + inventory.getType().name() + ", " + inventory.getType());
            BuildPortals.logger.log(BuildPortals.logLevel, "Inventory type default size: " + inventory.getType().getDefaultSize());
            BuildPortals.logger.log(BuildPortals.logLevel, "Inventory contents: " + inventory.getContents());
            for (ItemStack item: inventory.getContents()) {
                if (item != null) {
                    BuildPortals.logger.log(BuildPortals.logLevel, "Inventory contents type: " + item.getType());
                    BuildPortals.logger.log(BuildPortals.logLevel, "Inventory contents count: " + item.getAmount());
                } else {
                    BuildPortals.logger.log(BuildPortals.logLevel, "Inventory contents type: null");
                }
            }
            // ItemStack[] items = Arrays.copyOf(
            //     inventory.getContents(),
            //     Math.min(inventory.getContents().length, inventory.getSize())
            // );
            ItemStack[] items = inventory.getContents().clone();
            destInventoryHolder.getInventory().setContents(items);
            inventoryHolder.getInventory().clear();
        }
    }

    private class EntityAttributeCopier implements AttributeCopier {
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering EntityCloner.copyAttributes() method");
            destEntity.setCustomName(entity.getCustomName());
            destEntity.setCustomNameVisible(entity.isCustomNameVisible());
            destEntity.setFallDistance(entity.getFallDistance());
            destEntity.setFireTicks(entity.getFireTicks());
            destEntity.setFreezeTicks(entity.getFreezeTicks());
            destEntity.setGlowing(entity.isGlowing());
            destEntity.setGravity(entity.hasGravity());
            destEntity.setInvulnerable(entity.isInvulnerable());
            destEntity.setLastDamageCause(entity.getLastDamageCause());
            destEntity.setPortalCooldown(entity.getPortalCooldown());
            destEntity.setSilent(entity.isSilent());
            destEntity.setTicksLived(entity.getTicksLived());
            destEntity.setVisibleByDefault(entity.isVisibleByDefault());
            destEntity.setVisualFire(entity.isVisualFire());
            if ((entity instanceof Vehicle) && (destEntity instanceof Vehicle)) {
                VehicleAttributeCopier vehicleCopier = new VehicleAttributeCopier();
                vehicleCopier.copyAttributes(entity, destEntity, destination);
            }
            if ((entity instanceof InventoryHolder) && (destEntity instanceof InventoryHolder)) {
                InventoryHolderAttributeCopier invCopier = new InventoryHolderAttributeCopier();
                invCopier.copyAttributes(entity, destEntity, destination);
            }
        }
    }

    private class AnimalAttributeCopier extends EntityAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering AnimalAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Animals) || !(entity instanceof Animals)) {
                throw new RuntimeException("destEntity and entity must both be Animals");
            }
            Animals destAnimal = (Animals) destEntity;
            Animals animal = (Animals) entity;

            super.copyAttributes(animal, destAnimal, destination);
            // LivingEntity
            destAnimal.setAI(animal.hasAI());
            destAnimal.setArrowCooldown(animal.getArrowCooldown());
            destAnimal.setArrowsInBody(animal.getArrowsInBody());
            destAnimal.setCanPickupItems(animal.getCanPickupItems());
            destAnimal.setCollidable(animal.isCollidable());
            destAnimal.setGliding(animal.isGliding());
            destAnimal.setInvisible(animal.isInvisible());
            destAnimal.setLastDamage(animal.getLastDamage());
            destAnimal.setMaximumAir(animal.getMaximumAir());
            destAnimal.setMaximumNoDamageTicks(animal.getMaximumNoDamageTicks());
            destAnimal.setNoActionTicks(animal.getNoActionTicks());
            destAnimal.setNoDamageTicks(animal.getNoDamageTicks());
            destAnimal.setRemainingAir(animal.getRemainingAir());
            destAnimal.setRemoveWhenFarAway(animal.getRemoveWhenFarAway());
            destAnimal.setSwimming(animal.isSwimming());

            // Ageable
            destAnimal.setAge(animal.getAge());

            // Breedable
            destAnimal.setBreed(animal.canBreed());
            destAnimal.setAgeLock(animal.getAgeLock());

            // Damageable
            destAnimal.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(animal.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            destAnimal.setHealth(animal.getHealth());
            destAnimal.setAbsorptionAmount(animal.getAbsorptionAmount());

            // Animals
            destAnimal.setBreedCause(animal.getBreedCause());
            destAnimal.setLoveModeTicks(animal.getLoveModeTicks());

        }
    }

    private class AbstractHorseAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering AbstractHorseAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof AbstractHorse) || !(entity instanceof AbstractHorse)) {
                throw new RuntimeException("destEntity and entity must both be AbstractHorses");
            }
            AbstractHorse destHorse = (AbstractHorse) destEntity;
            AbstractHorse horse = (AbstractHorse) entity;

            // This must be set BEFORE any attempts are made at copying inventories over
            if ((horse instanceof ChestedHorse) && (destHorse instanceof ChestedHorse)) {
                AttributeCopier copier = new ChestedHorseAttributeCopier();
                copier.copyAttributes(horse, destHorse, destination);
            }
            super.copyAttributes(horse, destHorse, destination);
            destHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
            destHorse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).getBaseValue());
            destHorse.setJumpStrength(horse.getJumpStrength());
            destHorse.setMaximumAir(horse.getMaximumAir());
            destHorse.setDomestication(horse.getDomestication());
            destHorse.setMaxDomestication(horse.getMaxDomestication());
            destHorse.setOwner(horse.getOwner());
            destHorse.setTamed(horse.isTamed());
            destHorse.setEatingHaystack(horse.isEatingHaystack());
        }
    }

    private class ChestedHorseAttributeCopier implements AttributeCopier {
        /* This sets whether the horse is carrying a chest, but we still assume the actual
         * copying of inventory will happen in InventoryHolderAttributeCopier
         */
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering ChestedHorseAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof ChestedHorse) || !(entity instanceof ChestedHorse)) {
                throw new RuntimeException("destEntity and entity must both be ChestedHorses");
            }
            ChestedHorse destHorse = (ChestedHorse) destEntity;
            ChestedHorse horse = (ChestedHorse) entity;

            destHorse.setCarryingChest(horse.isCarryingChest());
        }
    }

    private class LlamaAttributeCopier extends AbstractHorseAttributeCopier {
        /* This sets whether the horse is carrying a chest, but we still assume the actual
         * copying of inventory will happen in InventoryHolderAttributeCopier
         */
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering LlamaAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Llama) || !(entity instanceof Llama)) {
                throw new RuntimeException("destEntity and entity must both be Llamas");
            }
            Llama destHorse = (Llama) destEntity;
            Llama horse = (Llama) entity;
            super.copyAttributes(horse, destHorse, destination);
            destHorse.setColor(horse.getColor());
            destHorse.setStrength(horse.getStrength());
        }
    }

    private class HorseAttributeCopier extends AbstractHorseAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering HorseAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Horse) || !(entity instanceof Horse)) {
                throw new RuntimeException("destEntity and entity must both be Horses");
            }
            Horse destHorse = (Horse) destEntity;
            Horse horse = (Horse) entity;

            super.copyAttributes(horse, destHorse, destination);
            destHorse.setColor(horse.getColor());
            destHorse.setStyle(horse.getStyle());
            destHorse.getInventory().setArmor(horse.getInventory().getArmor());
            destHorse.getInventory().setSaddle(horse.getInventory().getSaddle());
        }
    }

    private class CamelAttributeCopier extends AbstractHorseAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering CamelAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Camel) || !(entity instanceof Camel)) {
                throw new RuntimeException("destEntity and entity must both be Camels");
            }
            Camel destCamel = (Camel) destEntity;
            Camel camel = (Camel) entity;

            super.copyAttributes(camel, destCamel, destination);
            destCamel.setDashing(camel.isDashing());
        }
    }

    private class PigAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering PigAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Pig) || !(entity instanceof Pig)) {
                throw new RuntimeException("destEntity and entity must both be Pigs");
            }
            Pig destPig = (Pig) destEntity;
            Pig pig = (Pig) entity;

            super.copyAttributes(pig, destPig, destination);
            destPig.setSaddle(pig.hasSaddle());
            destPig.setBoostTicks(pig.getBoostTicks());
            destPig.setCurrentBoostTicks(pig.getCurrentBoostTicks());
        }
    }

    private class SheepAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering SheepAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Sheep) || !(entity instanceof Sheep)) {
                throw new RuntimeException("destEntity and entity must both be Sheep");
            }
            Sheep destSheep = (Sheep) destEntity;
            Sheep sheep = (Sheep) entity;

            super.copyAttributes(sheep, destSheep, destination);
            destSheep.setColor(sheep.getColor());
            destSheep.setSheared(sheep.isSheared());
        }
    }

    private class VillagerAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering VillagerAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Villager) || !(entity instanceof Villager)) {
                throw new RuntimeException("destEntity and entity must both be Villagers");
            }
            Villager destVillager = (Villager) destEntity;
            Villager villager = (Villager) entity;

            destVillager.setProfession(villager.getProfession());
            destVillager.getInventory().setContents(villager.getInventory().getContents());
            destVillager.setRecipes(villager.getRecipes());
            destVillager.setVillagerExperience(villager.getVillagerExperience());
            destVillager.setVillagerLevel(villager.getVillagerLevel());
            destVillager.setVillagerType(villager.getVillagerType());
        }
    }

    private class MinecartAttributeCopier extends EntityAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering MinecartAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Minecart) || !(entity instanceof Minecart)) {
                throw new RuntimeException("destEntity and entity must both be Minecarts");
            }
            Minecart destMinecart = (Minecart) destEntity;
            Minecart minecart = (Minecart) entity;

            super.copyAttributes(minecart, destMinecart, destination);
            destMinecart.setDamage(minecart.getDamage());
            destMinecart.setDerailedVelocityMod(minecart.getDerailedVelocityMod());
            destMinecart.setDisplayBlock(minecart.getDisplayBlock());
            destMinecart.setDisplayBlockData(minecart.getDisplayBlockData());
            destMinecart.setDisplayBlockOffset(minecart.getDisplayBlockOffset());
            destMinecart.setFlyingVelocityMod(minecart.getFlyingVelocityMod());
            destMinecart.setMaxSpeed(minecart.getMaxSpeed());
            destMinecart.setSlowWhenEmpty(minecart.isSlowWhenEmpty());
            AttributeCopier copier = null;
            if ((minecart instanceof CommandMinecart) && (destEntity instanceof CommandMinecart)) {
                copier = new CommandMinecartAttributeCopier();
            } else if ((minecart instanceof ExplosiveMinecart) && (destEntity instanceof ExplosiveMinecart)) {
                copier = new ExplosiveMinecartAttributeCopier();
            } else if ((minecart instanceof HopperMinecart) && (destEntity instanceof HopperMinecart)) {
                copier = new HopperMinecartAttributeCopier();
            } else if ((minecart instanceof PoweredMinecart) && (destEntity instanceof PoweredMinecart)) {
                copier = new PoweredMinecartAttributeCopier();
            }
            if (copier != null) {
                copier.copyAttributes(entity, destEntity, destination);
            }
        }
    }

    private class CommandMinecartAttributeCopier implements AttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering CommandMinecartAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof CommandMinecart) || !(entity instanceof CommandMinecart)) {
                throw new RuntimeException("destEntity and entity must both be Minecarts");
            }
            CommandMinecart destMinecart = (CommandMinecart) destEntity;
            CommandMinecart minecart = (CommandMinecart) entity;

            destMinecart.setCommand(minecart.getCommand());
            destMinecart.setName(minecart.getName());
        }
    }

    private class ExplosiveMinecartAttributeCopier implements AttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering ExplosiveMinecartAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof ExplosiveMinecart) || !(entity instanceof ExplosiveMinecart)) {
                throw new RuntimeException("destEntity and entity must both be Minecarts");
            }
            ExplosiveMinecart destMinecart = (ExplosiveMinecart) destEntity;
            ExplosiveMinecart minecart = (ExplosiveMinecart) entity;

            destMinecart.setFuseTicks(minecart.getFuseTicks());
        }
    }

    private class HopperMinecartAttributeCopier implements AttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering HopperMinecartAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof HopperMinecart) || !(entity instanceof HopperMinecart)) {
                throw new RuntimeException("destEntity and entity must both be Minecarts");
            }
            HopperMinecart destMinecart = (HopperMinecart) destEntity;
            HopperMinecart minecart = (HopperMinecart) entity;

            destMinecart.setEnabled(minecart.isEnabled());
        }
    }

    private class PoweredMinecartAttributeCopier implements AttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering PoweredMinecartAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof PoweredMinecart) || !(entity instanceof PoweredMinecart)) {
                throw new RuntimeException("destEntity and entity must both be Minecarts");
            }
            PoweredMinecart destMinecart = (PoweredMinecart) destEntity;
            PoweredMinecart minecart = (PoweredMinecart) entity;

            destMinecart.setFuel(minecart.getFuel());
        }
    }

    private class BoatAttributeCopier extends EntityAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering BoatAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Boat) || !(entity instanceof Boat)) {
                throw new RuntimeException("destEntity and entity must both be Boats");
            }
            Boat destBoat = (Boat) destEntity;
            Boat boat = (Boat) entity;

            super.copyAttributes(boat, destBoat, destination);
            destBoat.setBoatType(boat.getBoatType());
        }
    }
}
