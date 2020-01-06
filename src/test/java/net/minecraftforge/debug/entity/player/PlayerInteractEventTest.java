/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.debug.entity.player;

import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.DropperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("playerinteractevent_test")
public class PlayerInteractEventTest
{
    // NOTE: Test with both this ON and OFF - ensure none of the test behaviours show when this is off!
    private static final boolean ENABLE = true;
    private static Logger logger = LogManager.getLogger();

    public PlayerInteractEventTest()
    {
        if(ENABLE)
        {
            MinecraftForge.EVENT_BUS.register(this);
            MinecraftForge.EVENT_BUS.register(PlayerInteractEventTest.class); // Test Static event listeners
        }
    }

    @SubscribeEvent(receiveCanceled = true) // this triggers after the subclasses below, and we'd like to log them all
    public void global(PlayerInteractEvent evt)
    {
        logger.info("{} | {}", evt.getClass().getSimpleName(), evt.getSide().name());
        logger.info("{} | stack: {}", evt.getHand(), evt.getItemStack());
        logger.info("{} | face: {}", evt.getPos(), evt.getFace());
    }

    @SubscribeEvent
    public void leftClickBlock(PlayerInteractEvent.LeftClickBlock evt)
    {
        if (!evt.getItemStack().isEmpty())
        {
            if (evt.getItemStack().getItem() == Items.GOLDEN_PICKAXE)
            {
                evt.setCanceled(true); // Redstone ore should not activate and pick should not be able to dig anything
            }
            if (evt.getItemStack().getItem() == Items.DIAMOND_PICKAXE)
            {
                evt.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY); // Redstone ore should not activate, pick should still dig
            }
            if (evt.getItemStack().getItem() == Items.IRON_PICKAXE)
            {
                evt.setUseItem(Event.Result.DENY); // Pick should not dig, Redstone ore should still activate
            }
        }

        // When item use denied, the event will keep firing as long as the left click button is held.
        // This is due to how vanilla calls the left click handling methods to let people not lift their button when mining multiple blocks.
        // Note that when item use is denied, the cool down for the item does not occur. This is good!
    }

    @SubscribeEvent
    public void rightClickBlock(PlayerInteractEvent.RightClickBlock evt)
    {
        // Shift right clicking dropper with an item in hand should still open the dropper contrary to normal mechanics
        // The item in hand is used as well (not specifying anything would not use the item)
        TileEntity te = evt.getWorld().getTileEntity(evt.getPos());
        if (te instanceof DropperTileEntity)
        {
            evt.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            evt.setUseItem(Event.Result.ALLOW);
        }

        // Same as above, except the item should no longer be used
        if (te instanceof ChestTileEntity)
        {
            evt.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            evt.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY); // could be left out as well
        }

        // Case: Flint and steel in main hand on top of a TE will light a fire, not open the TE.
        // Note that if you do this on a chest, the f+s will fail, but then your off hand will open the chest
        // If you dual wield flints and steels and right click a chest nothing should happen
        if (!evt.getItemStack().isEmpty() && evt.getItemStack().getItem() == Items.FLINT_AND_STEEL)
        {
            evt.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }

        // Case: Painting in main hand
        // Opening a TE will also place a painting on the TE if possible
        if (evt.getHand() == Hand.MAIN_HAND && !evt.getItemStack().isEmpty() && evt.getItemStack().getItem() == Items.PAINTING)
        {
            evt.setUseItem(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }

        // Spawn egg in main hand, block in offhand -> block should be placed
        if (!evt.getItemStack().isEmpty()
                && evt.getItemStack().getItem() instanceof SpawnEggItem
                && evt.getHand() == Hand.MAIN_HAND
                && !evt.getPlayer().getHeldItemOffhand().isEmpty()
                && evt.getPlayer().getHeldItemOffhand().getItem() instanceof BlockItem)
        {
            evt.setCanceled(true);
        }

        // Spawn egg in main hand, potion in offhand -> potion should NOT be thrown
        if (!evt.getItemStack().isEmpty()
                && evt.getItemStack().getItem() instanceof SpawnEggItem
                && evt.getHand() == Hand.MAIN_HAND
                && !evt.getPlayer().getHeldItemOffhand().isEmpty()
                && evt.getPlayer().getHeldItemOffhand().getItem() == Items.SPLASH_POTION)
        {
            evt.setCanceled(true);
            // Fake spawn egg success so splash potion does not trigger
            evt.setCancellationResult(ActionResultType.SUCCESS);
        }
    }

    @SubscribeEvent
    public void rightClickItem(PlayerInteractEvent.RightClickItem evt)
    {
        // Case: Ender pearl in main hand, block in offhand -> Block is NOT placed
        if (!evt.getItemStack().isEmpty()
                && evt.getItemStack().getItem() == Items.ENDER_PEARL
                && evt.getHand() == Hand.MAIN_HAND
                && !evt.getPlayer().getHeldItemOffhand().isEmpty()
                && evt.getPlayer().getHeldItemOffhand().getItem() instanceof BlockItem)
        {
            evt.setCanceled(true);
            evt.setCancellationResult(ActionResultType.SUCCESS); // We fake success on the ender pearl so block is not placed
            return;
        }

        // Case: Ender pearl in main hand, bow in offhand with arrows in inv -> Bow should trigger
        // Case: Sword in main hand, ender pearl in offhand -> Nothing should happen
        if (!evt.getItemStack().isEmpty() && evt.getItemStack().getItem() == Items.ENDER_PEARL)
        {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void interactSpecific(PlayerInteractEvent.EntityInteractSpecific evt)
    {
        logger.info("LOCAL POS: {}", evt.getLocalPos());

        if (!evt.getItemStack().isEmpty()
                && evt.getTarget() instanceof ArmorStandEntity
                && evt.getItemStack().getItem() == Items.IRON_HELMET)
        {
            evt.setCanceled(true); // Should not be able to place iron helmet onto armor stand (you will put it on instead)
        }

        if (!evt.getItemStack().isEmpty()
                && evt.getTarget() instanceof ArmorStandEntity
                && evt.getItemStack().getItem() == Items.GOLDEN_HELMET)
        {
            evt.setCanceled(true);
            evt.setCancellationResult(ActionResultType.SUCCESS);
            // Should not be able to place golden helmet onto armor stand
            // However you will NOT put it on because we fake success on the armorstand.
        }

        if (!evt.getWorld().isRemote
                && evt.getTarget() instanceof SkeletonEntity
                && evt.getLocalPos().y > evt.getTarget().getHeight() / 2.0)
        {
            // If we right click the upper half of a skeleton it dies.
            evt.getTarget().remove();
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void interactNormal(PlayerInteractEvent.EntityInteract evt)
    {
        if (!evt.getItemStack().isEmpty() && evt.getTarget() instanceof HorseEntity)
        {
            // Should not be able to feed wild horses with golden apple (you will start eating it in survival)
            if (evt.getItemStack().getItem() == Items.GOLDEN_APPLE
                    && evt.getItemStack().getDamage() == 0)
            {
                evt.setCanceled(true);
            }
            // Should not be able to feed wild horses with notch apple but you will NOT eat it
            if (evt.getItemStack().getItem() == Items.GOLDEN_APPLE
                    && evt.getItemStack().getDamage() == 1)
            {
                evt.setCanceled(true);
                evt.setCancellationResult(ActionResultType.SUCCESS);
            }
        }
    }
}
