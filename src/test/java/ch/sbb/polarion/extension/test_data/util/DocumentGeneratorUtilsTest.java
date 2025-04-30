package ch.sbb.polarion.extension.test_data.util;

import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DocumentGeneratorUtilsTest {

    @Test
    void generateWorkItemType() {
        String workItemType = DocumentGeneratorUtils.generateWorkItemType();
        assertThat(workItemType).isIn(DocumentGeneratorUtils.HEADING, DocumentGeneratorUtils.REQUIREMENT);
    }

    @Test
    void generateRandomHtmlText() {
        String randomHtmlText = DocumentGeneratorUtils.generateRandomHtmlText();

        assertNotNull(randomHtmlText);
        String[] elements = randomHtmlText.split("</s>\n");
        assertTrue(elements.length >= DocumentGeneratorUtils.MINIMAL_COUNT && elements.length <= (DocumentGeneratorUtils.NUMBER_OF_ELEMENTS_BOUND + DocumentGeneratorUtils.MINIMAL_COUNT));

        String regex = "(<strong><em>.*?</em></strong>\\n<u>.*?</u>\\n<s>.*?</s>\\n)+";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(randomHtmlText);
        assertTrue(matcher.matches());
    }

    @Test
    void generateRandomHtmlImages() {
        String randomHtmlImages = DocumentGeneratorUtils.generateRandomHtmlImages();
        assertNotNull(randomHtmlImages);

        long imageCount = randomHtmlImages.chars().filter(ch -> ch == '<').count();
        assertTrue(imageCount >= 0 && imageCount <= DocumentGeneratorUtils.NUMBER_OF_IMAGES_BOUND);

        String regex = "(<img src=\"data:image/svg\\+xml;base64,[^\"]+\" alt=\"Random SVG Image\">)+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(randomHtmlImages);

        while (matcher.find()) {
            String imageTag = matcher.group();
            assertTrue(imageTag.contains("data:image/svg+xml;base64,"));
        }
    }

    @Test
    void generateRandomWorkItemLinks() {
        String emptyWorkItemLinks = DocumentGeneratorUtils.generateRandomWorkItemLinks(new ArrayList<>());
        assertEquals("", emptyWorkItemLinks);

        List<IWorkItem> workitems = new ArrayList<>();
        IWorkItem workItem1 = mock(IWorkItem.class);
        IWorkItem workItem2 = mock(IWorkItem.class);
        IWorkItem workItem3 = mock(IWorkItem.class);
        workitems.add(workItem1);
        workitems.add(workItem2);
        workitems.add(workItem3);
        String randomWorkItemLinks = DocumentGeneratorUtils.generateRandomWorkItemLinks(workitems);

        long linkCount = randomWorkItemLinks.split("</span><span class=\"polarion-rte-link\"").length - 1;
        System.out.println(linkCount);
        assertTrue(linkCount >= 0 && linkCount <= DocumentGeneratorUtils.NUMBER_OF_ELEMENTS_BOUND);

        String regex = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"[^\"]+\" data-option-id=\"long\">.*?</span>";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(randomWorkItemLinks);

        while (matcher.find()) {
            String link = matcher.group();
            assertThat(link).contains("data-item-id=");
            assertThat(link).contains("href=\"/polarion/#/project/");
        }
    }
}
