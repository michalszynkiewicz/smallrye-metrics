package io.smallrye.metrics.tck.rest;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

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
        System.out.println("in metricsArchiveProcessor, testDeployment " + testDeployment);System.out.flush();
        Archive<?> archive = testDeployment.getArchiveForEnrichment();
        System.out.println("archiveForEnrichment " + archive);System.out.flush();
        System.out.println("protocol archive: " + protocolArchive);

        WebArchive war = (WebArchive)protocolArchive;
        war.addAsWebInfResource("WEB-INF/jboss-web.xml", "jboss-web.xml");
        String[] deps = {
                "org.jboss.weld.servlet:weld-servlet-core",
        };

        File[] dependencies = Maven.resolver().loadPomFromFile(new File("pom.xml")).resolve(deps).withTransitivity().asFile();

        war.addAsLibraries(dependencies);
    }
}
