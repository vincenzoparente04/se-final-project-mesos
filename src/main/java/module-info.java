module org.example.mesos {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens org.example.mesos to javafx.fxml;
    exports org.example.mesos;
}