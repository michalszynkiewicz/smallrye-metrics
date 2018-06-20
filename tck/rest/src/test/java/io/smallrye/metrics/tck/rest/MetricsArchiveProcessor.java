package io.smallrye.metrics.tck.rest;

import io.smallrye.config.SmallRyeConfigProviderResolver;
import io.smallrye.config.inject.ConfigExtension;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.weld.environment.deployment.discovery.BeanArchiveHandler;

import javax.enterprise.inject.spi.Extension;
import java.io.File;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/19/18
 */
public class MetricsArchiveProcessor implements ProtocolArchiveProcessor {
    @Override
    public void process(TestDeployment testDeployment, Archive<?> protocolArchive) {
        WebArchive war = (WebArchive)protocolArchive;
        war.addAsWebInfResource("WEB-INF/jboss-web.xml", "jboss-web.xml");
        String[] deps = {
                "io.smallrye:smallrye-config",
                "io.smallrye:smallrye-metrics",
                "org.jboss.weld.servlet:weld-servlet",
                "org.yaml:snakeyaml",
        };

        File[] dependencies = Maven.resolver().loadPomFromFile(new File("pom.xml")).resolve(deps).withTransitivity().asFile();

        war.addAsLibraries(dependencies);

        war.addClass(SmallRyeBeanArchiveHandler.class);
        war.addClass(MetricsService.class);
        war.addAsResource("mapping.yml", "mapping.yml");
        war.addAsServiceProvider(BeanArchiveHandler.class, SmallRyeBeanArchiveHandler.class);
        war.addAsServiceProvider(Extension.class, ConfigExtension.class);
        war.addAsServiceProvider(ConfigProviderResolver.class, SmallRyeConfigProviderResolver.class);

//        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
//        jar.addAsResource("META-INF/beans.xml", "META-INF/beans.xml");
    }
}
