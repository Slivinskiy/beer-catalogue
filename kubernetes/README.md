# Kubernetes Deployment

This folder contains a minimal Minikube-ready deployment for the `beer-catalogue` app.

## 1. Start Minikube

```bash
minikube start
```

## 2. Build the Docker image inside Minikube

```bash
minikube image build -t beer-catalogue:latest .
```

## 3. Fill in the database secret values

Edit [secret.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/secret.yaml) and replace:

- `<rds-endpoint>`
- `<database-name>`
- `<rds-username>`
- `<rds-password>`

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
