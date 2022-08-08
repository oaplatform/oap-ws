package oap.openapi.maven;

import com.google.common.base.Joiner;
import io.swagger.v3.core.converter.ModelConverters;

import oap.application.ApplicationException;
import oap.application.module.Module;
import oap.json.Binder;
import oap.ws.WsConfig;
import oap.ws.openapi.OpenapiGenerator;
import oap.ws.openapi.OpenapiGeneratorSettings;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * mvn oap:openapi-maven-plugin:0.0.1-SNAPSHOT:openapi
 */

@Mojo( name = "openapi", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class OpenApiGeneratorPlugin extends AbstractMojo {

    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;

    @Parameter( required = true, readonly = true )
    private String jsonOutputPath = "/Users/mac/IdeaProjects/oap-ws/oap-ws-openapi-ws/target/swagger.json";

    private final ModelConverters converters = new ModelConverters();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "OpenAPI generation..." );
        try {
            Objects.requireNonNull( jsonOutputPath );
            List<URL> moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
            getLog().info( "Configurations loaded" );

            OpenapiGeneratorSettings settings = OpenapiGeneratorSettings.builder().ignoreOpenapiWS( false ).build();
            OpenapiGenerator openapiGenerator = new OpenapiGenerator( "title", "", settings );
            List<String> description = new ArrayList<>();

            moduleConfigurations.forEach( url -> {
                Module config = Module.CONFIGURATION.fromFile( url, new HashMap<>() );
                config.services.forEach( ( name, service ) -> {
                    WsConfig wsService = ( WsConfig ) service.ext.get( "ws-service" );
                    if ( wsService == null ) {
                        getLog().debug( "Skipping non-WS module: " + name );
                        return;
                    }
                    try {
                        Class clazz = Class.forName( service.implementation );
                        openapiGenerator.processWebservice( clazz, wsService.path.stream().findFirst().orElse( "" ) );
                        getLog().info( "WebService class " + clazz.getCanonicalName() + " processed" );
                        description.add( clazz.getCanonicalName() );
                    } catch( ReflectiveOperationException e ) {
                        getLog().error( "Could not deal with module: " + name + " due to the implementation class '"
                            + service.implementation + "' is unavailable" );
                        throw new RuntimeException( e );
                    }
                } );
            } );
            openapiGenerator.setDescription( "WS services: " + Joiner.on( ", " ).join( description ) );
            String json = Binder.json.marshal( openapiGenerator.build() );
            getLog().info( "OpenAPI JSON generated" );
            IOUtils.write( json.getBytes( StandardCharsets.UTF_8 ), new FileOutputStream( jsonOutputPath ) );
            getLog().info( "OpenAPI JSON is written to " + jsonOutputPath );
        } catch( Exception e ) {
            throw new ApplicationException( e );
        }
    }
}
