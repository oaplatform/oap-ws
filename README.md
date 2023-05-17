# oap-ws

## oap-ws-file-ws

    oap-ws-file-bucket-manager.parameters.buckets {
         DEFAULT = /path/to/default
         bucket1 = /path/to/bucket1
    }

## cors proxy
    docker run --add-host=host.docker.internal:host-gateway--restart always -d -p <port1>:<port1> bulletmark/corsproxy <port1>:host.docker.internal:<port2>

- port1 - cors http port
- port2 - oap-ws http port

## OpenAPI with Swagger-UI on docker
    docker pull swaggerapi/swagger-ui
    docker run -p 8088:8080 -e BASE_URL=/swagger-ui -e SWAGGER_JSON_URL=http://localhost:20001/system/openapi swaggerapi/swagger-ui
- modify SWAGGER_JSON_URL according to your host:port
- modify desired external docker port (8088 in '-p 8088:8080')
- make sure that you can fetch OpenAPI json file from URL '/system/openapi' (like 'http://localhost:20001/system/openapi')
- go to '/swagger-ui/' (like 'http://localhost:8088/swagger-ui/')
- Note: in case you get the error 'Failed to load API definition.' it's possible a CORS issue. 
The simplest way to fix it is to install 'Allow-Control-Allow-Origin plugin' for Chrome browser 
from https://chrome.google.com/webstore/detail/moesif-orign-cors-changer/digfbfaphojjndkpccljibejjbppifbc?hl=en-US
- make sure you've had this Chrome extension enabled and try open /swagger-ui/ again
- Note: after using Swagger-UI switch off this Chrome extension to stay secured

## interceptors/filters
interceptors are implementing oap.ws.Interceptor and 
interceptors is configured as below 
~~~
some-ws {
    implementation = ...
    ws-service {
      sessionAware = true
      interceptors = [
        modules.full-module-name.some-interceptor
      ]
    }
}
~~~
interceptors may catch parameters when calling like below
~~~
@Override
public Optional<Response> before( InvocationContext context ) {
    return "error".equals( context.exchange.getStringParameter( "wsMethodParameter" ) )
        ? Optional.of( new Response( FORBIDDEN, "caused by interceptor" ) )
        : Optional.empty();
}
~~~
## processors or request-chain (workflow)
Some service class has some workflow definition
~~~
public RequestWorkflow<ProcessingState> workflow; // to be shared in workflow chain
private void initWorkflow() {
workflow = RequestWorkflow
            .init( new RequestHandler1( ) )
            .next( new RequestHandler2( ) )
            ...
            .build()
}
static class RequestHandler1 extends PnioRequestHandler<Object> {
        @Override
        public void handle( PnioExchange<Object> pnioExchange, Object o ) throws InterruptedException, IOException {

        }
}
static class RequestHandler2 ...
~~~
and ProcessingState class has at least 
~~~
public Object originalRequest;
~~~

## validators
Web service parameter validators:
- by JSON schema - WsValidateJson annotation for single parameter
~~~
@WsMethod
public String checkData(
    @WsParam( from = BODY )
    @WsValidateJson( schema = "/schemas/data.conf" )
    Data data 
) {
...
}
~~~
- by calling the method for all parameters
~~~
    @WsMethod( method = GET, path = "addNew/{id}" )
    @WsValidate( "dataValidator" )
    public int serviceWithValidate( @WsParam( from = QUERY ) int requiredParameter, @WsParam( from = PATH ) String id ) {
        return requiredParameter;
    }
    
    protected ValidationErrors dataValidator( int shouldBePositive, String id ) {
        return id.trim().length() == 0 || shouldBePositive > 0 
        ? ValidationErrors.empty() 
        : ValidationErrors.error( "not a positive parameter or id is empty" );
    }
~~~
