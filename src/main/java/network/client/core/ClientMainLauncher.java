package network.client.core;

/**
 * Plain (non-{@link javafx.application.Application}) launcher for the GUI client.
 * Delegating to {@link ClientMain#main} from a class that does not extend
 * {@code Application} lets the JVM start JavaFX without the module-path check a
 * direct launch of an {@code Application} subclass would trigger.
 */
public class ClientMainLauncher {
    /**
     * @param args forwarded to {@link ClientMain#main}
     */
    public static void main(String[] args) {
        // Indirectly starts the JavaFX application, bypassing the module check.
        ClientMain.main(args);
    }
}
