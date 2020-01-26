package ua.org.java.dynamoit.tables;

import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "TablesTopComponent"
)
@TopComponent.Registration(
        mode = "explorer",
        openAtStartup = true
)
@NbBundle.Messages({
        "CTL_TablesTopComponent=Tables",
        "HINT_TablesTopComponent=List of tables"
})
public class TablesTopComponent extends TopComponent {

    public TablesTopComponent() {
        setName(Bundle.CTL_TablesTopComponent());
        setToolTipText(Bundle.HINT_TablesTopComponent());
    }
}
