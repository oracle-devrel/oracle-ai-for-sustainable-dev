## Deploying the LRA sample app using Helm Chart
Modify the `sampleappslraspring-openshift/values.yaml` and provide the details.

```
sampleappslra:
  lraCoordinatorUrl: http://otmm-tcs.otmm.svc.cluster.local:9000/api/v1/lra-coordinator
  tripmanager:
    name: trip-manager
    host: trip-manager
    namespace: otmm
    version: v1
    image: lra-trip-manager-sb:1.0
    imagePullSecret:
```

`namespace: otmm` : This will be the name of the current openshift project or namespace

`imagePullSecret` : If the image is pulled from OpenShift local image registry, `imagePullSecret` is not needed and can be left empty. 
And if the image is pulled from external image registry, imagePullSecret has to be created and `imagePullSecret` name has to be provided.

Refer [openshift documentation](https://docs.openshift.com/container-platform/4.14/openshift_images/managing_images/using-image-pull-secrets.html) on creating `imagePullSecret` while using external image registry.   

`image` : Image registry url. Change it to the appropriate external image registry URL based on the environment config. If it is external image URL, `imagePullSecret` parameter has to be provided. 

If it is using openshift local image registry, use the local registry url 
Local image registry example:
(`otmm` in the image URL is the project name)
```
image: image-registry.openshift-image-registry.svc:5000/otmm/lra-trip-manager-sb:1.0
``` 

### To Install:
Switch to the right project using `oc` CLI and deploy using below command.

For instance project name is `otmm`, then run:
```
oc project otmm
```
Deploy the Helm chart:
```
helm install sampleappslra sampleappslraspring-openshift/ --values sampleappslraspring-openshift/values.yaml
```

#### Verify deployment:
Run `oc` CLI to check the deployment status.

```
oc get pods
```
If everything is fine, it will result the output something like below:
```
$ oc get pods
NAME                            READY   STATUS    RESTARTS   AGE
flight-7cf8d77944-vt644         1/1     Running   0          20h
hotel-7799788b66-nj859          1/1     Running   0          2d21h
trip-manager-7d97f57786-5tl4r   1/1     Running   0          2d21h
```

### To Uninstall:
Swithc to right project and run below command to uninstall the sample helmchart
```
helm uninstall sampleappslra
```