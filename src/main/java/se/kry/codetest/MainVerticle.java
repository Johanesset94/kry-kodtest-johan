package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    //private HashMap<String, String> services = new HashMap<>();
    private List<JsonObject> servicesJson = new ArrayList<>();
    private DBConnector connector;
    private BackgroundPoller poller = new BackgroundPoller();

    private String SQL_ALL_SERVICES = "SELECT * FROM service";
    private String SQL_INSERT_SERVICE = "INSERT INTO service (url, date) VALUES('";
    private String SQL_DELETE_SERVICE = "DELETE FROM service WHERE url = '";

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        //services.put("https://www.kry.se", "UNKNOWN");
        //saveService(new JsonObject().put("url", "https://www.kry.se"));
        fetchServices();
        vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(servicesJson));
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
                req.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonArray(servicesJson).encode());
            });
        });
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            saveService(jsonBody);
            //servicesJson.put(jsonBody.getString("url"), "UNKNOWN");
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
        router.delete("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            deleteService(jsonBody).setHandler(done -> {
                if (done.succeeded()) {
                    req.response()
                            .setStatusCode(202)
                            .putHeader("content-type", "text/plain")
                            .end("Accepted");
                } else {
                    req.response()
                            .setStatusCode(406)
                            .putHeader("content-type", "text/plain")
                            .end("Not accepted");
                }
            });
        });
        router.put("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            updateService(jsonBody).setHandler(done -> {
                if (done.succeeded()) {
                    req.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "text/plain")
                            .end("Accepted");
                } else {
                    req.response()
                            .setStatusCode(406)
                            .putHeader("content-type", "text/plain")
                            .end("Not accepted");
                }
            });

        });
    }

    /**
     * Saves a service with provided url to the database and saves the date it was added
     * @param json json containing url as a string
     */
    private void saveService(JsonObject json) {
        // Naive attempt to prevent SQL-injections by escaping ; chars
        String url = json.getString("url").replace(";", "");
        connector.query(SQL_INSERT_SERVICE + url + "', datetime('now', 'localtime'));").setHandler(done -> {
            if (done.succeeded()) {
                System.out.println("Service " + url + " saved successfully");
            } else {
                System.out.println("Service could not be saved in DB, cause: " + done.cause());
            }
        });
    }

    /**
     * Fetches services from database and puts them into servicesJson list
     * @return future to determine operation done
     */
    private Future<Boolean> fetchServices() {
        Future<Boolean> done = Future.future();
        connector.query(SQL_ALL_SERVICES).setHandler(res -> {
            if (res.succeeded()) {
                List<JsonObject> rows = res.result().getRows();
                // Do not overwrite already known services
                List<String> urls = servicesJson
                        .stream()
                        .map(service -> service.getString("url"))
                        .collect(Collectors.toList());

                for (JsonObject row : rows) {
                    row.put("status", "UNKNOWN");
                    if (!urls.contains(row.getString("url"))) {
                        servicesJson.add(row);
                    }
                    //System.out.println(row);
                }
                done.complete(true);
            } else {
                System.out.println("Fetch services unsuccessfull, cause: " + res.cause());
                done.failed();
            }
        });
        return done;
    }

    /**
     * Deletes a service from local list and database
     * @param service containing url of service to be deleted
     * @return future to determine operation done
     */
    private Future<Boolean> deleteService(JsonObject service) {
        Future<Boolean> done = Future.future();
        // Naive attempt to prevent SQL-injections by escaping ; chars
        String url = service.getString("url").replace(";", "");
        // Delete service from local list
        for (JsonObject s : servicesJson) {
            if (s.getString("url").equals(service.getString("url"))) {
                servicesJson.remove(s);
                break;
            }
        }
        // Delete from db
        connector.query(SQL_DELETE_SERVICE + url + "';").setHandler(res -> {
            if (res.succeeded()) {
                System.out.println("Successfully deleted service " + url);
                done.complete(true);
            } else {
                System.out.println("Could not remove service " + url + ", cause: " + res.cause());
                done.failed();
            }
        });
        return done;
    }

    /**
     * Updates the name of a service in local list and database.
     * Could quite easily be extended to change multiple parameters since everything is included in input
     * @param service as a json object
     * @return future to determine operation done
     */
    private Future<Boolean> updateService(JsonObject service) {
        Future<Boolean> done = Future.future();
        // Naive attempt to prevent SQL-injections by escaping ; chars
        String name = service.getString("name").replace(";", "");
        String url =  service.getString("url").replace(";", "");

        for (JsonObject s : servicesJson) {
            if (s.getString("url").equals(url)) {
                s.put("name", service.getString("name"));
                break;
            }
        }
        connector.query("UPDATE service " +
                "SET name = '" + name + "' " +
                "WHERE url = '" + url + "';")
                .setHandler(res -> {
                    if (res.succeeded()) {
                        System.out.println("Successfully updated service " + url);
                        done.complete(true);
                    } else {
                        System.out.println("Could not update service " + url + ", cause: " + res.cause());
                        done.failed();
                    }
                });
        return done;
    }
}



