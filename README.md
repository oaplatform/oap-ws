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
