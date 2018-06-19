package io.smallrye.metrics.tck.rest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import org.eclipse.microprofile.metrics.test.MetricAppBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

import static com.jayway.restassured.RestAssured.given;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 6/19/18
 */
@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(Arquillian.class)
public class MyTest {


    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;

    @Inject
    private MetricAppBean metricAppBean;

    @BeforeClass
    static public void setup() throws MalformedURLException {
        // set base URI and port number to use for all requests
        String serverUrl = System.getProperty("test.url");
        String protocol = DEFAULT_PROTOCOL;
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverUrl != null) {
            URL url = new URL(serverUrl);
            protocol = url.getProtocol();
            host = url.getHost();
            port = (url.getPort() == -1) ? DEFAULT_PORT : url.getPort();
        }

        RestAssured.baseURI = protocol + "://" + host;
        RestAssured.port = port;

        // set user name and password to use for basic authentication for all requests
        String userName = System.getProperty("test.user");
        String password = System.getProperty("test.pwd");

        if (userName != null && password != null) {
            RestAssured.authentication = RestAssured.basic(userName, password);
            RestAssured.useRelaxedHTTPSValidation();
        }

        System.out.println("n\n\n\n" + RestAssured.baseURI + RestAssured.basePath + " \n\n\n\n");
    }

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class).addClass(MetricAppBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        System.out.println(jar.toString(true));
        return jar;
    }
    
    @Test
    @RunAsClient
    public void testValidUrl() {
        Header acceptHeader = new Header("Accept", APPLICATION_JSON);

        given().header(acceptHeader).when().get("/metrics").then().statusCode(200).and().contentType(APPLICATION_JSON);
    }
}

