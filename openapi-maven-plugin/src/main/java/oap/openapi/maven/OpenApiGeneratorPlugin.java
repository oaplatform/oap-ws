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

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * mvn oap:openapi-maven-plugin:0.0.1-SNAPSHOT:openapi
 */

@Mojo( name = "openapi", defaultPhase = LifecyclePhase.TEST )
public class OpenApiGeneratorPlugin extends AbstractMojo {

    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    private MavenProject project;
    @Parameter( property = "project.compileClasspathElements", required = true, readonly = true )
    private List<String> classpath;

    @Parameter( required = true, readonly = true )
    private String jsonOutputPath = "/Users/mac/IdeaProjects/oap-ws/openapi-maven-plugin/target/swagger.json";

    private final ModelConverters converters = new ModelConverters();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "OpenAPI generation..." );
        try {
            Objects.requireNonNull( jsonOutputPath );
            List<URL> moduleConfigurations = Module.CONFIGURATION.urlsFromClassPath();
            if ( classpath != null && !classpath.isEmpty() ) {
                moduleConfigurations.add( new File( classpath.get( 0 ) + "/META-INF/oap-module.conf" ).toURI().toURL() );
            }
            getLog().info( "Configurations loaded: " + moduleConfigurations );

            OpenapiGeneratorSettings settings = OpenapiGeneratorSettings.builder().ignoreOpenapiWS( false ).build();
            OpenapiGenerator openapiGenerator = new OpenapiGenerator( "title", "", settings );
            List<String> description = new ArrayList<>();

            moduleConfigurations.forEach( url -> {
                getLog().info( "Reading config from " + url.getPath() );
                Module config = Module.CONFIGURATION.fromFile( url, new HashMap<>() );
                config.services.forEach( ( name, service ) -> {
                    WsConfig wsService = ( WsConfig ) service.ext.get( "ws-service" );
                    if ( wsService == null ) {
                        getLog().debug( "Skipping non-WS module: " + name );
                        return;
                    }
                    getLog().info( "WS service: " + name + " implementing " + service.implementation );
                    try {
                        Class clazz;
                        if ( classpath != null ) {
                            List<URL> cps = classpath
                                .stream()
                                .map( cp -> {
                                    try {
                                        return new File( classpath.get( 0 ) ).toURI().toURL();
                                    } catch( MalformedURLException e ) {
                                        throw new RuntimeException( e );
                                    }
                                } )
                                .collect( Collectors.toList() );
                            getLog().debug( "Using classpaths: " + cps );
                            URLClassLoader urlClassLoader = new URLClassLoader( cps.toArray( new URL[0] ) );
                            clazz = urlClassLoader.loadClass( service.implementation );
                        } else {
                            clazz = Class.forName( service.implementation );
                        }
                        getLog().info( "WebService class: " + clazz.getCanonicalName() + " processing..." );
                        openapiGenerator.processWebservice( clazz, wsService.path.stream().findFirst().orElse( "" ) );
                        getLog().info( "WebService class " + clazz.getCanonicalName() + " processed" );
                        description.add( clazz.getCanonicalName() );
                    } catch( Exception e ) {
                        getLog().warn( "Could not deal with module: " + name + " due to the implementation class '"
                            + service.implementation + "' is unavailable", e );
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
