package ch.sbb.polarion.extension.test_data.util;

import com.polarion.alm.tracker.model.IWorkItem;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@UtilityClass
@SuppressWarnings("java:S1192")
public class DocumentGeneratorUtils {
    public static final String HEADING = "heading";
    public static final String REQUIREMENT = "requirement";
    public static final int NUMBER_OF_ELEMENTS_BOUND = 10;
    public static final int IMAGE_WIDTH_BOUND = 200;
    public static final int IMAGE_HEIGHT_BOUND = 200;
    public static final int NUMBER_OF_IMAGES_BOUND = 3;
    public static final int IMAGE_DIMENSION_MIN_SIZE = 100;
    public static final int MINIMAL_COUNT = 5;

    // Large PNG image defaults
    public static final int LARGE_IMAGE_DEFAULT_WIDTH = 1920;
    public static final int LARGE_IMAGE_DEFAULT_HEIGHT = 1080;
    public static final int LARGE_IMAGE_DEFAULT_COUNT = 3;
    public static final int LARGE_IMAGE_SHAPES_COUNT = 50;

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int NUMBER_OF_SHAPES = 10;

    public static @NotNull String generateWorkItemType() {
        return RANDOM.nextBoolean() ? HEADING : REQUIREMENT;
    }

    public static @NotNull String generateRandomHtmlText() {
        StringBuilder html = new StringBuilder();
        String[] words = {
                "Lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do",
                "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua", "Ut",
                "enim", "ad", "minim", "veniam", "quis", "nostrud", "exercitation", "ullamco", "laboris",
                "nisi", "ut", "aliquip", "ex", "ea", "commodo", "consequat", "Duis", "aute", "irure",
                "dolor", "in", "reprehenderit", "in", "voluptate", "velit", "esse", "cillum", "dolore",
                "eu", "fugiat", "nulla", "pariatur", "Excepteur", "sint", "occaecat", "cupidatat", "non",
                "proident", "sunt", "in", "culpa", "qui", "officia", "deserunt", "mollit", "anim", "id",
                "est", "laborum", "Curabitur", "pretium", "tincidunt", "lacus", "Suspendisse", "potenti",
                "Sed", "nec", "metus", "mi", "Vivamus", "ac", "augue", "eget", "arcu", "dictum", "varius",
                "Duis", "at", "consectetur", "lectus", "Fusce", "dapibus", "Phasellus", "non", "arcu",
                "porttitor", "orci", "mollis", "sed", "sem", "Pellentesque", "habitant", "morbi",
                "tristique", "senectus", "netus", "et", "malesuada", "fames", "ac", "turpis", "egestas"
        };

        int numElements = RANDOM.nextInt(NUMBER_OF_ELEMENTS_BOUND) + MINIMAL_COUNT;

        for (int i = 0; i < numElements; i++) {
            int numWords = RANDOM.nextInt(NUMBER_OF_ELEMENTS_BOUND) + MINIMAL_COUNT;

            html.append("<strong><em>");
            for (int j = 0; j < numWords; j++) {
                html.append(words[RANDOM.nextInt(words.length)]).append(" ");
            }
            html.append("</em></strong>\n");

            html.append("<u>");
            for (int j = 0; j < numWords; j++) {
                html.append(words[RANDOM.nextInt(words.length)]).append(" ");
            }
            html.append("</u>\n");

            html.append("<s>");
            for (int j = 0; j < numWords; j++) {
                html.append(words[RANDOM.nextInt(words.length)]).append(" ");
            }
            html.append("</s>\n");
        }

        return html.toString();
    }

