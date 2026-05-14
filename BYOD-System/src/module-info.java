module com.byod {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.byod to javafx.fxml;
    exports com.byod;
}