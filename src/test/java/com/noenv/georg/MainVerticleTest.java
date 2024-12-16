package com.noenv.georg;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.micrometer.backends.BackendRegistries;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private Vertx vertx;
  private MeterRegistry registry;

  private record CompareMetric(String key, String code, String metricPath) {

  }

  @Before
  public void before(final TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class, new DeploymentOptions())
      .onSuccess(x -> {
        registry = BackendRegistries.getDefaultNow();
        registry.clear();
      })
      .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void after(final TestContext context) {
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void rootLevelMetrics(final TestContext ctx) {
    call(ctx, "/good", 200, "/good");
    call(ctx, "/bad", 500, "/bad");
    call(ctx, "/notfound", 404, "/notfound");
  }

  @Test
  public void firstLevelMetrics(final TestContext ctx) {
    call(ctx, "/first/good", 200, "/first/>/good");
    call(ctx, "/first/bad", 500, "/first/>/bad");
    call(ctx, "/first/notfound", 404, "/first/>/notfound");
  }

  @Test
  public void secondLevelMetrics(final TestContext ctx) {
    call(ctx, "/first/second/good", 200, "/first/>/second/>/good");
    call(ctx, "/first/second/bad", 500, "/first/>/second/>/bad");
    call(ctx, "/first/second/notfound", 404, "/first/>/second/>/notfound");
  }

  private CompareMetric getMetric(int code) {
    return registry.getMeters().stream()
      .filter(meter ->
        Objects.equals(meter.getId().getName(), "vertx.http.server.response.time") &&
          Objects.equals(meter.getId().getTag("code"), String.valueOf(code))
      )
      .map(m -> new CompareMetric(m.getId().getName(), m.getId().getTag("code"), m.getId().getTag("route")))
      .findFirst().orElseThrow();
  }

  private void call(TestContext ctx, String path, int statusCode, String expectedMetricPath) {
    client(path)
      .flatMap(HttpClientRequest::send)
      .onSuccess(response -> ctx.assertEquals(statusCode, response.statusCode()))
      .flatMap(HttpClientResponse::body)
      .onSuccess(x -> ctx.assertEquals(expectedMetricPath, getMetric(statusCode).metricPath()))
      .onComplete(ctx.asyncAssertSuccess());
  }

  private Future<HttpClientRequest> client(String path) {
    return vertx.createHttpClient(new HttpClientOptions())
      .request(new RequestOptions()
        .setMethod(HttpMethod.POST)
        .setPort(MainVerticle.SERVER_PORT)
        .setURI(path)
      );
  }

}