    public static @NotNull String generateRandomHtmlImages() {
        int height = RANDOM.nextInt(IMAGE_HEIGHT_BOUND) + IMAGE_DIMENSION_MIN_SIZE;
        int width = RANDOM.nextInt(IMAGE_WIDTH_BOUND) + IMAGE_DIMENSION_MIN_SIZE;
        int numberOfImages = RANDOM.nextInt(NUMBER_OF_IMAGES_BOUND);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfImages; i++) {
            String svg = generateRandomSVG(width, height);
            String base64 = Base64.getEncoder().encodeToString(svg.getBytes());
            result.append("<img src=\"data:image/svg+xml;base64,%s\" alt=\"Random SVG Image\">".formatted(base64));
        }
        return result.toString();
    }

    private static String generateRandomSVG(int width, int height) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\">\n");

        String bgColor = String.format("#%06X", RANDOM.nextInt(0xFFFFFF));
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"")
                .append(bgColor)
                .append("\" />\n");

        for (int i = 0; i < NUMBER_OF_SHAPES; i++) {
            String color = String.format("#%06X", RANDOM.nextInt(0xFFFFFF));
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            int w = RANDOM.nextInt(width / 2);
            int h = RANDOM.nextInt(height / 2);
            svg.append("<rect x=\"")
                    .append(x)
                    .append("\" y=\"")
                    .append(y)
                    .append("\" width=\"")
                    .append(w)
                    .append("\" height=\"")
                    .append(h)
                    .append("\" fill=\"")
                    .append(color)
                    .append("\" />\n");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    public static @NotNull String generateRandomWorkItemLinks(@NotNull List<IWorkItem> workItems) {
        StringBuilder result = new StringBuilder();
        int numberOfLinks = RANDOM.nextInt(NUMBER_OF_ELEMENTS_BOUND);

        if (workItems.isEmpty()) {
            return result.toString();
        }

        for (int i = 0; i < numberOfLinks; i++) {
            IWorkItem workItem = workItems.get(RANDOM.nextInt(workItems.size()));

            String link = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"" + workItem.getId() + "\" data-option-id=\"long\">\n" +
                    "\t<span class=\"polarion-no-style-cleanup\" style=\"white-space:nowrap;\" title=\"" + workItem.getId() + " - " + workItem.getTitle() + "\">\n" +
                    "\t\t<a style=\"font-size:1em;\" target=\"_top\" class=\"polarion-Hyperlink\" href=\"/polarion/#/project/" + workItem.getProjectId() + "/workitem?id=" + workItem.getId() + "\">\n" +
                    "\t\t\t<span style=\"white-space:nowrap;\">\n" +
                    "\t\t\t\t<img src=\"/polarion/icons/default/enums/type_requirement.gif\" class=\"polarion-Icons\" onmousedown=\"return false;\" contenteditable=\"false\">\n" +
                    "\t\t\t</span>\n" +
                    "\t\t\t<span style=\"color:#000000;\">" + workItem.getId() + "</span>\n" +
                    "\t\t\t<span style=\"white-space: normal\"> - " + workItem.getTitle() + "</span>\n" +
                    "\t\t</a>\n" +
                    "\t</span>\n" +
                    "</span>";

            result.append(link);
        }

        return result.toString();
    }

    /**
     * Generates HTML with large PNG images embedded as base64.
     *
     * @param imageCount  number of images to generate
     * @param imageWidth  width of each image in pixels
     * @param imageHeight height of each image in pixels
     * @return HTML string with embedded PNG images
     */
    public static @NotNull String generateLargePngImages(int imageCount, int imageWidth, int imageHeight) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < imageCount; i++) {
            byte[] pngBytes = generateRandomPngImage(imageWidth, imageHeight);
            String base64 = Base64.getEncoder().encodeToString(pngBytes);
            result.append("<p><img src=\"data:image/png;base64,%s\" alt=\"Large PNG Image %d\" width=\"%d\" height=\"%d\"></p>\n"
                    .formatted(base64, i + 1, imageWidth, imageHeight));
        }
        return result.toString();
    }

    /**
     * Generates a random PNG image with colorful shapes, gradients, and patterns.
     *
     * @param width  image width in pixels
     * @param height image height in pixels
     * @return PNG image as byte array
     */
    public static byte[] generateRandomPngImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Generate gradient background
        Color bgColor1 = new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
        Color bgColor2 = new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
        GradientPaint gradient = new GradientPaint(0, 0, bgColor1, width, height, bgColor2);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Draw random shapes
        for (int i = 0; i < LARGE_IMAGE_SHAPES_COUNT; i++) {
            Color shapeColor = new Color(
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    RANDOM.nextInt(256),
                    100 + RANDOM.nextInt(156) // Semi-transparent
            );
            g2d.setColor(shapeColor);

            int shapeType = RANDOM.nextInt(4);
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            int w = 50 + RANDOM.nextInt(width / 3);
            int h = 50 + RANDOM.nextInt(height / 3);

            switch (shapeType) {
                case 0 -> g2d.fillRect(x, y, w, h);
                case 1 -> g2d.fillOval(x, y, w, h);
                case 2 -> g2d.fillRoundRect(x, y, w, h, 20, 20);
                case 3 -> {
                    int[] xPoints = {x, x + w / 2, x + w};
                    int[] yPoints = {y + h, y, y + h};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                }
            }
        }

        // Add some lines for visual complexity
        for (int i = 0; i < 20; i++) {
            Color lineColor = new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(1 + RANDOM.nextInt(5)));
            g2d.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                    RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PNG image", e);
        }
    }

    /**
     * Generates extended Lorem Ipsum text for large documents.
     * Produces more content than the standard method.
     *
     * @param paragraphs number of paragraphs to generate
     * @return HTML string with multiple paragraphs
     */
    public static @NotNull String generateExtendedHtmlText(int paragraphs) {
        StringBuilder html = new StringBuilder();
        String[] words = {
                "Lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do",
                "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua", "Ut",
                "enim", "ad", "minim", "veniam", "quis", "nostrud", "exercitation", "ullamco", "laboris",
                "nisi", "aliquip", "ex", "ea", "commodo", "consequat", "Duis", "aute", "irure",
                "reprehenderit", "voluptate", "velit", "esse", "cillum", "eu", "fugiat", "nulla", "pariatur",
                "Excepteur", "sint", "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui",
                "officia", "deserunt", "mollit", "anim", "id", "est", "laborum", "Curabitur", "pretium",
                "tincidunt", "lacus", "Suspendisse", "potenti", "Sed", "nec", "metus", "mi", "Vivamus",
                "ac", "augue", "eget", "arcu", "dictum", "varius", "at", "lectus", "Fusce", "dapibus",
                "Phasellus", "porttitor", "orci", "mollis", "sem", "Pellentesque", "habitant", "morbi",
                "tristique", "senectus", "netus", "malesuada", "fames", "turpis", "egestas", "vitae",
                "sapien", "vel", "mauris", "commodo", "facilisis", "Nulla", "posuere", "sollicitudin",
                "aliquam", "ultrices", "sagittis", "massa", "urna", "erat", "sodales", "ligula"
        };

        for (int p = 0; p < paragraphs; p++) {
            html.append("<p>");

            // Each paragraph has 5-10 sentences
            int sentences = 5 + RANDOM.nextInt(6);
            for (int s = 0; s < sentences; s++) {
                // Each sentence has 10-20 words
                int wordCount = 10 + RANDOM.nextInt(11);
                StringBuilder sentence = new StringBuilder();
                for (int w = 0; w < wordCount; w++) {
                    String word = words[RANDOM.nextInt(words.length)];
                    if (w == 0) {
                        word = word.substring(0, 1).toUpperCase() + word.substring(1);
                    }
                    sentence.append(word);
                    if (w < wordCount - 1) {
                        sentence.append(" ");
                    }
                }
                sentence.append(". ");

                // Randomly add formatting
                int format = RANDOM.nextInt(5);
                switch (format) {
                    case 0 -> html.append("<strong>").append(sentence).append("</strong>");
                    case 1 -> html.append("<em>").append(sentence).append("</em>");
                    case 2 -> html.append("<u>").append(sentence).append("</u>");
                    default -> html.append(sentence);
                }
            }

            html.append("</p>\n");
        }

        return html.toString();
    }

}
