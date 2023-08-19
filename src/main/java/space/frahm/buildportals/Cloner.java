package space.frahm.buildportals;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Cat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Sniffer;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;
import org.bukkit.potion.PotionEffect;
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
        } else if (entity instanceof AbstractVillager) {
            copier = new AbstractVillagerAttributeCopier();
        } else if (entity instanceof Horse) {
            copier = new HorseAttributeCopier();
        } else if (entity instanceof Llama) {
            copier = new LlamaAttributeCopier();
        } else if (entity instanceof AbstractHorse) {
            copier = new AbstractHorseAttributeCopier();
        } else if (entity instanceof Sheep) {
            copier = new SheepAttributeCopier();
        } else if (entity instanceof Pig) {
            copier = new PigAttributeCopier();
        } else if (entity instanceof Camel) {
            copier = new CamelAttributeCopier();
        } else if (entity instanceof Frog) {
            copier = new FrogAttributeCopier();
        } else if (entity instanceof Sniffer) {
            copier = new SnifferAttributeCopier();
        } else if (entity instanceof Cat) {
            copier = new CatAttributeCopier();
        } else if (entity instanceof Parrot) {
            copier = new ParrotAttributeCopier();
        } else if (entity instanceof Wolf) {
            copier = new WolfAttributeCopier();
        } else if (entity instanceof Animals) {
            copier = new AnimalAttributeCopier();
        } else if (entity instanceof Allay) {
            copier = new AllayAttributeCopier();
        } else if (entity instanceof Shulker) {
            copier = new ShulkerAttributeCopier();
        } else if (entity instanceof Creeper) {
            copier = new CreeperAttributeCopier();
        } else if (entity instanceof ZombieVillager) {
            copier = new ZombieVillagerAttributeCopier();
        } else if (entity instanceof PigZombie) {
            copier = new PigZombieAttributeCopier();
        } else if (entity instanceof Zombie) {
            copier = new ZombieAttributeCopier();
        } else if (entity instanceof IronGolem) {
            copier = new IronGolemAttributeCopier();
        } else if (entity instanceof Snowman) {
            copier = new SnowmanAttributeCopier();
        } else if (entity instanceof Raider) {
            copier = new RaiderAttributeCopier();
        } else if (entity instanceof Mob) {
            copier = new MobAttributeCopier();
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

            // Calculate speed going into the portal
            Vector speedVec = vehicle.getVelocity();
            double speed = Math.sqrt(speedVec.getX()*speedVec.getX() + speedVec.getY()*speedVec.getY() + speedVec.getZ()*speedVec.getZ());
            // Set minimum exit velocity
            if (speed < 0.1) {
                speed = 0.1;
            }
            // Apply speed to new unit vector for portal exit
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
            ItemStack[] items = ((InventoryHolder)inventoryHolder).getInventory().getContents().clone();
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

    private class MobAttributeCopier extends EntityAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering MobAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Mob) || !(entity instanceof Mob)) {
                throw new RuntimeException("destEntity and entity must both be Mobs");
            }
            Mob destMob = (Mob) destEntity;
            Mob mob = (Mob) entity;

            super.copyAttributes(mob, destMob, destination);
            // LivingEntity
            destMob.addPotionEffects(mob.getActivePotionEffects());
            destMob.setAI(mob.hasAI());
            destMob.setArrowCooldown(mob.getArrowCooldown());
            destMob.setArrowsInBody(mob.getArrowsInBody());
            destMob.setCanPickupItems(mob.getCanPickupItems());
            destMob.setCollidable(mob.isCollidable());
            destMob.setGliding(mob.isGliding());
            destMob.setInvisible(mob.isInvisible());
            destMob.setLastDamage(mob.getLastDamage());
            destMob.setMaximumAir(mob.getMaximumAir());
            destMob.setMaximumNoDamageTicks(mob.getMaximumNoDamageTicks());
            destMob.setNoActionTicks(mob.getNoActionTicks());
            destMob.setNoDamageTicks(mob.getNoDamageTicks());
            destMob.setRemainingAir(mob.getRemainingAir());
            destMob.setRemoveWhenFarAway(mob.getRemoveWhenFarAway());
            destMob.setSwimming(mob.isSwimming());
            destMob.getEquipment().setArmorContents(mob.getEquipment().getArmorContents());
            destMob.getEquipment().setItemInMainHand(mob.getEquipment().getItemInMainHand());
            destMob.getEquipment().setItemInOffHand(mob.getEquipment().getItemInOffHand());

            // Damageable
            destMob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            destMob.setHealth(mob.getHealth());
            destMob.setAbsorptionAmount(mob.getAbsorptionAmount());

            // Mob
            destMob.setAware(mob.isAware());
            destMob.setTarget(mob.getTarget());

            if ((mob instanceof Lootable) && (destMob instanceof Lootable)) {
                destMob.setSeed(mob.getSeed());
                destMob.setLootTable(mob.getLootTable());
            }
        }
    }

    private class RaiderAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering RaiderAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Raider) || !(entity instanceof Raider)) {
                throw new RuntimeException("destEntity and entity must both be Raiders");
            }
            Raider destRaider = (Raider) destEntity;
            Raider raider = (Raider) entity;

            super.copyAttributes(raider, destRaider, destination);
            destRaider.setCanJoinRaid(raider.isCanJoinRaid());
            destRaider.setCelebrating(raider.isCelebrating());
            destRaider.setPatrolLeader(raider.isPatrolLeader());
            destRaider.setPatrolTarget(raider.getPatrolTarget());
            destRaider.setRaid(raider.getRaid());
            destRaider.setTicksOutsideRaid(raider.getTicksOutsideRaid());
            destRaider.setWave(raider.getWave());
        }
    }

    private class AllayAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering AllayAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Allay) || !(entity instanceof Allay)) {
                throw new RuntimeException("destEntity and entity must both be Allays");
            }
            Allay destAllay = (Allay) destEntity;
            Allay allay = (Allay) entity;

            super.copyAttributes(allay, destAllay, destination);
            destAllay.setCanDuplicate(allay.canDuplicate());
            destAllay.setDuplicationCooldown(allay.getDuplicationCooldown());

        }
    }

    private class ShulkerAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering ShulkerAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Shulker) || !(entity instanceof Shulker)) {
                throw new RuntimeException("destEntity and entity must both be Shulkers");
            }
            Shulker destShulker = (Shulker) destEntity;
            Shulker shulker = (Shulker) entity;

            super.copyAttributes(shulker, destShulker, destination);
            destShulker.setAttachedFace(shulker.getAttachedFace());
            destShulker.setPeek(shulker.getPeek());
            destShulker.setColor(shulker.getColor());
        }
    }

    private class CreeperAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering CreeperAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Creeper) || !(entity instanceof Creeper)) {
                throw new RuntimeException("destEntity and entity must both be Creepers");
            }
            Creeper destCreeper = (Creeper) destEntity;
            Creeper creeper = (Creeper) entity;

            super.copyAttributes(creeper, destCreeper, destination);
            destCreeper.setExplosionRadius(creeper.getExplosionRadius());
            destCreeper.setFuseTicks(creeper.getFuseTicks());
            destCreeper.setMaxFuseTicks(creeper.getMaxFuseTicks());
            destCreeper.setPowered(creeper.isPowered());
        }
    }

    private class IronGolemAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering IronGolemAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof IronGolem) || !(entity instanceof IronGolem)) {
                throw new RuntimeException("destEntity and entity must both be IronGolems");
            }
            IronGolem destIronGolem = (IronGolem) destEntity;
            IronGolem golem = (IronGolem) entity;

            super.copyAttributes(golem, destIronGolem, destination);
            destIronGolem.setPlayerCreated(golem.isPlayerCreated());
        }
    }

    private class SnowmanAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering SnowmanAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Snowman) || !(entity instanceof Snowman)) {
                throw new RuntimeException("destEntity and entity must both be Snowmans");
            }
            Snowman destSnowman = (Snowman) destEntity;
            Snowman golem = (Snowman) entity;

            super.copyAttributes(golem, destSnowman, destination);
            destSnowman.setDerp(golem.isDerp());
        }
    }

    private class ZombieAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering ZombieAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Zombie) || !(entity instanceof Zombie)) {
                throw new RuntimeException("destEntity and entity must both be Zombies");
            }
            Zombie destZombie = (Zombie) destEntity;
            Zombie zombie = (Zombie) entity;

            super.copyAttributes(zombie, destZombie, destination);
            destZombie.setCanBreakDoors(zombie.canBreakDoors());
            if (zombie.isConverting()) {
                destZombie.setConversionTime(zombie.getConversionTime());
            }
        }
    }

    private class ZombieVillagerAttributeCopier extends ZombieAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering ZombieVillagerAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof ZombieVillager) || !(entity instanceof ZombieVillager)) {
                throw new RuntimeException("destEntity and entity must both be ZombieVillagers");
            }
            ZombieVillager destZombie = (ZombieVillager) destEntity;
            ZombieVillager zombie = (ZombieVillager) entity;

            super.copyAttributes(zombie, destZombie, destination);
            destZombie.setConversionPlayer(zombie.getConversionPlayer());
            destZombie.setVillagerProfession(zombie.getVillagerProfession());
            destZombie.setVillagerType(zombie.getVillagerType());
        }
    }

    private class PigZombieAttributeCopier extends ZombieAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering PigZombieAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof PigZombie) || !(entity instanceof PigZombie)) {
                throw new RuntimeException("destEntity and entity must both be PigZombies");
            }
            PigZombie destZombie = (PigZombie) destEntity;
            PigZombie zombie = (PigZombie) entity;

            super.copyAttributes(zombie, destZombie, destination);
            destZombie.setAnger(zombie.getAnger());
        }
    }

    private class BreedableAttributeCopier extends MobAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering BreedableAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Breedable) || !(entity instanceof Breedable)) {
                throw new RuntimeException("destEntity and entity must both be Breedable");
            }
            Breedable destBreedable = (Breedable) destEntity;
            Breedable breedable = (Breedable) entity;

            super.copyAttributes(breedable, destBreedable, destination);
            // Ageable
            destBreedable.setAge(breedable.getAge());

            // Breedable
            destBreedable.setBreed(breedable.canBreed());
            destBreedable.setAgeLock(breedable.getAgeLock());
        }
    }

    private class AnimalAttributeCopier extends BreedableAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering AnimalAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Animals) || !(entity instanceof Animals)) {
                throw new RuntimeException("destEntity and entity must both be Animals");
            }
            Animals destAnimal = (Animals) destEntity;
            Animals animal = (Animals) entity;

            super.copyAttributes(animal, destAnimal, destination);
            destAnimal.setBreedCause(animal.getBreedCause());
            destAnimal.setLoveModeTicks(animal.getLoveModeTicks());
        }
    }

    private class FrogAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering FrogAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Frog) || !(entity instanceof Frog)) {
                throw new RuntimeException("destEntity and entity must both be Frogs");
            }
            Frog destFrog = (Frog) destEntity;
            Frog frog = (Frog) entity;

            super.copyAttributes(frog, destFrog, destination);
            destFrog.setTongueTarget(frog.getTongueTarget());
            destFrog.setVariant(frog.getVariant());
        }
    }

    private class SnifferAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering SnifferAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Sniffer) || !(entity instanceof Sniffer)) {
                throw new RuntimeException("destEntity and entity must both be Sniffers");
            }
            Sniffer destSniffer = (Sniffer) destEntity;
            Sniffer sniffer = (Sniffer) entity;

            super.copyAttributes(sniffer, destSniffer, destination);
            destSniffer.setState(sniffer.getState());
            for (Location loc: sniffer.getExploredLocations()) {
                destSniffer.addExploredLocation(loc);
            }
        }
    }

    private class TameSitAttributeCopier extends AnimalAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering TameSitAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Tameable) || !(entity instanceof Tameable)) {
                throw new RuntimeException("destEntity and entity must both be Tameable");
            }
            if (!(destEntity instanceof Sittable) || !(entity instanceof Sittable)) {
                throw new RuntimeException("destEntity and entity must both be Sittable");
            }
            Tameable destTameable = (Tameable) destEntity;
            Tameable tameable = (Tameable) entity;

            super.copyAttributes(tameable, destTameable, destination);
            // Sitable
            ((Sittable)destTameable).setSitting(((Sittable)destTameable).isSitting());
            // Tameable
            destTameable.setOwner(tameable.getOwner());
            destTameable.setTamed(tameable.isTamed());
        }
    }

    private class CatAttributeCopier extends TameSitAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering CatAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Cat) || !(entity instanceof Cat)) {
                throw new RuntimeException("destEntity and entity must both be Cats");
            }
            Cat destCat = (Cat) destEntity;
            Cat cat = (Cat) entity;

            super.copyAttributes(cat, destCat, destination);
            destCat.setCatType(cat.getCatType());
            destCat.setCollarColor(cat.getCollarColor());
        }
    }

    private class ParrotAttributeCopier extends TameSitAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering ParrotAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Parrot) || !(entity instanceof Parrot)) {
                throw new RuntimeException("destEntity and entity must both be Parrots");
            }
            Parrot destParrot = (Parrot) destEntity;
            Parrot parrot = (Parrot) entity;

            super.copyAttributes(parrot, destParrot, destination);
            destParrot.setVariant(parrot.getVariant());
        }
    }

    private class WolfAttributeCopier extends TameSitAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering WolfAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Wolf) || !(entity instanceof Wolf)) {
                throw new RuntimeException("destEntity and entity must both be Wolfs");
            }
            Wolf destWolf = (Wolf) destEntity;
            Wolf wolf = (Wolf) entity;

            super.copyAttributes(wolf, destWolf, destination);
            destWolf.setAngry(wolf.isAngry());
            destWolf.setCollarColor(wolf.getCollarColor());
            destWolf.setInterested(wolf.isInterested());
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
            // destHorse.getInventory().setArmor(horse.getInventory().getArmor());
            // destHorse.getInventory().setSaddle(horse.getInventory().getSaddle());
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

    private class AbstractVillagerAttributeCopier extends BreedableAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering AbstractVillagerAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof AbstractVillager) || !(entity instanceof AbstractVillager)) {
                throw new RuntimeException("destEntity and entity must both be AbstractVillagers");
            }
            AbstractVillager destAbstractVillager = (AbstractVillager) destEntity;
            AbstractVillager abstractVillager = (AbstractVillager) entity;

            super.copyAttributes(abstractVillager, destAbstractVillager, destination);
            destAbstractVillager.setRecipes(abstractVillager.getRecipes());
        }
    }

    private class VillagerAttributeCopier extends AbstractVillagerAttributeCopier {
        @Override
        public void copyAttributes(Entity entity, Entity destEntity, Location destination) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Entering VillagerAttributeCopier.copyAttributes() method");
            if (!(destEntity instanceof Villager) || !(entity instanceof Villager)) {
                throw new RuntimeException("destEntity and entity must both be Villagers");
            }
            Villager destVillager = (Villager) destEntity;
            Villager villager = (Villager) entity;

            destVillager.setProfession(villager.getProfession());
            destVillager.setVillagerExperience(villager.getVillagerExperience());
            destVillager.setVillagerLevel(villager.getVillagerLevel());
            destVillager.setVillagerType(villager.getVillagerType());
            // Apply recipes after setting villager profession, etc
            super.copyAttributes(villager, destVillager, destination);
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
