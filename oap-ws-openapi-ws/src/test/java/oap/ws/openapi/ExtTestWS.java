
package oap.ws.openapi;

import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.Optional;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.ws.WsParam.From.PATH;
import static oap.ws.WsParam.From.QUERY;

@WsOpenapi( tag = "Test" )
public class ExtTestWS {

    @WsMethod( method = GET, path = "/test/empty/{id}", description = "This method returns nothing (Void)")
    public void testVoid( @WsParam( from = PATH ) String id,
                          @WsParam( from = QUERY ) Optional<Integer> limit ) {
    }
}
