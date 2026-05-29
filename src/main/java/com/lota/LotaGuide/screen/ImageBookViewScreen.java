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
    private final Player player;
    private final ItemStack bookStack;
    private final InteractionHand hand;
    private final ImageBookData bookData;
    private final boolean isAuthor;
    private ImageBookLayout layout;
    
    private int currentPage = 0;
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

        this.layout = ImageBookLayout.forView(this.width, this.height, false, isAuthor, true);

        this.prevPageButton = Button.builder(Component.literal("<"), button -> previousPage())
            .bounds(layout.prevButtonX, layout.bottomRowY, ImageBookLayout.navButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.prevPageButton);

        this.doneButton = Button.builder(Component.translatable("gui.done"),
            button -> onClose())
            .bounds(layout.centerButtonX, layout.bottomRowY, ImageBookLayout.actionButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.doneButton);

        this.nextPageButton = Button.builder(Component.literal(">"), button -> nextPage())
            .bounds(layout.nextButtonX, layout.bottomRowY, ImageBookLayout.navButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.nextPageButton);

        if (isAuthor) {
            this.editButton = Button.builder(Component.translatable("screen.lotaguide.image_book.edit"), 
                button -> openEditMode())
                .bounds(layout.editButtonX, layout.editButtonY, layout.editButtonWidth, 20)
                .build();
            this.addRenderableWidget(this.editButton);
        }

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

        String imageUrl = page.getImageUrl();
        boolean hasImage = false;
        boolean hasText = !page.getText().isEmpty();
        ImageBookLayout renderLayout = ImageBookLayout.forView(this.width, this.height, hasText, isAuthor, page.isImageVisible());
        int textBoxX = renderLayout.textX;
        int textBoxY = renderLayout.textY;
        int textBoxWidth = renderLayout.textWidth;
        int textBoxHeight = renderLayout.textHeight;

        if (page.isImageVisible() && !imageUrl.isEmpty()) {
            ImageCache.CachedImage cachedImage = ImageCache.getInstance().getImage(imageUrl);

            if (cachedImage != null && !cachedImage.isError()) {
                ResourceLocation texture = cachedImage.getTexture(System.currentTimeMillis());
                if (texture != null) {
                    hasImage = true;
                    RenderSystem.setShaderTexture(0, texture);

                    int imgWidth = cachedImage.getWidth();
                    int imgHeight = cachedImage.getHeight();

                    float scale = Math.min(
                        (float) renderLayout.imageWidth / imgWidth,
                        (float) renderLayout.imageHeight / imgHeight
                    );
                    imgWidth = Math.max(1, (int) (imgWidth * scale));
                    imgHeight = Math.max(1, (int) (imgHeight * scale));

                    int renderX = renderLayout.imageX + (renderLayout.imageWidth - imgWidth) / 2;
                    int renderY = renderLayout.imageY + (renderLayout.imageHeight - imgHeight) / 2;

                    graphics.blit(texture, renderX, renderY, 0, 0, imgWidth, imgHeight,
                        imgWidth, imgHeight);
                }
            }
        }

        if (page.isImageVisible()) {
            graphics.fill(renderLayout.imageX, renderLayout.imageY,
                renderLayout.imageX + renderLayout.imageWidth, renderLayout.imageY + renderLayout.imageHeight,
                hasImage ? 0x00000000 : 0xFF333333);

            if (!hasImage) {
                if (!imageUrl.isEmpty()) {
                    ImageCache.CachedImage cachedImage = ImageCache.getInstance().getImage(imageUrl);
                    if (cachedImage != null && cachedImage.isError()) {
                        graphics.drawCenteredString(this.font, cachedImage.getErrorMessage(),
                            renderLayout.contentLeft + renderLayout.contentWidth / 2,
                            renderLayout.imageY + renderLayout.imageHeight / 2, 0xFF5555);
                    } else if (ImageCache.getInstance().isLoading(imageUrl)) {
                        long dots = (System.currentTimeMillis() / 500) % 4;
                        String loading = "Loading" + ".".repeat((int) dots);
                        graphics.drawCenteredString(this.font, loading,
                            renderLayout.contentLeft + renderLayout.contentWidth / 2,
                            renderLayout.imageY + renderLayout.imageHeight / 2, 0xAAAAAA);
                    }
                } else {
                    graphics.drawCenteredString(this.font,
                        Component.translatable("screen.lotaguide.image_book.no_image").getString(),
                        renderLayout.contentLeft + renderLayout.contentWidth / 2,
                        renderLayout.imageY + renderLayout.imageHeight / 2, 0x888888);
                }
            }
        }

        if (hasText && textBoxHeight > 0) {
            String text = page.getText();
            graphics.fill(textBoxX, textBoxY, textBoxX + textBoxWidth, textBoxY + textBoxHeight, 0x66000000);

            List<String> lines = wrapText(text, textBoxWidth - 12);
            int yOffset = 0;
            int maxLines = textBoxHeight / 12;
            for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
                graphics.drawString(this.font, lines.get(i), textBoxX + 6, textBoxY + 4 + yOffset, 0xFFFFFF);
                yOffset += 12;
            }
        }

        String pageIndicator = (currentPage + 1) + " / " + bookData.getPageCount();
        graphics.drawCenteredString(this.font, pageIndicator,
            renderLayout.contentLeft + renderLayout.contentWidth / 2, renderLayout.pageIndicatorY + 5, 0xFFFFFF);

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
