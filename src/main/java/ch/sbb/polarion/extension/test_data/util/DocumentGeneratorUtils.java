package ch.sbb.polarion.extension.test_data.util;

import com.polarion.alm.tracker.model.IWorkItem;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

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

}
