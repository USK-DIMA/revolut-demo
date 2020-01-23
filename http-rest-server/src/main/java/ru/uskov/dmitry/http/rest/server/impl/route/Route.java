package ru.uskov.dmitry.http.rest.server.impl.route;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import ru.uskov.dmitry.http.rest.server.impl.exception.HttpRequestException;
import ru.uskov.dmitry.http.rest.server.utils.TemplateUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Route {

    private static final String PATH_SEPARATOR = "/";

    private final String path;
    private final Route parent;
    private final Map<String, Route> staticNexRoutes = new ConcurrentHashMap<>();
    private final AtomicReference<Route> templatedRoute = new AtomicReference<>();
    private final Map<String, RequestExecutor> executorsByMethod = new ConcurrentHashMap<>();

    public Route() {
        this.path = "/";
        this.parent = null;
    }

    public Route(Route parent, String path) {
        this.parent = parent;
        this.path = path;
    }

    public void register(@NotNull RequestExecutor executor, @NotNull String path, @NotNull String method) {
        String[] split = path.split("/");
        if (split.length > 0 && split[0].equals("")) {
            split = Arrays.copyOfRange(split, 1, split.length);
        }
        register(executor, split, method);
    }

    private void register(@NotNull RequestExecutor executor, @NotNull String[] splitPath, @NotNull String method) {
        if (splitPath.length == 0) {
            registerExecutor(executor, method);
            return;
        }
        String nextPath = splitPath[0];
        Route route;

        if (isTemplate(nextPath)) {
            route = getOrCreateTemplateRoute(nextPath);
        } else {
            route = getOrCreateStaticRoute(nextPath);
        }

        route.register(executor, Arrays.copyOfRange(splitPath, 1, splitPath.length), method);
    }

    private Route getOrCreateStaticRoute(String nextPath) {
        Route route = new Route(this, nextPath);
        Route fromMap = staticNexRoutes.putIfAbsent(nextPath, route);
        if (fromMap != null) {
            route = fromMap;
        }
        return route;
    }

    private Route getOrCreateTemplateRoute(String nextPath) {
        Route route = new Route(this, nextPath);
        boolean isSet = templatedRoute.compareAndSet(null, route);
        if (!isSet) {
            route = templatedRoute.get();
        }
        return route;
    }

    private boolean isTemplate(String path) {
        if (TemplateUtils.TEMPLATE_PATTERN_FINDER.matcher(path).find()) {
            if (!TemplateUtils.TEMPLATE_PATTERN_VALIDATOR.matcher(path).find()) {
                throw new IllegalArgumentException("Invalid template path '" + path + "'");
            }
            return true;
        }
        return false;
    }

    private void registerExecutor(RequestExecutor executor, String method) {
        RequestExecutor res = executorsByMethod.putIfAbsent(method, executor);
        if (res != null) {
            throw new IllegalArgumentException("Duplicate path '" + getPath() + "'. Http Method: " + method);
        }
    }

    public void execute(HttpExchange exchange) throws Exception {
        String[] split = exchange.getRequestURI().getPath().split(PATH_SEPARATOR);
        if (split.length > 0 && split[0].equals("")) {
            split = Arrays.copyOfRange(split, 1, split.length);
        }
        boolean executed = execute(exchange, split);
        if (!executed) {
            throw new HttpRequestException(404, "Not Found");
        }
    }

    private boolean execute(HttpExchange exchange, String[] split) throws Exception {
        if (split.length == 0) {
            RequestExecutor requestExecutor = executorsByMethod.get(exchange.getRequestMethod());
            if (requestExecutor == null) {
                throw new HttpRequestException(404, "Not Found");
            }
            requestExecutor.execute(exchange);
            return true;
        }

        boolean executed = false;

        Route route = staticNexRoutes.get(split[0]);
        if (route != null) {
            executed = route.execute(exchange, Arrays.copyOfRange(split, 1, split.length));
        }
        if (!executed) {
            Route templRoute = templatedRoute.get();
            if (templRoute != null) {
                executed = templRoute.execute(exchange, Arrays.copyOfRange(split, 1, split.length));
            }
        }

        return executed;
    }

    private String getPath() {
        String result;
        if (parent != null) {
            result = parent.getPath();
            if (!result.equals(PATH_SEPARATOR)) {
                result = PATH_SEPARATOR;
            }
            result += path;
        } else {
            result = PATH_SEPARATOR + path;
        }
        return result;
    }

}
