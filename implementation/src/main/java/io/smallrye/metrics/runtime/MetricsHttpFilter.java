/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.smallrye.metrics.runtime;

import io.smallrye.metrics.runtime.exporters.Exporter;
import io.smallrye.metrics.runtime.exporters.JsonExporter;
import io.smallrye.metrics.runtime.exporters.JsonMetadataExporter;
import io.smallrye.metrics.runtime.exporters.PrometheusExporter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author hrupp
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * // mstodo register this thing
 * // mstodo: switch to servlet?
 */
@WebFilter(filterName="MP-metrics-filter",servletNames={"MicroProfile Metrics Filter"}, urlPatterns = "/")
public class MetricsHttpFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)
                || !(servletResponse instanceof HttpServletResponse)) {
            throw new RuntimeException(getClass().getName() + " works only for HttpServletRequest and HttServletResponse");
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestPath = request.getRequestURI();

        if (!requestPath.startsWith("/metrics")) {
            chain.doFilter(request, response);
            return;
        }

        // request is for us, so let's handle it

        Exporter exporter = obtainExporter(request);
        if (exporter == null) {
            respondWith(response, 406, "No exporter found for method " + request.getMethod() + " and media type");
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

            MetricRegistry.Type scope = getScopeFromPath(response, scopePath.substring(0, scopePath.indexOf('/')));
            if (scope == null) {
                respondWith(response, 404, "Scope " + scopePath + " not found");
                return;
            }

            MetricRegistry registry = registries.get(scope);
            Map<String, Metric> metricValuesMap = registry.getMetrics();

            if (metricValuesMap.containsKey(attribute)) {
                sb = exporter.exportOneMetric(scope, attribute);
            } else {
                respondWith(response, 404, "Metric " + scopePath + " not found");
                return;
            }
        } else {
            // A single scope

            MetricRegistry.Type scope = getScopeFromPath(response, scopePath);
            if (scope == null) {
                respondWith(response, 404, "Scope " + scopePath + " not found");
                return;
            }

            MetricRegistry reg = registries.get(scope);
            if (reg.getMetadata().size() == 0) {
                respondWith(response, 204, "No data in scope " + scopePath);
            }

            sb = exporter.exportOneScope(scope);
        }

        response.addHeader("Content-Type", exporter.getContentType());
        provideCorsHeaders(response);
        response.addHeader("Access-Control-Max-Age", "1209600");
        response.getWriter().write(sb.toString());

    }

    private void respondWith(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(message);
    }

    private void provideCorsHeaders(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

    private MetricRegistry.Type getScopeFromPath(HttpServletResponse response, String scopePath) throws IOException {
        MetricRegistry.Type scope;
        try {
            scope = MetricRegistry.Type.valueOf(scopePath.toUpperCase());
        } catch (IllegalArgumentException iae) {
            respondWith(response, 404, "Bad scope requested: " + scopePath);
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
    private Exporter obtainExporter(HttpServletRequest request) {
        Enumeration<String> acceptHeaders = request.getHeaders("Accept");
        Exporter exporter;

        String method = request.getMethod();

        if (acceptHeaders == null) {
            if (method.equals("GET")) {
                exporter = new PrometheusExporter();
            } else {
                return null;
            }
        } else {
            // Header can look like "application/json, text/plain, */*"
            if (acceptHeaders.hasMoreElements() && acceptHeaders.nextElement().startsWith("application/json")) {


                if (method.equals("GET")) {
                    exporter = new JsonExporter();
                } else if (method.equals("OPTIONS")) {
                    exporter = new JsonMetadataExporter();
                } else {
                    return null;
                }
            } else {
                // This is the fallback, but only for GET, as Prometheus does not support OPTIONS
                if (method.equals("GET")) {
                    exporter = new PrometheusExporter();
                } else {
                    return null;
                }
            }
        }
        return exporter;
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("initializing filter\n\n\n\n\n\n");
    }

    @Override
    public void destroy() {
        // TODO: Customise this generated block
    }

    @Inject
    private MetricRegistries registries;
}
