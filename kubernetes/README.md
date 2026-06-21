# Kubernetes Deployment

This folder contains a minimal Minikube-ready deployment for the `beer-catalogue` app.

A reference deployment values file is available at:

- [values.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/values.yaml)

`values.yaml` contains non-secret deployment settings only.

## 1. Start Minikube

```bash
minikube start
```

## 2. Build the Docker image inside Minikube

```bash
minikube image build -t beer-catalogue:latest .
```

## 3. Fill in the database secret values

Before applying the manifests, open [secret.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/secret.yaml) and replace:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

with real PostgreSQL values.

You can also use [values.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/values.yaml) as the central reference for image, service, resource, probe, and non-secret database deployment values used by the deployment.

Datasource credentials are defined only in [secret.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/secret.yaml).

## 4. Apply the manifests

```bash
kubectl apply -k kubernetes
```

## 5. Wait for the rollout

```bash
kubectl rollout status deployment/beer-catalogue -n beer-catalogue
```

## 6. Get the service URL

```bash
minikube service beer-catalogue-service -n beer-catalogue --url
```

That URL will expose the app running in the cluster.
