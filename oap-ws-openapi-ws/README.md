# OAP-WS Openapi module
Generating [OpenApi](https://www.openapis.org) documentation for OAP web services


## General Information 
- Generates OpenApi (version 3.0+) documentation for all classes which are used as web services in an easy way.
- No need to use swagger annotation for api documentation. Module uses data from reflection to form proper and appropriate document.


## Documentation
- All necessary information about OpenApi specification could be found in Swagger [docs](https://swagger.io/resources/open-api/) or in original [OAS](https://spec.openapis.org/oas/latest.html).


## OAP-WS-API Comparison
OAP-WS-Openapi provides functionality similar to OAP-WS-API module.

### Difference: 

##### Schema
- OpenApi module uses [OAS](https://spec.openapis.org/oas/latest.html) for describing web services.
- Api module uses its own format for web services description.

##### Web services

- OpenApi module only describes services which marked as included.
- Api module describes all web services within module.

### Pros & Cons of using OAP-WS-Openapi module:

#### Pros

- Uses [OAS](https://spec.openapis.org/oas/latest.html) for describing web services.
- OpenApi is well known and widespread format.
- Response schema can be used for code generation tools like [Swagger Codegen](https://swagger.io/tools/swagger-codegen/) or others.
- Web services can be manually included/excluded to/from resulting document.

#### Cons

- Requires OAS versions support and update.
- Requires dependency on OAS implementation.
- Web services which added to module as a dependency can't be easily added to description.

## Usage
Steps to use this module within other oap module

- oap-ws-openapi-ws module depends on oap-ws module.

- add module as dependency to _pom.xml/build.gradle_

```
//Example:

<dependency>
    <groupId>oap</groupId>
    <artifactId>oap-ws-openapi-ws</artifactId>
    <version>17.3.0.8</version>
</dependency>
```
- add module as dependency to _oap.module.conf_

```
name: some-module
dependsOn: oap-ws-openapi-ws
services {
...
```

- mark with @WsOpenapi annotation all services which should be included to OpenApi documentation

```
@WsOpenapi
class TestWS {

    @WsMethod( method = GET, id = "returnTwo", path = "/" )
    public int test() {
        return 2;
    }
}
```

- use `enabled = false` property to disable OpenAPI output for the specific class

```
@WsOpenapi( enabled = false )
class TestWS {

    @WsMethod( method = GET, id = "returnTwo", path = "/" )
    public int test() {
        return 2;
    }
}
```

- [Tag parameter](https://swagger.io/specification/#tag-object) can be specified by tag field within @WsOpenapi annotation
```
@WsOpenapi( tag = "Test" )
class TestWS {

    @WsMethod( method = GET, id = "returnTwo", path = "/" )
    public int test() {
        return 2;
    }
}
```

- [OpenApi Info](https://swagger.io/specification/#info-object) can be specified within application.conf

```
oap-ws-openapi-ws.openapi-info.parameters.title = "Title"
oap-ws-openapi-ws.openapi-info.parameters.description = "Description"
oap-ws-openapi-ws.openapi-info.parameters.version = "0.1.1"
```
