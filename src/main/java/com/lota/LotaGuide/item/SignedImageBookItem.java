package com.lota.LotaGuide.item;

import com.lota.LotaGuide.data.ImageBookData;
import com.lota.LotaGuide.screen.ImageBookViewScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class SignedImageBookItem extends Item {
    
    public SignedImageBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide()) {
            openViewScreen(player, stack, hand);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    
    @OnlyIn(Dist.CLIENT)
    private void openViewScreen(Player player, ItemStack stack, InteractionHand hand) {
        ImageBookData data = ImageBookData.loadFromStack(stack);
        Minecraft.getInstance().setScreen(new ImageBookViewScreen(player, stack, hand, data));
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ImageBookData data = ImageBookData.loadFromStack(stack);
        int pageCount = data.getPageCount();
        tooltip.add(Component.translatable("item.lotaguide.signed_image_book.pages", pageCount).withStyle(ChatFormatting.GRAY));
    }
}
