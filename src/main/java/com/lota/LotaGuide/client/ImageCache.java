package com.lota.LotaGuide.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ImageCache {
    private static final ImageCache INSTANCE = new ImageCache();
    private static final int MAX_CACHE_SIZE = 50;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int CONNECTION_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    
    private final ConcurrentHashMap<String, CachedImage> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<CachedImage>> pendingDownloads = new ConcurrentHashMap<>();
    
    public static ImageCache getInstance() {
        return INSTANCE;
    }
    
    public static class CachedImage {
        private final List<ResourceLocation> frameTextures;
        private final List<Integer> frameDelays;
        private final int width;
        private final int height;
        private final boolean isAnimated;
        private final boolean isError;
        private final String errorMessage;
        
        public CachedImage(List<ResourceLocation> frameTextures, List<Integer> frameDelays, int width, int height) {
            this.frameTextures = frameTextures;
            this.frameDelays = frameDelays;
            this.width = width;
            this.height = height;
            this.isAnimated = frameTextures.size() > 1;
            this.isError = false;
            this.errorMessage = null;
        }
        
        public CachedImage(String errorMessage) {
            this.frameTextures = new ArrayList<>();
            this.frameDelays = new ArrayList<>();
            this.width = 0;
            this.height = 0;
            this.isAnimated = false;
            this.isError = true;
            this.errorMessage = errorMessage;
        }
        
        public ResourceLocation getTexture(long gameTime) {
            if (frameTextures.isEmpty()) return null;
            if (!isAnimated) return frameTextures.get(0);
            
            long totalCycleTime = 0;
            for (int delay : frameDelays) {
                totalCycleTime += delay;
            }
            if (totalCycleTime == 0) return frameTextures.get(0);
            
            long cyclePosition = (System.currentTimeMillis()) % totalCycleTime;
            long accumulated = 0;
            for (int i = 0; i < frameDelays.size(); i++) {
                accumulated += frameDelays.get(i);
                if (cyclePosition < accumulated) {
                    return frameTextures.get(i);
                }
            }
            return frameTextures.get(0);
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean isAnimated() { return isAnimated; }
        public boolean isError() { return isError; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    @Nullable
    public CachedImage getImage(String url) {
        if (url == null || url.isEmpty()) return null;
        
        CachedImage cached = cache.get(url);
        if (cached != null) return cached;
        
        if (!pendingDownloads.containsKey(url)) {
            startDownload(url);
        }
        
        return null;
    }
    
    public boolean isLoading(String url) {
        return pendingDownloads.containsKey(url);
    }
    
    private void startDownload(String url) {
        CompletableFuture<CachedImage> future = CompletableFuture.supplyAsync(() -> {
            try {
                return downloadImage(url);
            } catch (Exception e) {
                return new CachedImage("Error: " + e.getMessage());
            }
        });
        
        pendingDownloads.put(url, future);
        
        future.thenAccept(image -> {
            Minecraft.getInstance().execute(() -> {
                if (image != null) {
                    if (cache.size() >= MAX_CACHE_SIZE) {
                        String oldest = cache.keys().nextElement();
                        CachedImage removed = cache.remove(oldest);
                        if (removed != null) {
                            cleanupImage(removed);
                        }
                    }
                    cache.put(url, image);
                }
                pendingDownloads.remove(url);
            });
        });
    }
    
    private CachedImage downloadImage(String urlString) {
        try {
            if (!urlString.startsWith("https://") && !urlString.startsWith("http://")) {
                return new CachedImage("Invalid URL");
            }
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/*,*/*;q=0.8");
            connection.setInstanceFollowRedirects(true);
            
            try {
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return new CachedImage("HTTP " + responseCode);
                }
                
                int contentLength = connection.getContentLength();
                if (contentLength > MAX_FILE_SIZE) {
                    return new CachedImage("File too large");
                }
                
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] data = new byte[16384];
                    int bytesRead;
                    int totalRead = 0;
                    while ((bytesRead = inputStream.read(data)) != -1) {
                        totalRead += bytesRead;
                        if (totalRead > MAX_FILE_SIZE) {
                            return new CachedImage("File too large");
                        }
                        buffer.write(data, 0, bytesRead);
                    }
                }
                
                byte[] imageData = buffer.toByteArray();
                
                if (imageData.length < 10) {
                    return new CachedImage("Empty response");
                }
                
                // Check for GIF magic bytes (GIF87a or GIF89a)
                boolean isGif = imageData.length > 6 && 
                    imageData[0] == (byte)'G' && 
                    imageData[1] == (byte)'I' && 
                    imageData[2] == (byte)'F' &&
                    imageData[3] == (byte)'8' &&
                    (imageData[4] == (byte)'7' || imageData[4] == (byte)'9') &&
                    imageData[5] == (byte)'a';
                
                if (!isGif && urlString.toLowerCase().contains(".gif")) {
                    isGif = true;
                }
                
                if (isGif) {
                    CachedImage result = parseGif(imageData);
                    if (result != null && !result.isError()) {
                        return result;
                    }
                    // GIF parsing failed, try as static image
                }
                
                return parseStaticImage(imageData);
                
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            return new CachedImage("Download failed");
        }
    }
    
    private CachedImage parseStaticImage(byte[] imageData) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (bufferedImage == null) {
                return new CachedImage("Unsupported format");
            }
            
            ResourceLocation texture = createTexture(bufferedImage);
            if (texture == null) {
                return new CachedImage("Texture creation failed");
            }
            
            List<ResourceLocation> textures = new ArrayList<>();
            textures.add(texture);
            List<Integer> delays = new ArrayList<>();
            delays.add(0);
            
            return new CachedImage(textures, delays, bufferedImage.getWidth(), bufferedImage.getHeight());
        } catch (Exception e) {
            return new CachedImage("Image parse error");
        }
    }
    
    private CachedImage parseGif(byte[] imageData) {
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int width = 0;
        int height = 0;
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            ImageInputStream stream = ImageIO.createImageInputStream(bais);
            
            if (stream == null) {
                return null;
            }
            
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                stream.close();
                return null;
            }
            
            ImageReader reader = readers.next();
            reader.setInput(stream, false, false);
            
            BufferedImage firstFrame;
            try {
                firstFrame = reader.read(0);
            } catch (Exception e) {
                reader.dispose();
                stream.close();
                return null;
            }
            
            if (firstFrame == null) {
                reader.dispose();
                stream.close();
                return null;
            }
            
            width = firstFrame.getWidth();
            height = firstFrame.getHeight();
            
            // Create a canvas for compositing frames
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setBackground(new Color(0, 0, 0, 0));
            
            int frameIndex = 0;
            int maxFrames = 300; // Limit to prevent memory issues
            
            while (frameIndex < maxFrames) {
                BufferedImage frame;
                try {
                    frame = reader.read(frameIndex);
                } catch (IndexOutOfBoundsException e) {
                    break;
                } catch (Exception e) {
                    break;
                }
                
                if (frame == null) break;
                
                g.drawImage(frame, 0, 0, null);
                
                // Create a copy of the current canvas state
                BufferedImage completeFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = completeFrame.createGraphics();
                g2.drawImage(canvas, 0, 0, null);
                g2.dispose();
                
                frames.add(completeFrame);
                
                int delay = 100;
                try {
                    javax.imageio.metadata.IIOMetadata metadata = reader.getImageMetadata(frameIndex);
                    if (metadata != null) {
                        String[] formatNames = metadata.getMetadataFormatNames();
                        for (String formatName : formatNames) {
                            if (formatName.equals("javax_imageio_gif_image_1.0")) {
                                org.w3c.dom.Node root = metadata.getAsTree(formatName);
                                org.w3c.dom.NodeList children = root.getChildNodes();
                                for (int i = 0; i < children.getLength(); i++) {
                                    org.w3c.dom.Node child = children.item(i);
                                    if ("GraphicControlExtension".equals(child.getNodeName())) {
                                        org.w3c.dom.NamedNodeMap attrs = child.getAttributes();
                                        if (attrs != null) {
                                            org.w3c.dom.Node delayNode = attrs.getNamedItem("delayTime");
                                            if (delayNode != null) {
                                                int delayValue = Integer.parseInt(delayNode.getNodeValue());
                                                delay = delayValue * 10;
                                                if (delay <= 0) delay = 100;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                
                delays.add(delay);
                frameIndex++;
            }
            
            g.dispose();
            reader.dispose();
            stream.close();
            
        } catch (Exception e) {
            // Complete failure
            return null;
        }
        
        if (frames.isEmpty()) {
            return null;
        }
        
        List<ResourceLocation> textures = new ArrayList<>();
        
        for (BufferedImage frame : frames) {
            ResourceLocation texture = createTexture(frame);
            if (texture != null) {
                textures.add(texture);
            }
        }
        
        if (textures.isEmpty()) {
            return null;
        }
        
        while (delays.size() > textures.size()) {
            delays.remove(delays.size() - 1);
        }
        while (delays.size() < textures.size()) {
            delays.add(100);
        }
        
        return new CachedImage(textures, delays, width, height);
    }
    
    private ResourceLocation createTexture(BufferedImage image) {
        try {
            BufferedImage argbImage;
            if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
                argbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = argbImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            } else {
                argbImage = image;
            }
            
            NativeImage nativeImage = new NativeImage(argbImage.getWidth(), argbImage.getHeight(), false);
            
            for (int y = 0; y < argbImage.getHeight(); y++) {
                for (int x = 0; x < argbImage.getWidth(); x++) {
                    int argb = argbImage.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    // ABGR format for NativeImage
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }
            
            final NativeImage finalImage = nativeImage;
            final ResourceLocation[] result = new ResourceLocation[1];
            
            Minecraft mc = Minecraft.getInstance();
            if (mc.isSameThread()) {
                DynamicTexture dynamicTexture = new DynamicTexture(finalImage);
                result[0] = mc.getTextureManager().register("lotaguide_", dynamicTexture);
            } else {
                mc.executeBlocking(() -> {
                    DynamicTexture dynamicTexture = new DynamicTexture(finalImage);
                    result[0] = mc.getTextureManager().register("lotaguide_", dynamicTexture);
                });
            }
            
            return result[0];
        } catch (Exception e) {
            return null;
        }
    }
    
    private void cleanupImage(CachedImage image) {
        if (image != null && !image.isError()) {
            for (ResourceLocation loc : image.frameTextures) {
                if (loc != null) {
                    try {
                        Minecraft.getInstance().getTextureManager().release(loc);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
    
    public void clearCache() {
        for (CachedImage image : cache.values()) {
            cleanupImage(image);
        }
        cache.clear();
    }
    
    public void invalidate(String url) {
        CachedImage removed = cache.remove(url);
        if (removed != null) {
            cleanupImage(removed);
        }
    }
}
