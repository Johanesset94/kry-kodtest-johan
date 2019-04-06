package se.kry.codetest;

import com.sun.org.apache.xpath.internal.operations.Bool;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import netscape.javascript.JSObject;
import se.kry.codetest.migrate.DBMigration;

import javax.swing.text.StyledEditorKit;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private HashMap<String, String> services = new HashMap<>();
    private DBConnector connector;
    private BackgroundPoller poller = new BackgroundPoller();

    private String SQL_ALL_SERVICES = "SELECT * FROM service";
    private String SQL_INSERT_SERVICE = "INSERT INTO service (\n url) VALUES('";
    private String SQL_DELETE_SERVICE = "DELETE FROM service WHERE url = '";

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        services.put("https://www.kry.se", "UNKNOWN");
        //saveService(new JsonObject().put("url", "https://www.kry.se"));
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
        setRoutes(router);
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(req -> {
            fetchServices().setHandler(done -> {
                List<JsonObject> jsonServices = services
                        .entrySet()
                        .stream()
                        .map(service ->
                                new JsonObject()
                                        .put("name", service.getKey())
                                        .put("status", service.getValue()))
                        .collect(Collectors.toList());
                req.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonArray(jsonServices).encode());
            });
        });
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            saveService(jsonBody);
            services.put(jsonBody.getString("url"), "UNKNOWN");
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
        router.delete("/service").handler(req ->{
            JsonObject jsonBody = req.getBodyAsJson();
            deleteService(jsonBody).setHandler(done ->{
                if (done.succeeded()) {
                    req.response()
                            .setStatusCode(202)
                            .putHeader("content-type", "text/plain")
                            .end("Accepted");
                }else{
                    req.response()
                            .setStatusCode(406)
                            .putHeader("content-type", "text/plain")
                            .end("Not accepted");
                }
            });
        });
    }

    public void saveService(JsonObject json) {
        connector.query(SQL_INSERT_SERVICE + json.getString("url") + "');").setHandler(done ->{
            if(done.succeeded()){
                System.out.println("Service "+ json.getString("url") + " saved successfully");
            }else{
                System.out.println("Service could not be saved in DB, cause: " + done.cause());
            }
        });
    }

    public Future<Boolean> fetchServices() {
        Future<Boolean> done = Future.future();
        connector.query(SQL_ALL_SERVICES).setHandler(res -> {
            if (res.succeeded()) {
                List<JsonObject> rows = res.result().getRows();
                for (JsonObject row : rows) {
                    services.put(row.getString("url"), "UNKNOWN");
                }
                done.complete(true);
            } else {
                System.out.println("Fetch services unsuccessfull, cause: " + res.cause());
                done.failed();
            }
        });
        return done;
    }

    public Future<Boolean> deleteService(JsonObject service){
        Future<Boolean> done = Future.future();
        services.remove(service.getString("url"));
        connector.query(SQL_DELETE_SERVICE+service.getString("url") + "';").setHandler(res ->{
            if (res.succeeded()) {
                System.out.println("Successfully deleted service " + service.getString("url"));
                done.complete(true);
            }else{
                System.out.println("Could not remove service " + service.getString("url") + ", cause: " + res.cause());
                done.failed();
            }
        });
        return done;
    }

}



