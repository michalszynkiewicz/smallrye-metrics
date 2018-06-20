package io.smallrye.metrics.tck.rest;

import io.smallrye.metrics.deployment.MetricCdiInjectionExtension;
import io.smallrye.metrics.runtime.MetricsHttpServlet;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.util.logging.Logger;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/18/18
 */
public class MetricsArchiveAppender implements AuxiliaryArchiveAppender {
    // mstodo remove

    private static Logger LOGGER = Logger.getLogger(MetricsArchiveAppender.class.getName());

    @Override
    public Archive<?> createAuxiliaryArchive() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackages(true,
                MetricsHttpServlet.class.getPackage(),
                MetricCdiInjectionExtension.class.getPackage(),
                Metered.class.getPackage());
        jar.addClass(MetricRegistry.Type.class);
        jar.addAsResource("META-INF/beans.xml", "META-INF/beans.xml");

        return jar;
    }

}
