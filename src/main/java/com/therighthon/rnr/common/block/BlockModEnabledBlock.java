package com.therighthon.rnr.common.block;

import com.therighthon.rnr.RNRHelpers;
import javax.swing.text.html.BlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// NOTE: This class is now a misnomer, block mod enable blocks no longer exist, all blocks just work
public class BlockModEnabledBlock extends Block
{
    private final boolean isTransparent;

    public BlockModEnabledBlock(Properties properties, Boolean isTransparent)
    {
        super(properties);
        this.isTransparent = isTransparent;
    }

    public BlockModEnabledBlock(Properties properties)
    {
        super(properties);
        this.isTransparent = false;
    }

    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos)
    {
        return this.isTransparent;
    }
}
