package ch.sbb.polarion.extension.test_data.rest.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecordsTest {

    @Test
    void documentRefDefaultsSpaceIdWhenNull() {
        assertEquals("_default", new DocumentRef(null, "doc").spaceId());
    }

    @Test
    void documentRefDefaultsSpaceIdWhenBlank() {
        assertEquals("_default", new DocumentRef("  ", "doc").spaceId());
    }

    @Test
    void documentRefKeepsExplicitSpaceId() {
        assertEquals("custom", new DocumentRef("custom", "doc").spaceId());
    }

    @Test
    void crossDocLinksRequestDefaultsLinksPerWorkItemAndRole() {
        CrossDocumentLinksRequest r = new CrossDocumentLinksRequest(List.of(), null, null);
        assertEquals(1, r.linksPerWorkItem());
        assertEquals("relates_to", r.linkRole());
    }

    @Test
    void crossDocLinksRequestDefaultsBlankRole() {
        assertEquals("relates_to", new CrossDocumentLinksRequest(List.of(), 5, "  ").linkRole());
    }

    @Test
    void crossDocLinksRequestKeepsExplicitValues() {
        CrossDocumentLinksRequest r = new CrossDocumentLinksRequest(List.of(), 3, "depends_on");
        assertEquals(3, r.linksPerWorkItem());
        assertEquals("depends_on", r.linkRole());
    }

    @Test
    void linkedRevisionsRequestDefaultsWorkItemsPerRevision() {
        LinkedRevisionsRequest r = new LinkedRevisionsRequest(List.of("1"), null, null);
        assertEquals(1, r.workItemsPerRevision());
        assertNull(r.comment());
    }

    @Test
    void linkedRevisionsRequestKeepsExplicitValues() {
        LinkedRevisionsRequest r = new LinkedRevisionsRequest(List.of("1"), 7, "note");
        assertEquals(7, r.workItemsPerRevision());
        assertEquals("note", r.comment());
    }

    @Test
    void collectionElementRefDefaultsSpaceIdWhenNullOrBlank() {
        assertEquals("_default", new CollectionElementRef(null, "doc", "1").spaceId());
        assertEquals("_default", new CollectionElementRef("", "doc", "1").spaceId());
    }

    @Test
    void collectionElementRefKeepsExplicitSpaceId() {
        assertEquals("custom", new CollectionElementRef("custom", "doc", "1").spaceId());
    }

    @Test
    void baselineResponseStoresFields() {
        BaselineResponse r = new BaselineResponse("name", "42");
        assertEquals("name", r.name());
        assertEquals("42", r.revision());
    }

    @Test
    void collectionRequestStoresFields() {
        CollectionRequest r = new CollectionRequest("desc", List.of());
        assertEquals("desc", r.description());
        assertEquals(0, r.elements().size());
    }
}
