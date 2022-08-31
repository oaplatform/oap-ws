package oap.openapi.maven;

import com.google.common.base.Joiner;
import io.swagger.v3.core.util.Yaml;
import oap.application.ApplicationException;
import oap.json.Binder;
import oap.ws.WebServicesWalker;
import oap.ws.openapi.OpenapiGenerator;
import oap.ws.openapi.OpenapiGeneratorSettings;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
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
    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor pluginDescriptor;
    @Parameter( required = true, readonly = true, defaultValue = "swagger" )
    private String outputPath;

    @Parameter( required = true, readonly = true, defaultValue = "JSON" )
    private String outputType;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Objects.requireNonNull( outputPath );
        getLog().info( "OpenAPI generation..." );
        try {
            var settings = OpenapiGeneratorSettings
                .builder()
                .processOnlyAnnotatedMethods( false )
                .outputType( OpenapiGeneratorSettings.Type.valueOf( outputType ) )
                .build();
            var openapiGenerator = new OpenapiGenerator( "title", "", settings );
            var visitor = new WebServiceVisitorForPlugin( pluginDescriptor, openapiGenerator, classpath, outputPath, getLog() );

            WebServicesWalker.walk( visitor );
            getLog().info( "Configurations (from oap-module.conf files) loaded: " + visitor.getModuleConfigurations() );

            openapiGenerator.setDescription( "WS services: " + Joiner.on( ", " ).join( visitor.getDescription() ) );
            if ( settings.getOutputType() == OpenapiGeneratorSettings.Type.JSON ) {
                String json = Binder.json.marshal( openapiGenerator.build() );
                outputPath = visitor.getOutputPath() + ".json";
                getLog().info( "OpenAPI JSON generated -> " + outputPath );
                IOUtils.write( json.getBytes( StandardCharsets.UTF_8 ), new FileOutputStream( outputPath ) );
                getLog().debug( "OpenAPI JSON is written to " + outputPath );
            } else {
                outputPath = visitor.getOutputPath() + ".yml";
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
