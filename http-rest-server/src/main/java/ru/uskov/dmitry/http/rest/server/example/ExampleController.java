package ru.uskov.dmitry.http.rest.server.example;

import ru.uskov.dmitry.http.rest.server.api.*;
import ru.uskov.dmitry.http.rest.server.api.annotation.*;

import java.util.Map;

@Path(value = "/")
public class ExampleController {


    /**
     * you can get HttpRequestExchange for method but
     * @param hello
     * @param param1
     */
    @Path(value = "", method = HttpMethod.GET)
    public void method(
            @Body String hello,
            @QueryParam String param1,
            @QueryParams Map<String, String> params,
            @PathVariable String name
    ) {

    }


    @Path(value = "", method = HttpMethod.GET)
    public void testQueryParams(
            @QueryParams Map<String, Object> param
    ) {

    }

    @Path(value = "", method = HttpMethod.GET)
    public void testMoreThanOneAnnotation(
            @Body
            @QueryParams String param
            ) {

    }
}
