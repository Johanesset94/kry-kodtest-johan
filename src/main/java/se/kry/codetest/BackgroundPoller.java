package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class BackgroundPoller {
    private int size;
    private int handled;

    /**
     * Iterates list of json in parallell to poll each service.
     * If they return any code other than 200 or takes longer than 10 seconds the poll failed and status is set to FAIL,
     * otherwise OK if status returned is 200
     * @param services list of services formated as Json
     * @return Future that determines if poll was complete
     */
    public Future<List<String>> pollServices(List<JsonObject> services) {
        Future<List<String>> res = Future.future();

        size = services.size();
        handled = 0;
        for(JsonObject service: services){
            Thread t = new Thread(new Runnable(){
                @Override
                public void run() {
                    System.out.println("pinging " + service.getString("url"));
                    try {
                        URL url = new URL(service.getString("url"));
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        con.setConnectTimeout(10000); //This counts as protecting the poller from misbehaving services? :D
                        con.connect();
                        int status = con.getResponseCode();
                        if(status == 200){
                            System.out.println("Service checked and ok: " + service.getString("url"));
                            service.put("status", "OK");
                        }else{
                            throw new Exception();
                        }
                    }catch(Exception e){
                        System.out.println("Service not ok: " + service.getString("url"));
                        service.put("status", "FAIL");
                    }

                    if(size == ++handled){
                        res.complete();
                        System.out.println("Pollservice complete");
                    }
                }
            });
            t.start();

        }
        return res;
    }
}
