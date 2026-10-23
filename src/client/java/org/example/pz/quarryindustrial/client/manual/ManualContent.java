package org.example.pz.quarryindustrial.client.manual;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.example.pz.quarryindustrial.Quarryindustrial;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

/**
 * Contenido del manual, leído de {@code assets/quarryindustrial/manual/<idioma>.json} (con en_us de respaldo).
 * Al ser un recurso, un resource pack lo puede traducir o ampliar.
 */
public final class ManualContent {
    private static final Gson GSON = new Gson();

    public record Book(List<Chapter> chapters) {
    }

    /** Un capítulo: título, item del ícono y sus páginas. */
    public record Chapter(String id, String title, String icon, List<Page> pages) {
    }

    /** Una página: título, texto (con \n para cortes) y, opcional, items para mostrar arriba a la derecha. */
    public record Page(String title, String text, List<String> items) {
    }

    private ManualContent() {
    }

    public static Book load() {
        Minecraft minecraft = Minecraft.getInstance();
        String language = minecraft.getLanguageManager().getSelected();
        return read(language).or(() -> read("en_us")).orElse(new Book(List.of()));
    }

    private static Optional<Book> read(String language) {
        Identifier id = Quarryindustrial.id("manual/" + language + ".json");
        return Minecraft.getInstance().getResourceManager().getResource(id).flatMap(resource -> {
            try (Reader reader = resource.openAsReader()) {
                Book book = GSON.fromJson(reader, Book.class);
                return book != null && book.chapters() != null ? Optional.of(book) : Optional.empty();
            } catch (IOException | JsonParseException e) {
                Quarryindustrial.LOGGER.warn("No se pudo leer el manual {}", id, e);
                return Optional.empty();
            }
        });
    }
}
