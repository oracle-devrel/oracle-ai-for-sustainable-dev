
## To Install:
helm install sampleappslra --namespace otmm sampleappslrak8s/ --values sampleappslrak8s/values.yaml

## To Uninstall:
helm uninstall sampleappslra --namespace otmm

## Create a new Pod that runs curl
To test services in kubernetes, you can use curl. 
For that, find an image that contains curl. The next thing to do is run it in Kubernetes, in a Pod.
To do that, you can use the kubectl run command, which creates a single Pod with curl.

```bash
kubectl run mycurlpod --image=curlimages/curl -i --tty -- sh
```

## Check services:
$ curl http://hotel.otmm.svc.cluster.local:8082/hotelService/api/hotel
[]

$ curl http://flight.otmm.svc.cluster.local:8083/flightService/api/flight
[]

$ curl http://trip-manager.otmm.svc.cluster.local:8081/trip-service/api/trip
[]

## Create a Booking
$ curl -X POST -d '' http://trip-manager.otmm.svc.cluster.local:8081/trip-service/api/trip?hotelName=Mercury&flightNumber=A123

{
"details": [{
    "encodedId": "http%3A%2F%2Fotmm-tcs.otmm.svc.cluster.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda",
    "id": "http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda",
    "name": "Mercury",
    "status": "PROVISIONAL",
    "type": "Hotel"
    }, {
    "details": [],
    "encodedId": "http%3A%2F%2Fotmm-tcs.otmm.svc.cluster.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda",
    "id": "http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda",
    "name": "A123",
    "status": "PROVISIONAL",
    "type": "Flight"
    }],
"encodedId": "http%3A%2F%2Fotmm-tcs.otmm.svc.cluster.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda",
"id": "http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda",
"name": "Trip",
"status": "PROVISIONAL",
"type": "Trip"
}

## Confirm the Booking

$ curl --location --request PUT -H "Long-Running-Action: http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda" -d '' http://trip-manager.otmm.svc.cluster.local:8081/trip-service/api/trip/http%3A%2F%2Fotmm-tcs.otmm.svc.cluster
.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda

## Get the Booking Details
 $ curl http://trip-manager.otmm.svc.cluster.local:8081/trip-service/api/trip

[{
    "details": [{
    "encodedId": "http%3A%2F%2Fotmm-tcs.otmm.svc.cluster.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda",
    "id": "http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda",
    "name": "Mercury",
    "status": "CONFIRMED",
    "type": "Hotel"
    }, {
    "details": [],
    "encodedId": "http%3A%2F%2Fotmm-tcs.otmm.svc.cluster.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda",
    "id": "http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda",
    "name": "A123",
    "status": "CONFIRMED",
    "type": "Flight"
    }],
"encodedId": "http%3A%2F%2Fotmm-tcs.otmm.svc.cluster.local%3A9000%2Fapi%2Fv1%2Flra-coordinator%2F64fc621a-1521-4bae-a481-975061cf4bda",
"id": "http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator/64fc621a-1521-4bae-a481-975061cf4bda",
"name": "Trip",
"status": "CONFIRMED",
"type": "Trip"
}]