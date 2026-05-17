module auction.common {
    // 1. External Libraries
    requires com.google.gson;
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    // 2. Exports: Allows other modules to use these classes
    exports com.auction.project.Entities;
    exports com.auction.project.Entities.enums;
    exports com.auction.project.Packets;
    exports com.auction.project.Exception;
    exports com.auction.project.Observer;
    exports com.auction.project.Factory;
    exports com.auction.project.ManagerServer;

    // 3. Opens: Combined to fix the "Duplicate opens" error
    // We open Entities to Gson (for JSON), and to Hibernate/Jakarta (for Database mapping)
    opens com.auction.project.Entities to com.google.gson, org.hibernate.orm.core, jakarta.persistence;

    // Packets only need to be open to Gson for network transmission
    opens com.auction.project.Packets to com.google.gson;
}