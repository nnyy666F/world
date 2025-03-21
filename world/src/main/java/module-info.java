module com.mirror.world {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.mirror.world to javafx.fxml;
    exports com.mirror.world;
    exports com.mirror.block;
    opens com.mirror.block to javafx.fxml;
}