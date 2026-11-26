package ch.sbb.polarion.extension.test_data.rest.controller;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.test_data.service.BaselineService;
import ch.sbb.polarion.extension.test_data.service.LinksService;
import ch.sbb.polarion.extension.test_data.service.ModuleService;
import ch.sbb.polarion.extension.test_data.service.ProjectTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class})
class TestDataInternalControllerTest {

    @Mock
    private PolarionService polarionService;
    @Mock
    private ModuleService moduleService;
    @Mock
    private ProjectTemplateService projectTemplateService;
    @Mock
    private LinksService linksService;
    @Mock
    private BaselineService baselineService;

    private TestDataInternalController controller;

    @BeforeEach
    void setUp() {
        lenient().when(polarionService.callPrivileged(any(Callable.class))).thenAnswer(invocation -> invocation.<Callable<?>>getArgument(0).call());
        controller = new TestDataInternalController(polarionService, moduleService, projectTemplateService, linksService, baselineService);
    }

    @Test
    void getTemplateHashReturnsTheStoredHash() {
        when(projectTemplateService.readTemplateHash("template")).thenReturn("abc123");

        try (Response response = controller.getTemplateHash("template")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("abc123", response.getEntity());
        }
    }

    @Test
    void getTemplateHashAnswersNotFoundWithoutAStoredHash() {
        when(projectTemplateService.readTemplateHash("template")).thenReturn(null);

        try (Response response = controller.getTemplateHash("template")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals("Hash not found for template: template", response.getEntity());
        }
    }

    @Test
    void getTemplateHashReportsAFailedReadAsSuch() {
        when(projectTemplateService.readTemplateHash("template"))
                .thenThrow(new ProjectTemplateService.TemplateProcessingException("Failed to read template hash: template", null));

        try (Response response = controller.getTemplateHash("template")) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals("Failed to retrieve template hash: Failed to read template hash: template", response.getEntity());
        }
    }

    @Test
    void getTemplateHashAnswersBadRequestForAnInvalidTemplateId() {
        when(projectTemplateService.readTemplateHash("a:b"))
                .thenThrow(new IllegalArgumentException("Template ID contains invalid characters"));

        try (Response response = controller.getTemplateHash("a:b")) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertEquals("Template ID contains invalid characters", response.getEntity());
        }
    }
}
