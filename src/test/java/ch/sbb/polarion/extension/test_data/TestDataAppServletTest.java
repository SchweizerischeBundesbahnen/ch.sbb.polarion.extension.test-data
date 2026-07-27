package ch.sbb.polarion.extension.test_data;

import ch.sbb.polarion.extension.generic.GenericUiServlet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestDataAppServletTest {

    @Test
    void instantiatesAsGenericUiServlet() {
        TestDataAppServlet servlet = new TestDataAppServlet();

        assertThat(servlet).isInstanceOf(GenericUiServlet.class);
    }
}
