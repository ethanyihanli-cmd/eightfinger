module com.macondo.eightfinger {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;


    opens com.macondo.eightfinger to javafx.fxml;
    exports com.macondo.eightfinger;
    exports com.macondo.eightfinger.model;
    exports com.macondo.eightfinger.data;

}