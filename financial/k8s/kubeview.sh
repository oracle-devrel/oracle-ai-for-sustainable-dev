

https://kubeview.benco.io/deploy/helm/

helm repo add kv2 https://code.benco.io/kubeview/deploy/helm
helm repo update

helm install kubeview kv2/kubeview

#todo modify values.yaml so it doesnt create LB , etc.

http://129.159.11.143:8000/