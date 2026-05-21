package view.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen in-game overlay that renders the rules PDF page by page
 * using Apache PDFBox and displays them in a scrollable panel.
 * Click ✕ or the dark backdrop to dismiss.
 *
 * <p>Pure view utility — no model dependency, no server commands.
 */
public final class RulesOverlay {

    private static final String RULES_PDF_PATH = "/images/mesos_rules_en.pdf";
    private static final float  RENDER_DPI     = 150f;
    private static final double PAGE_WIDTH     = 900.0;

    private RulesOverlay() {}

    /**
     * Shows the rules overlay on top of {@code root}.
     * Must be called on the JavaFX Application Thread.
     */
    public static void show(StackPane root) {
        List<Image> pages = loadPages();

        // ── Backdrop ────────────────────────────────────────────────────────
        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-card-zoom-backdrop");
        backdrop.setUserData("rules-overlay");

        // ── Pages stacked vertically in a ScrollPane ─────────────────────
        VBox pageBox = new VBox(8);
        pageBox.setAlignment(Pos.CENTER);
        pageBox.setPadding(new Insets(12));
        for (Image page : pages) {
            ImageView iv = new ImageView(page);
            iv.setFitWidth(PAGE_WIDTH);
            iv.setPreserveRatio(true);
            pageBox.getChildren().add(iv);
        }

        ScrollPane scroll = new ScrollPane(pageBox);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(PAGE_WIDTH + 40);
        scroll.setMaxWidth(PAGE_WIDTH + 40);
        scroll.setMaxHeight(750);
        scroll.getStyleClass().add("mesos-card-zoom-detail");

        // ── Close button ────────────────────────────────────────────────────
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("mesos-music-icon-btn");
        closeBtn.setOnAction(e -> root.getChildren().remove(backdrop));

        // ── Content wrapper ─────────────────────────────────────────────────
        StackPane content = new StackPane(scroll, closeBtn);
        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(6, 6, 0, 0));
        content.setMaxWidth(PAGE_WIDTH + 40);
        content.setMaxHeight(750);

        backdrop.getChildren().add(content);
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) root.getChildren().remove(backdrop);
        });

        root.getChildren().add(backdrop);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /** Loads all PDF pages as JavaFX Images via PDFBox. Returns empty list on failure. */
    private static List<Image> loadPages() {
        List<Image> result = new ArrayList<>();
        try (InputStream is = RulesOverlay.class.getResourceAsStream(RULES_PDF_PATH)) {
            if (is == null) return result;
            byte[] pdfBytes = is.readAllBytes();
            try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
                PDFRenderer renderer = new PDFRenderer(doc);
                for (int i = 0; i < doc.getNumberOfPages(); i++) {
                    BufferedImage bi = renderer.renderImageWithDPI(i, RENDER_DPI);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bi, "png", baos);
                    result.add(new Image(new ByteArrayInputStream(baos.toByteArray())));
                }
            }
        } catch (IOException ignored) {}
        return result;
    }
}
