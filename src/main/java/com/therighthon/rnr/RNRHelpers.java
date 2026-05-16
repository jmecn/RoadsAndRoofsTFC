package com.therighthon.rnr;

import com.mojang.datafixers.util.Either;
import com.therighthon.rnr.common.RNRTags;
import com.therighthon.rnr.common.block.CrackingWetConcretePathBlock;
import com.therighthon.rnr.common.block.WetConcretePathControlJointBlock;
import com.therighthon.rnr.common.fluid.RNRFluids;
import com.therighthon.rnr.common.fluid.SimpleRNRFluid;
import com.therighthon.rnr.common.item.RNRItems;
import com.therighthon.rnr.common.recipe.BlockModRecipe;
import com.therighthon.rnr.common.recipe.MattockRecipe;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.player.PlayerDataCapability;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;

import static com.therighthon.rnr.common.block.CrackingWetConcretePathBlock.*;

public final class RNRHelpers
{
    public static InteractionResult blockModRecipeCompatible(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        ItemStack stack = player.getItemInHand(hand);
        final BlockModRecipe recipe = BlockModRecipe.getRecipe(level.getBlockState(pos), stack);
        if (recipe != null && !(player.blockPosition().equals(pos)))
        {
            final BlockState output = recipe.getOutputBlock().getBlock().withPropertiesOf(blockState);
            if (!player.isCreative() && recipe.consumesItem())
            {

                // Concrete pouring
                final IFluidHandlerItem fluidHandler = stack.getCapability(Capabilities.FLUID_ITEM).resolve().orElse(null);
                if (fluidHandler != null) {
                    fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    player.setItemInHand(hand, fluidHandler.getContainer());
                }
                else if (stack.isDamageableItem())
                {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                }
                else
                {
                    stack.shrink(1);
                }
            }
            level.playLocalSound(pos, output.getSoundType().getHitSound(), SoundSource.BLOCKS, 1f, 1f, false);
            level.setBlock(pos, output, 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, blockState));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public static InteractionResult useMattockOn(Player player, Level level, BlockPos pos, BlockHitResult blockHitResult)
    {
        if (player != null)
        {
            final BlockState state = level.getBlockState(pos);
            final Either<BlockState, InteractionResult> result = MattockRecipe.computeResult(player, state, blockHitResult, true);
            return result.map(resultState -> {
                player.playSound(resultState.getSoundType().getHitSound(), 1f, 1f);

                ItemStack held = player.getMainHandItem();
                if (!level.isClientSide)
                {
                    if (TFCConfig.SERVER.enableChiselsStartCollapses.get())
                    {
                        if (Helpers.isBlock(state, TFCTags.Blocks.CAN_TRIGGER_COLLAPSE) && CollapseRecipe.tryTriggerCollapse(level, pos))
                        {
                            return InteractionResult.SUCCESS; // Abort chiseling
                        }
                    }

                    player.getCapability(PlayerDataCapability.CAPABILITY).ifPresent(cap -> {
                        final MattockRecipe recipeUsed = MattockRecipe.getRecipe(state, held, cap.getChiselMode());
                        if (recipeUsed != null)
                        {
                            ItemStack extraDrop = recipeUsed.getExtraDrop(held);
                            if (!extraDrop.isEmpty())
                            {
                                ItemHandlerHelper.giveItemToPlayer(player, extraDrop);
                            }
                        }
                    });
                }
                //Silly hard code to make joints connect properly
                if (resultState.getBlock() instanceof WetConcretePathControlJointBlock)
                {
                    resultState = WetConcretePathControlJointBlock.updateControlJointShape(resultState, Direction.NORTH, level.getBlockState(pos.north()));
                    resultState = WetConcretePathControlJointBlock.updateControlJointShape(resultState, Direction.EAST, level.getBlockState(pos.east()));
                    resultState = WetConcretePathControlJointBlock.updateControlJointShape(resultState, Direction.SOUTH, level.getBlockState(pos.south()));
                    resultState = WetConcretePathControlJointBlock.updateControlJointShape(resultState, Direction.WEST, level.getBlockState(pos.west()));
                }
                level.setBlockAndUpdate(pos, resultState);


                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                player.getCooldowns().addCooldown(held.getItem(), 5);
                return InteractionResult.SUCCESS;
            }, Function.identity()); // returns the interaction result if we are given one
        }
        return InteractionResult.PASS;
    }

    public static void updateWetCrackingConcrete(Level level, BlockPos pos)
    {
        for (int i : new int[] {1, -1, 2, -2})
        {
            final int dist = Math.abs(i);

            final BlockPos xPos = pos.offset(i, 0, 0);
            final BlockState xState = level.getBlockState(xPos);
            if (xState.getBlock() instanceof CrackingWetConcretePathBlock)
            {
                // Block mod recipes should not reset concrete cure times
                if (level.getBlockEntity(xPos) instanceof TickCounterBlockEntity counter)
                {
                    final long lastUpdateTick = counter.getLastUpdateTick();
                    level.setBlockAndUpdate(xPos, xState.setValue(DISTANCE_X, Math.min(xState.getValue(DISTANCE_X), dist)));
                    if (level.getBlockEntity(xPos) instanceof TickCounterBlockEntity newCounter)
                        newCounter.setLastUpdateTick(lastUpdateTick);
                }
                else
                {
                    level.setBlockAndUpdate(xPos, xState.setValue(DISTANCE_X, Math.min(xState.getValue(DISTANCE_X), dist)));
                }
            }

            final BlockPos zPos = pos.offset(0, 0, i);
            final BlockState zState = level.getBlockState(zPos);
            if (zState.getBlock() instanceof CrackingWetConcretePathBlock)
            {

                // Block mod recipes should not reset concrete cure times
                if (level.getBlockEntity(zPos) instanceof TickCounterBlockEntity counter)
                {
                    final long lastUpdateTick = counter.getLastUpdateTick();
                    level.setBlockAndUpdate(zPos, zState.setValue(DISTANCE_Z, Math.min(zState.getValue(DISTANCE_Z), dist)));
                    if (level.getBlockEntity(zPos) instanceof TickCounterBlockEntity newCounter)
                        newCounter.setLastUpdateTick(lastUpdateTick);
                }
                else
                {
                    level.setBlockAndUpdate(zPos, zState.setValue(DISTANCE_Z, Math.min(zState.getValue(DISTANCE_Z), dist)));
                }
            }
        }
    }
}
