package com.lota.LotaGuide.screen;

import com.lota.LotaGuide.LotaGuide;
import com.lota.LotaGuide.client.ImageCache;
import com.lota.LotaGuide.data.ImageBookData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Screen for editing an Image Book.
 * Layout: URL field -> Image -> Text -> Page controls
 */
@OnlyIn(Dist.CLIENT)
public class ImageBookEditScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int IMAGE_AREA_HEIGHT = 210;
    private static final int IMAGE_AREA_WIDTH = 280;
    private static final int PADDING = 10;
    private static final int TEXT_FIELD_HEIGHT = 40;
    
    private final Player player;
    private final ItemStack bookStack;
    private final InteractionHand hand;
    private final ImageBookData bookData;
    
    private int currentPage = 0;
    private int leftPos;
    private int topPos;
    
    private EditBox urlField;
    private MultiLineEditBox textField;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button addPageButton;
    private Button removePageButton;
    private Button signButton;
    private Button cancelButton;
    
    public ImageBookEditScreen(Player player, ItemStack bookStack, InteractionHand hand, ImageBookData bookData) {
        super(Component.empty());
        this.player = player;
        this.bookStack = bookStack;
        this.hand = hand;
        this.bookData = bookData;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = 10;
        
        int fieldWidth = IMAGE_AREA_WIDTH;
        int fieldX = leftPos + (PANEL_WIDTH - fieldWidth) / 2;
        int currentY = topPos;
        
        this.urlField = new EditBox(this.font, fieldX, currentY, fieldWidth, 20, 
            Component.empty());
        this.urlField.setMaxLength(500);
        this.urlField.setHint(Component.literal("https://example.com/image.png"));
        this.urlField.setResponder(this::onUrlChanged);
        this.addRenderableWidget(this.urlField);
        
        currentY += 25;
        currentY += IMAGE_AREA_HEIGHT + PADDING;
        
        this.textField = new MultiLineEditBox(this.font, fieldX, currentY, fieldWidth, TEXT_FIELD_HEIGHT,
            Component.empty(), Component.empty());
        this.textField.setCharacterLimit(1000);
        this.textField.setValueListener(this::onTextChanged);
        this.addRenderableWidget(this.textField);
        
        currentY += TEXT_FIELD_HEIGHT + PADDING;
        
        int pageCounterY = currentY;
        int pageCounterWidth = 50;
        int centerX = leftPos + PANEL_WIDTH / 2;
        
        this.removePageButton = Button.builder(Component.literal("-"), button -> removePage())
            .bounds(centerX - pageCounterWidth / 2 - 25, pageCounterY, 20, 20)
            .build();
        this.addRenderableWidget(this.removePageButton);
        
        this.addPageButton = Button.builder(Component.literal("+"), button -> addPage())
            .bounds(centerX + pageCounterWidth / 2 + 5, pageCounterY, 20, 20)
            .build();
        this.addRenderableWidget(this.addPageButton);
        
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
        
        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), 
            button -> onClose())
            .bounds(leftPos + PADDING, currentY, 60, 20)
            .build();
        this.addRenderableWidget(this.cancelButton);
        
        this.signButton = Button.builder(Component.translatable("screen.lotaguide.image_book.sign"), 
            button -> signBook())
            .bounds(leftPos + PANEL_WIDTH - PADDING - 60, currentY, 60, 20)
            .build();
        this.addRenderableWidget(this.signButton);
        
        loadPageData();
        updateNavigationButtons();
    }
    
    private void loadPageData() {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            this.urlField.setValue(page.getImageUrl());
            this.textField.setValue(page.getText());
        }
    }
    
    private void saveCurrentPage() {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.setImageUrl(this.urlField.getValue());
            page.setText(this.textField.getValue());
        }
    }
    
    private void updateNavigationButtons() {
        this.prevPageButton.active = currentPage > 0;
        this.nextPageButton.active = currentPage < bookData.getPageCount() - 1;
        this.removePageButton.active = bookData.getPageCount() > 1;
    }
    
    private void onUrlChanged(String url) {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.setImageUrl(url);
        }
    }
    
    private void onTextChanged(String text) {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.setText(text);
        }
    }
    
    private void previousPage() {
        if (currentPage > 0) {
            saveCurrentPage();
            currentPage--;
            loadPageData();
            updateNavigationButtons();
        }
    }
    
    private void nextPage() {
        if (currentPage < bookData.getPageCount() - 1) {
            saveCurrentPage();
            currentPage++;
            loadPageData();
            updateNavigationButtons();
        }
    }
    
    private void addPage() {
        saveCurrentPage();
        bookData.addPage();
        currentPage = bookData.getPageCount() - 1;
        loadPageData();
        updateNavigationButtons();
    }
    
    private void removePage() {
        if (bookData.getPageCount() > 1) {
            bookData.removePage(currentPage);
            if (currentPage >= bookData.getPageCount()) {
                currentPage = bookData.getPageCount() - 1;
            }
            loadPageData();
            updateNavigationButtons();
        }
    }
    
    private void signBook() {
        saveCurrentPage();
        
        bookData.setAuthorUUID(player.getUUID());
        bookData.setAuthorName(player.getName().getString());
        
        ItemStack signedBook = new ItemStack(LotaGuide.SIGNED_IMAGE_BOOK.get());
        bookData.saveToStack(signedBook);
        
        player.setItemInHand(hand, signedBook);
        
        this.onClose();
    }
    
    @Override
    public void onClose() {
        saveCurrentPage();
        bookData.saveToStack(bookStack);
        super.onClose();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        int imageAreaX = leftPos + (PANEL_WIDTH - IMAGE_AREA_WIDTH) / 2;
        int imageAreaY = topPos + 25;
        
        String imageUrl = this.urlField.getValue();
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
                    
                    // Center the image in the area
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
            } else {
                graphics.drawCenteredString(this.font, 
                    Component.translatable("screen.lotaguide.image_book.no_image").getString(), 
                    leftPos + PANEL_WIDTH / 2, imageAreaY + IMAGE_AREA_HEIGHT / 2, 0x888888);
            }
        }
        
        int pageCounterY = topPos + 25 + IMAGE_AREA_HEIGHT + PADDING + TEXT_FIELD_HEIGHT + PADDING;
        String pageIndicator = (currentPage + 1) + " / " + bookData.getPageCount();
        graphics.drawCenteredString(this.font, pageIndicator, 
            leftPos + PANEL_WIDTH / 2, pageCounterY + 5, 0xFFFFFF);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
