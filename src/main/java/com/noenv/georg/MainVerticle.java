package com.noenv.georg;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;

public class MainVerticle extends AbstractVerticle {
  public static final int SERVER_PORT = 7777;
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  private static final String SERVER_HOST = "localhost";

  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private HttpServer server;

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)
    ));

    vertx.createHttpServer()
      .requestHandler(createRouter())
      .listen(SERVER_PORT)
      .onSuccess(server -> {
        this.server = server;
        logger.info("http://%s:%d/metrics".formatted(SERVER_HOST, SERVER_PORT));

        logger.info("http://%s:%d/good".formatted(SERVER_HOST, SERVER_PORT));
        logger.info("http://%s:%d/bad".formatted(SERVER_HOST, SERVER_PORT));
        logger.info("http://%s:%d/notfound".formatted(SERVER_HOST, SERVER_PORT));

        logger.info("http://%s:%d/first/good".formatted(SERVER_HOST, SERVER_PORT));
        logger.info("http://%s:%d/first/bad".formatted(SERVER_HOST, SERVER_PORT));
        logger.info("http://%s:%d/first/notfound".formatted(SERVER_HOST, SERVER_PORT));

        logger.info("http://%s:%d/first/second/good".formatted(SERVER_HOST, SERVER_PORT));
        logger.info("http://%s:%d/first/second/bad".formatted(SERVER_HOST, SERVER_PORT));
        logger.info("http://%s:%d/first/second/notfound".formatted(SERVER_HOST, SERVER_PORT));
        startPromise.complete();
      })
      .onFailure(ex -> startPromise.fail(ex.getCause()));
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    server.close()
      .onSuccess(stopPromise::complete)
      .onFailure(stopPromise::fail);
  }

  private Router createRouter() {
    Router router = getRouter();
    router.route().handler(ctx -> {
      logger.info("Request matched path: " + ctx.request().path());
      ctx.next();
    });
    router.route("/metrics").handler(PrometheusScrapingHandler.create());
    Router firstSub = getRouter();
    firstSub.route("/second/*").subRouter(getRouter());
    router.route("/first/*").subRouter(firstSub);
    return router;
  }

  private Router getRouter() {
    Router router = Router.router(vertx);
    router.route("/good").handler(ctx -> ctx.response().end("200 good"));
    router.route("/bad").handler(ctx -> ctx.fail(500));
    router.route("/notfound").handler(ctx -> ctx.fail(404));
    return router;
  }

}
