Having a sub-router attached to the main router will attach the routers again at metrics.

GIVEN:
```
vertx_http_server_response_time_seconds_max{code="200",method="GET",route="/first/>/good",}
vertx_http_server_response_time_seconds_max{code="500",method="GET",route="/first/>/bad>/first/",}
vertx_http_server_response_time_seconds_max{code="404",method="GET",route="/first/>/notfound>/first/",}

vertx_http_server_response_time_seconds_max{code="200",method="GET",route="/first/>/second/>/good",}
vertx_http_server_response_time_seconds_max{code="500",method="GET",route="/first/>/second/>/bad>/first/>/second/",}
vertx_http_server_response_time_seconds_max{code="404",method="GET",route="/first/>/second/>/notfound>/first/>/second/",}
```

EXPECTED:
```
vertx_http_server_response_time_seconds_max{code="200",method="GET",route="/first/>/good",}
vertx_http_server_response_time_seconds_max{code="500",method="GET",route="/first/>/bad",}
vertx_http_server_response_time_seconds_max{code="404",method="GET",route="/first/>/notfound",}

vertx_http_server_response_time_seconds_max{code="200",method="GET",route="/first/>/second/>/good",}
vertx_http_server_response_time_seconds_max{code="500",method="GET",route="/first/>/second/>/bad",}
vertx_http_server_response_time_seconds_max{code="404",method="GET",route="/first/>/second/>/notfound",}
```
