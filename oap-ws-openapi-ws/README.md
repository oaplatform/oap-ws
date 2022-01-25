# OAP-WS Openapi module
Generating OpenApi documentation for OAP web services


## General Information 
- Generates OpenApi (version 3.0+) documentation for all classes which are used as web services in an easy way.
- No need to use swagger annotation for api documentation. Module uses data from reflection to form proper and appropriate document.


## Documentation
- All necessary information about OpenApi specification could be found [_here_](https://swagger.io/resources/open-api/).


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

- for disabling generation for specific class can be used enabled field within @WsOpenapi annotation

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
