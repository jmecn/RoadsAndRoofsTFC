package com.therighthon.rnr.common.item;

import java.util.function.Function;
import com.mojang.datafixers.util.Either;
import com.therighthon.rnr.RoadsAndRoofs;
import com.therighthon.rnr.common.RNRTags;
import com.therighthon.rnr.common.block.WetConcretePathControlJointBlock;
import com.therighthon.rnr.common.recipe.MattockRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.capabilities.player.PlayerDataCapability;
import net.dries007.tfc.common.items.ToolItem;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.advancements.TFCAdvancements;

//Mostly copied from TFC's ChiselItem.java and is under the TFC License
public class MattockItem extends ToolItem
{
    public MattockItem(Tier tier, float attackDamage, float attackSpeed, Properties properties)
    {
        super(tier, attackDamage, attackSpeed, RNRTags.Blocks.MINEABLE_WITH_MATTOCK, properties);
    }

}
