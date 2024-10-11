package spring.academy.restful.hsqldb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class HsqldbServer {

    @Value("classpath:/devdb/hsqldb.properties")
    private Resource hsqldbProperties;

    /**
     * Starts hsqldb in server mode. We can do the same from the command line:
     * $ java -cp $MAVEN_REPO_HOME/org/hsqldb/hsqldb/2.5.2/hsqldb-2.5.2.jar org.hsqldb.server.Server --database.0 mem:rewards --dbname.0 rewards
     * Hsqldb server will be running in a different process though.
     */
    @Bean
    public Server hsqlServer() {
        try {
            Server server = new Server();
            server.setProperties(PropertiesLoaderUtils.loadProperties(hsqldbProperties));
            // With this you should see the startup sequence messages in the console
            server.setLogWriter(new PrintWriter(System.out));
            server.setErrWriter(new PrintWriter(System.out));
            return server;
        } catch (IOException | ServerAcl.AclFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void start() {
        hsqlServer().start();
    }

    @PreDestroy
    public void stop() {
        hsqlServer().stop();
    }
}
