package view.prevDev;

/** Plain launcher — avoids the "JavaFX runtime components are missing" error
 *  when running directly from IntelliJ without module-path VM flags. */
public class BoardPreviewLauncher {
    public static void main(String[] args) {
        BoardPreviewApp.main(args);
    }
}
