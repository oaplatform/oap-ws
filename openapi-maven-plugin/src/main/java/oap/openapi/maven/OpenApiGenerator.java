package oap.openapi.maven;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;

import oap.application.ApplicationConfiguration;
import oap.application.ApplicationException;
import oap.application.module.Module;
import oap.ws.openapi.OpenapiGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * mvn oap:openapi-maven-plugin:0.0.1-SNAPSHOT:openapi
 */

@Mojo( name = "openapi", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class OpenApiGenerator extends AbstractMojo {

    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;

    private String appConfigPath = "/Users/mac/IdeaProjects/oap-ws/oap-ws-openapi-ws/target/test-classes/application.test.conf";

    private final ModelConverters converters = new ModelConverters();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info( "OpenAPI generation..." );
        try {
            URL configURL = appConfigPath.startsWith( "classpath:" )
                ? Thread.currentThread().getContextClassLoader().getResource( appConfigPath.substring( "classpath:".length() ) )
                : new File( appConfigPath ).toURI().toURL();
            Path confdPath = new File( configURL.toURI() ).toPath().getParent().resolve( "conf.d" );

            List<URL> moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();

            getLog().info( "Configurations loaded." );

            OpenapiGenerator openapiGenerator = new OpenapiGenerator( "title", "description" );

            moduleConfigurations.forEach( url -> {
                ApplicationConfiguration config = ApplicationConfiguration.load( url, confdPath.toString() );
                config.services.forEach( (name, service) -> {
                    Map<String, String> wsService = ( Map<String, String> ) service.get( "ws-service" );
                    if ( wsService == null) return;
                    try {
                        Class clazz = Class.forName( (String) service.get( "implementation" ) );
                        openapiGenerator.processWebservice( clazz, wsService.get( "path" ) );
                        getLog().info( clazz.getCanonicalName() + " processed." );
                    } catch( ReflectiveOperationException e ) {
                        throw new RuntimeException( e );
                    }
                } );
            } );
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL);
            String json = mapper.writeValueAsString( openapiGenerator.build() );
            System.err.println( json );
        } catch( Exception e ) {
            throw new ApplicationException( e );
        }
    }
}
