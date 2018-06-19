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

    private static Logger LOGGER = Logger.getLogger(MetricsArchiveAppender.class.getName());

    @Override
    public Archive<?> createAuxiliaryArchive() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addPackages(true,
                MetricsHttpServlet.class.getPackage(),
                MetricCdiInjectionExtension.class.getPackage(),
                Metered.class.getPackage());
        war.addClass(MetricRegistry.Type.class);
        return war;
    }

}
