package oap.ws.openapi;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CreativeUniversal extends AbstractExtensionClass {
    public final List<String> assets = new ArrayList<>();

    @Override
    @JsonIgnore
    public Set<String> getFormat() {
        return Set.of( "html", "xml" );
    }
}
