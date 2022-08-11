package oap.openapi.maven;

import com.google.common.base.Joiner;
import io.swagger.v3.core.util.Yaml;
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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
/**
 *
 * mvn oap:openapi-maven-plugin:0.0.7-SNAPSHOT:openapi
 */

@Mojo(
    name = "openapi",
    defaultPhase = LifecyclePhase.COMPILE,
    configurator = "include-project-dependencies",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class OpenApiGeneratorPlugin extends AbstractMojo {

    @Parameter( property = "project.compileClasspathElements", required = true, readonly = true )
    private List<String> classpath;
    @Component
    private PluginDescriptor pluginDescriptor;
    @Parameter( required = true, readonly = true, defaultValue = "swagger" )
    private String outputPath;

    @Parameter( required = true, readonly = true, defaultValue = "JSON" )
    private String outputType;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info( "OpenAPI generation..." );
        try {
            Objects.requireNonNull( outputPath );
            LinkedHashSet<String> moduleConfigurations = new LinkedHashSet<>(  );

            List<URL> urls = new ArrayList<>( Module.CONFIGURATION.urlsFromClassPath()
                .stream()
                .filter( url -> moduleConfigurations.add( url.toString() ) )
                .toList() );
            if ( classpath != null && !classpath.isEmpty() ) {
                outputPath = classpath.get( 0 ) + "/swagger";
                URL currentModuleUrl = new File( classpath.get( 0 ) + "/META-INF/oap-module.conf" ).toURI().toURL();
                if ( moduleConfigurations.add( currentModuleUrl.toString() ) ) {
                    urls.add( currentModuleUrl );
                }
            }
            getLog().info( "Configurations (from oap-module.conf files) loaded: " + moduleConfigurations );

            OpenapiGeneratorSettings settings = OpenapiGeneratorSettings
                .builder()
                .processOnlyAnnotatedMethods( false )
                .outputType( OpenapiGeneratorSettings.Type.valueOf( outputType ) )
                .build();
            OpenapiGenerator openapiGenerator = new OpenapiGenerator( "title", "", settings );
            LinkedHashSet<String> description = new LinkedHashSet<>();

            urls.forEach( url -> {
                getLog().info( "Reading config from " + url.getPath() );
                Module config = Module.CONFIGURATION.fromUrl( url );
                config.services.forEach( ( name, service ) -> {
                    WsConfig wsService = ( WsConfig ) service.ext.get( "ws-service" );
                    if ( wsService == null ) {
                        getLog().debug( "Skipping bean: " + name );
                        return;
                    }
                    getLog().debug( "WS bean: " + name + " implementing " + service.implementation );
                    try {
                        ClassRealm realm = pluginDescriptor.getClassRealm();
                        Class clazz = realm.loadClass( service.implementation );
                        OpenapiGenerator.Result result = openapiGenerator.processWebservice( clazz, wsService.path.stream().findFirst().orElse( "" ) );
                        getLog().info( "WebService class " + clazz.getCanonicalName() + " " + result );
                        description.add( clazz.getCanonicalName() );
                    } catch( Exception e ) {
                        getLog().warn( "Could not deal with module: " + name + " due to the implementation class '"
                            + service.implementation + "' is unavailable", e );
                    }
                } );
            } );
            openapiGenerator.setDescription( "WS services: " + Joiner.on( ", " ).join( description ) );
            if ( settings.getOutputType() == OpenapiGeneratorSettings.Type.JSON ) {
                String json = Binder.json.marshal( openapiGenerator.build() );
                outputPath += ".json";
                getLog().info( "OpenAPI JSON generated -> " + outputPath );
                IOUtils.write( json.getBytes( StandardCharsets.UTF_8 ), new FileOutputStream( outputPath ) );
                getLog().debug( "OpenAPI JSON is written to " + outputPath );
            } else {
                outputPath += ".yml";
                String yaml = Yaml.mapper().writeValueAsString( openapiGenerator.build() );
                getLog().info( "OpenAPI YAML generated -> " + outputPath );
                IOUtils.write( yaml.getBytes( StandardCharsets.UTF_8 ), new FileOutputStream( outputPath ) );
                getLog().debug( "OpenAPI YAML is written to " + outputPath );
            }
        } catch( Exception e ) {
            throw new ApplicationException( e );
        }
    }

    void setOutputPath( String outputPath ) {
        this.outputPath = outputPath;
    }

    void setOutputType( String outputType ) {
        this.outputType = outputType;
    }
}
