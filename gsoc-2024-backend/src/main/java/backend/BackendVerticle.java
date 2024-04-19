package backend;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class BackendVerticle extends AbstractVerticle {
  private WebClient client;

  private static final String JOKE_ENDPOINT = "http://icanhazdadjoke.com/";
  private static final String JOKE_ROUTE = "/joke";
  private static final String FAILED_JOKE_MESSAGE = "Failed to fetch joke: ";
  private static final String PLAIN_TEXT_CONTENT_TYPE = "text/plain";
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final int HTTP_OK_STATUS_CODE = 200;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new BackendVerticle())
      .onFailure(Throwable::printStackTrace)
      .onSuccess(v -> System.out.println("Deployed backend"));
  }

  public Future<String> getJoke() {
    return client.getAbs(JOKE_ENDPOINT)
      .putHeader("Accept", JSON_CONTENT_TYPE)
      .as(BodyCodec.jsonObject())
      .send()
      .compose(response -> response.statusCode() == HTTP_OK_STATUS_CODE ?
        io.vertx.core.Future.succeededFuture(response.body().getString("joke")) :
        io.vertx.core.Future.failedFuture(FAILED_JOKE_MESSAGE + response.statusMessage()));
  }

  @Override
  public void start(Promise<Void> startPromise) {
    client = WebClient.create(vertx);
    var server = vertx.createHttpServer();

    var router = Router.router(vertx);
    router.route(HttpMethod.GET, JOKE_ROUTE).handler(this::handleJokeRequest);

    server.requestHandler(router).listen(8080, result -> {
      if (result.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail(result.cause());
      }
    });
  }

  private void handleJokeRequest(RoutingContext ctx) {
    var response = ctx.response();
    response.putHeader("Accept", PLAIN_TEXT_CONTENT_TYPE);
    getJoke().onComplete(result -> {
      if (result.succeeded()) {
        response.end(result.result());
      } else {
        response.end(result.cause().getMessage());
      }
    });
  }
}
