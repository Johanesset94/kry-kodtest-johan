package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.migrate.DBMigration;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private HashMap<String, String> services = new HashMap<>();
    //TODO use this
    private DBConnector connector;
    private BackgroundPoller poller = new BackgroundPoller();

    private String SQL_ALL_SERVICES = "SELECT * FROM service";
    private String SQL_INSERT_SERVICE = "INSERT INTO service (\n url) VALUES('";

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        services.put("https://www.kry.se", "UNKNOWN");

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
        Future<ResultSet> resp = connector.query(SQL_ALL_SERVICES);
        resp.setHandler(res -> {
            if (res.succeeded()) {
                List<JsonObject> rows = res.result().getRows();
                System.out.println(rows);
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

}



