module com.byod {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mindrot.jbcrypt;
    requires java.sql;

    opens com.example.byodsystem.byod to javafx.fxml;
    opens com.example.byodsystem.byod.controller to javafx.fxml;
    opens com.example.byodsystem.byod.model to javafx.fxml;

    exports com.example.byodsystem.byod;
}