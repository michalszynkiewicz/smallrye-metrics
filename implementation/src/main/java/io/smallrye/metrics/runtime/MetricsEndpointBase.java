package io.smallrye.metrics.runtime;

import io.smallrye.metrics.runtime.exporters.Exporter;
import io.smallrye.metrics.runtime.exporters.JsonExporter;
import io.smallrye.metrics.runtime.exporters.JsonMetadataExporter;
import io.smallrye.metrics.runtime.exporters.PrometheusExporter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/25/18
 */
@ApplicationScoped
public class MetricsEndpointBase {

    private static final Map<String, String> corsHeaders;

    static {
        corsHeaders = new HashMap<>();
        corsHeaders.put("Access-Control-Allow-Origin", "*");
        corsHeaders.put("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        corsHeaders.put("Access-Control-Allow-Credentials", "true");
        corsHeaders.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

    public void handleRequest(String requestPath,
                              String method,
                              Stream<String> acceptHeaders,
                              Responder responder) throws IOException {
        Exporter exporter = obtainExporter(method, acceptHeaders);
        if (exporter == null) {
            responder.respondWith(406, "No exporter found for method " + method + " and media type", Collections.emptyMap());
            return;
        }

        String scopePath = requestPath.substring(8);
        if (scopePath.startsWith("/")) {
            scopePath = scopePath.substring(1);
        }
        if (scopePath.endsWith("/")) {
            scopePath = scopePath.substring(0, scopePath.length() - 1);
        }

        StringBuffer sb;
        if (scopePath.isEmpty()) {
            // All metrics
            sb = exporter.exportAllScopes();

        } else if (scopePath.contains("/")) {
            // One metric in a scope

            String attribute = scopePath.substring(scopePath.indexOf('/') + 1);

            MetricRegistry.Type scope = getScopeFromPath(responder, scopePath.substring(0, scopePath.indexOf('/')));
            if (scope == null) {
                responder.respondWith(404, "Scope " + scopePath + " not found", Collections.emptyMap());
                return;
            }

            MetricRegistry registry = registries.get(scope);
            Map<String, Metric> metricValuesMap = registry.getMetrics();

            if (metricValuesMap.containsKey(attribute)) {
                sb = exporter.exportOneMetric(scope, attribute);
            } else {
                responder.respondWith( 404, "Metric " + scopePath + " not found", Collections.emptyMap());
                return;
            }
        } else {
            // A single scope

            MetricRegistry.Type scope = getScopeFromPath(responder, scopePath);
            if (scope == null) {
                responder.respondWith( 404, "Scope " + scopePath + " not found", Collections.emptyMap());
                return;
            }

            MetricRegistry reg = registries.get(scope);
            if (reg.getMetadata().size() == 0) {
                responder.respondWith( 204, "No data in scope " + scopePath, Collections.emptyMap());
            }

            sb = exporter.exportOneScope(scope);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", exporter.getContentType());
        headers.put("Access-Control-Max-Age", "1209600");
        headers.putAll(corsHeaders);

        responder.respondWith(200, sb.toString(), headers);

    }

    private MetricRegistry.Type getScopeFromPath(Responder responder, String scopePath) throws IOException {
        MetricRegistry.Type scope;
        try {
            scope = MetricRegistry.Type.valueOf(scopePath.toUpperCase());
        } catch (IllegalArgumentException iae) {
            responder.respondWith(404, "Bad scope requested: " + scopePath, Collections.emptyMap());
            return null;
        }
        return scope;
    }


    /**
     * Determine which exporter we want.
     *
     * @param exchange The http exchange coming in
     * @return An exporter instance or null in case no matching exporter existed.
     */
    private Exporter obtainExporter(String method, Stream<String> acceptHeaders) {
        Exporter exporter;

        if (acceptHeaders == null) {
            if (method.equals("GET")) {
                exporter = new PrometheusExporter(registries);
            } else {
                return null;
            }
        } else {
            // Header can look like "application/json, text/plain, */*"
            // mstodo simplify below
            if (acceptHeaders.findFirst().map(e -> e.startsWith("application/json")).orElse(false)) {


                if (method.equals("GET")) {
                    exporter = new JsonExporter(registries);
                } else if (method.equals("OPTIONS")) {
                    exporter = new JsonMetadataExporter(registries);
                } else {
                    return null;
                }
            } else {
                // This is the fallback, but only for GET, as Prometheus does not support OPTIONS
                if (method.equals("GET")) {
                    exporter = new PrometheusExporter(registries);
                } else {
                    return null;
                }
            }
        };
        return exporter;
    }

    @Inject
    private MetricRegistries registries;


    public interface Responder {
        void respondWith(int status, String message, Map<String, String> headers) throws IOException;
    }
}
