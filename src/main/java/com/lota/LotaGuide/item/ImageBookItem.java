package com.lota.LotaGuide.item;

import com.lota.LotaGuide.data.ImageBookData;
import com.lota.LotaGuide.screen.ImageBookEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ImageBookItem extends Item {
    
    public ImageBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            openEditScreen(player, stack, hand);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    
    @OnlyIn(Dist.CLIENT)
    private void openEditScreen(Player player, ItemStack stack, InteractionHand hand) {
        ImageBookData data = ImageBookData.loadFromStack(stack);
        Minecraft.getInstance().setScreen(new ImageBookEditScreen(player, stack, hand, data));
    }
}
