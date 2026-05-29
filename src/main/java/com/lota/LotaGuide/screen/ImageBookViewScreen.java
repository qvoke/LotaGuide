package com.lota.LotaGuide.screen;

import com.lota.LotaGuide.LotaGuide;
import com.lota.LotaGuide.client.ImageCache;
import com.lota.LotaGuide.data.ImageBookData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for viewing a signed Image Book.
 * Layout matches edit mode: (URL space) -> Image -> Text -> Page controls
 */
@OnlyIn(Dist.CLIENT)
public class ImageBookViewScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int IMAGE_AREA_HEIGHT = 210;
    private static final int IMAGE_AREA_WIDTH = 280;
    private static final int PADDING = 10;
    private static final int TEXT_AREA_HEIGHT = 40;
    private static final int URL_FIELD_SPACE = 25;
    
    private final Player player;
    private final ItemStack bookStack;
    private final InteractionHand hand;
    private final ImageBookData bookData;
    private final boolean isAuthor;
    
    private int currentPage = 0;
    private int leftPos;
    private int topPos;
    
    private Button prevPageButton;
    private Button nextPageButton;
    private Button editButton;
    private Button doneButton;
    
    public ImageBookViewScreen(Player player, ItemStack bookStack, InteractionHand hand, ImageBookData bookData) {
        super(Component.empty());
        this.player = player;
        this.bookStack = bookStack;
        this.hand = hand;
        this.bookData = bookData;
        this.isAuthor = bookData.isAuthor(player.getUUID());
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = 10;
        
        // Calculate Y position for controls - same as edit mode
        int currentY = topPos + URL_FIELD_SPACE + IMAGE_AREA_HEIGHT + PADDING + TEXT_AREA_HEIGHT + PADDING;
        
        int pageCounterY = currentY;
        int pageCounterWidth = 50;
        int centerX = leftPos + PANEL_WIDTH / 2;
        
        currentY += 25;
        
        int navButtonWidth = 25;
        this.prevPageButton = Button.builder(Component.literal("<"), button -> previousPage())
            .bounds(centerX - navButtonWidth - 5, currentY, navButtonWidth, 20)
            .build();
        this.addRenderableWidget(this.prevPageButton);
        
        this.nextPageButton = Button.builder(Component.literal(">"), button -> nextPage())
            .bounds(centerX + 5, currentY, navButtonWidth, 20)
            .build();
        this.addRenderableWidget(this.nextPageButton);
        
        if (isAuthor) {
            this.editButton = Button.builder(Component.translatable("screen.lotaguide.image_book.edit"), 
                button -> openEditMode())
                .bounds(leftPos + PADDING, currentY, 60, 20)
                .build();
            this.addRenderableWidget(this.editButton);
        }
        
        this.doneButton = Button.builder(Component.translatable("gui.done"), 
            button -> onClose())
            .bounds(leftPos + PANEL_WIDTH - PADDING - 60, currentY, 60, 20)
            .build();
        this.addRenderableWidget(this.doneButton);
        
        updateNavigationButtons();
    }
    
    private void updateNavigationButtons() {
        this.prevPageButton.active = currentPage > 0;
        this.nextPageButton.active = currentPage < bookData.getPageCount() - 1;
    }
    
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateNavigationButtons();
        }
    }
    
    private void nextPage() {
        if (currentPage < bookData.getPageCount() - 1) {
            currentPage++;
            updateNavigationButtons();
        }
    }
    
    private void openEditMode() {
        ItemStack editableBook = new ItemStack(LotaGuide.IMAGE_BOOK.get());
        bookData.saveToStack(editableBook);
        player.setItemInHand(hand, editableBook);
        
        this.minecraft.setScreen(new ImageBookEditScreen(player, editableBook, hand, bookData));
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        
        int imageAreaX = leftPos + (PANEL_WIDTH - IMAGE_AREA_WIDTH) / 2;
        int imageAreaY = topPos + URL_FIELD_SPACE;
        
        String imageUrl = page.getImageUrl();
        boolean hasImage = false;
        
        if (!imageUrl.isEmpty()) {
            ImageCache.CachedImage cachedImage = ImageCache.getInstance().getImage(imageUrl);
            
            if (cachedImage != null && !cachedImage.isError()) {
                ResourceLocation texture = cachedImage.getTexture(System.currentTimeMillis());
                if (texture != null) {
                    hasImage = true;
                    RenderSystem.setShaderTexture(0, texture);
                    
                    int imgWidth = cachedImage.getWidth();
                    int imgHeight = cachedImage.getHeight();
                    
                    // If image is larger than area, scale down to fit
                    if (imgWidth > IMAGE_AREA_WIDTH || imgHeight > IMAGE_AREA_HEIGHT) {
                        float scale = Math.min(
                            (float) IMAGE_AREA_WIDTH / imgWidth,
                            (float) IMAGE_AREA_HEIGHT / imgHeight
                        );
                        imgWidth = (int) (imgWidth * scale);
                        imgHeight = (int) (imgHeight * scale);
                    }
                    
                    // Center the image
                    int renderX = imageAreaX + (IMAGE_AREA_WIDTH - imgWidth) / 2;
                    int renderY = imageAreaY + (IMAGE_AREA_HEIGHT - imgHeight) / 2;
                    
                    graphics.blit(texture, renderX, renderY, 0, 0, imgWidth, imgHeight, 
                        imgWidth, imgHeight);
                }
            }
        }
        
        if (!hasImage) {
            graphics.fill(imageAreaX, imageAreaY, 
                imageAreaX + IMAGE_AREA_WIDTH, imageAreaY + IMAGE_AREA_HEIGHT, 
                0xFF333333);
                
            if (!imageUrl.isEmpty()) {
                ImageCache.CachedImage cachedImage = ImageCache.getInstance().getImage(imageUrl);
                if (cachedImage != null && cachedImage.isError()) {
                    graphics.drawCenteredString(this.font, cachedImage.getErrorMessage(), 
                        leftPos + PANEL_WIDTH / 2, imageAreaY + IMAGE_AREA_HEIGHT / 2, 0xFF5555);
                } else if (ImageCache.getInstance().isLoading(imageUrl)) {
                    long dots = (System.currentTimeMillis() / 500) % 4;
                    String loading = "Loading" + ".".repeat((int) dots);
                    graphics.drawCenteredString(this.font, loading, 
                        leftPos + PANEL_WIDTH / 2, imageAreaY + IMAGE_AREA_HEIGHT / 2, 0xAAAAAA);
                }
            }
        }
        
        int textAreaY = imageAreaY + IMAGE_AREA_HEIGHT + PADDING;
        String text = page.getText();
        if (!text.isEmpty()) {
            List<String> lines = wrapText(text, IMAGE_AREA_WIDTH);
            int yOffset = 0;
            int maxLines = TEXT_AREA_HEIGHT / 12;
            for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
                graphics.drawString(this.font, lines.get(i), imageAreaX, textAreaY + yOffset, 0xFFFFFF);
                yOffset += 12;
            }
        }
        
        int pageCounterY = topPos + URL_FIELD_SPACE + IMAGE_AREA_HEIGHT + PADDING + TEXT_AREA_HEIGHT + PADDING;
        String pageIndicator = (currentPage + 1) + " / " + bookData.getPageCount();
        graphics.drawCenteredString(this.font, pageIndicator, 
            leftPos + PANEL_WIDTH / 2, pageCounterY + 5, 0xFFFFFF);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        
        for (String paragraph : text.split("\n")) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            
            StringBuilder currentLine = new StringBuilder();
            int currentWidth = 0;
            
            for (String word : paragraph.split(" ")) {
                int wordWidth = this.font.width(word + " ");
                if (currentWidth + wordWidth > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                    currentWidth = 0;
                }
                currentLine.append(word).append(" ");
                currentWidth += wordWidth;
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
            }
        }
        
        return lines;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
