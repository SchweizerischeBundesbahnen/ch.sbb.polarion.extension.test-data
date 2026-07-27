package ch.sbb.polarion.extension.test_data;

import ch.sbb.polarion.extension.generic.GenericUiServlet;

import java.io.Serial;

/**
 * Serves the React single-page app from its own webapp context ({@code test-data-app}). The
 * admin extenders in hivemodule.xml open it as
 * {@code /polarion/test-data-app/ui/app/index.html?feature=<id>}; everything else about the
 * request handling comes from the generic servlet.
 */
public class TestDataAppServlet extends GenericUiServlet {

    @Serial
    private static final long serialVersionUID = 6893052734118250371L;

    public TestDataAppServlet() {
        super("test-data-app");
    }
}
