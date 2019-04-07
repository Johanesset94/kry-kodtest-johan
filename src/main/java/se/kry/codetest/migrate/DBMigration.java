package se.kry.codetest.migrate;

import io.vertx.core.Vertx;
import se.kry.codetest.DBConnector;

public class DBMigration {
  /**
   * "Schema" of table services
   */
  private static String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS service (\n" +
          "url VARCHAR(128) NOT NULL PRIMARY KEY,\n" +
          "name VARCHAR(128),\n" +
          "date VARCHAR(128)" +
          ");";
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    DBConnector connector = new DBConnector(vertx);
    connector.query(SQL_CREATE_TABLE)
            .setHandler(done -> {
      if(done.succeeded()){
        System.out.println("completed db migrations");
      } else {
        done.cause().printStackTrace();
      }
      vertx.close(shutdown -> {
        System.exit(0);
      });
    });
  }
}
