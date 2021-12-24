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
