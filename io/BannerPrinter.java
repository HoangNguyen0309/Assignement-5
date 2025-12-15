package io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BannerPrinter {

    private static final String DEFAULT_BANNER_PATH = "Legends_Monsters_and_Heroes/banner.txt";

    private final Renderer renderer;
    private final Path bannerPath;
    private final String classpathLocation;

    public BannerPrinter(Renderer renderer) {
        this(renderer, DEFAULT_BANNER_PATH);
    }

    public BannerPrinter(Renderer renderer, String bannerPath) {
        this.renderer = renderer;
        this.bannerPath = Path.of(bannerPath);
        this.classpathLocation = "/" + bannerPath.replace("\\", "/");
    }

    public void printValorBanner() {
        String art = loadBanner();
        if (art != null && !art.isEmpty()) {
            String redArt = "\u001B[31m" + art + "\u001B[0m";
            renderer.renderMessage(redArt);
        } else {
            renderer.renderMessage("Unable to load banner at " + bannerPath);
        }
    }

    private String loadBanner() {
        // Try filesystem first (useful when running from project root)
        try {
            if (Files.exists(bannerPath)) {
                return Files.readString(bannerPath);
            }
        } catch (IOException ignored) {
            // Fall through to classpath attempt
        }

        // Fallback: try to load from classpath (useful when packaged)
        try (InputStream in = BannerPrinter.class.getResourceAsStream(classpathLocation)) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }

        return null;
    }
}
