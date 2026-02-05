# Lumie Backend Development with Tilt
# =====================================
# Usage: tilt up [service-name]
# Example: tilt up auth-svc
#          tilt up  (starts all services)

# Configuration
REGISTRY = 'zot0213.kro.kr'
NAMESPACE = 'lumie-dev'

# Fixed ClusterIPs for gRPC services (prevents DNS cache issues)
GRPC_CLUSTER_IPS = {
    'tenant-svc': '10.43.200.1',
    'auth-svc': '10.43.200.2',
    'billing-svc': '10.43.200.3',
    'academy-svc': '10.43.200.4',
    'exam-svc': '10.43.200.5',
    'content-svc': '10.43.200.6',
    'spreadsheet-svc': '10.43.200.7',
}

# Service definitions
SERVICES = {
    'tenant-svc': {
        'path': 'services/platform/tenant-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': [],
    },
    'auth-svc': {
        'path': 'services/platform/auth-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['tenant-svc'],
    },
    'billing-svc': {
        'path': 'services/platform/billing-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['tenant-svc'],
    },
    'academy-svc': {
        'path': 'services/core/academy-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['auth-svc', 'tenant-svc', 'billing-svc'],
    },
    'exam-svc': {
        'path': 'services/core/exam-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['tenant-svc', 'billing-svc', 'academy-svc'],
    },
    'content-svc': {
        'path': 'services/core/content-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['tenant-svc'],
    },
    'file-svc': {
        'path': 'services/support/file-svc',
        'port': 8080,
        'grpc_port': None,
        'deps': ['tenant-svc'],
    },
    'spreadsheet-svc': {
        'path': 'services/core/spreadsheet-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['tenant-svc'],
    },
}

# Environment variables for all services
# Database: lumie-dev-db (dev-only, managed by ArgoCD)
# RabbitMQ/Redis: prod infrastructure (shared)
COMMON_ENV = {
    'SPRING_PROFILES_ACTIVE': 'dev',
    # Database via PgBouncer (session mode for multi-tenancy)
    'DB_HOST': 'pgbouncer.lumie-dev.svc.cluster.local',
    'DB_PORT': '5432',
    'DB_NAME': 'lumie',
    # RabbitMQ (prod, shared)
    'RABBITMQ_HOST': 'rabbitmq.lumie-event.svc.cluster.local',
    'RABBITMQ_PORT': '5672',
    # Redis Sentinel (prod, shared)
    'REDIS_SENTINEL_MASTER': 'mymaster',
    'REDIS_SENTINEL_NODES': 'redis.lumie-cache.svc.cluster.local:26379',
    # gRPC endpoints (within lumie-dev namespace)
    'AUTH_SVC_GRPC_HOST': 'auth-svc-grpc.lumie-dev.svc.cluster.local',
    'TENANT_SVC_GRPC_HOST': 'tenant-svc-grpc.lumie-dev.svc.cluster.local',
    'BILLING_SVC_GRPC_HOST': 'billing-svc-grpc.lumie-dev.svc.cluster.local',
    'ACADEMY_SVC_GRPC_HOST': 'academy-svc-grpc.lumie-dev.svc.cluster.local',
}

# Namespace and Secrets are managed by ArgoCD (lumie-infra/applications/lumie-dev)
# Tilt only manages Deployments and Services

# Function to generate deployment YAML
def generate_deployment(name, config):
    grpc_port_yaml = ''
    if config['grpc_port']:
        grpc_port_yaml = '''
        - name: grpc
          containerPort: %d
          protocol: TCP''' % config['grpc_port']

    env_yaml = ''
    for key, value in COMMON_ENV.items():
        env_yaml += '''
        - name: %s
          value: "%s"''' % (key, value)

    # Add secret references
    env_yaml += '''
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: lumie-dev-db-secrets
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-dev-db-secrets
              key: password
        - name: RABBITMQ_USERNAME
          value: "lumie"
        - name: RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-dev-rabbitmq-secrets
              key: password
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-dev-redis-secrets
              key: password'''

    # Add JWT secrets for auth-svc
    if name == 'auth-svc':
        env_yaml += '''
        - name: JWT_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-jwt-secrets
              key: secret
        - name: JWT_ACCESS_EXPIRATION
          value: "3600000"
        - name: JWT_REFRESH_EXPIRATION
          value: "604800000"
        - name: GOOGLE_CLIENT_ID
          value: "dev-google-client-id"
        - name: GOOGLE_CLIENT_SECRET
          value: "dev-google-client-secret"
        - name: KAKAO_CLIENT_ID
          value: "dev-kakao-client-id"
        - name: KAKAO_CLIENT_SECRET
          value: "dev-kakao-client-secret"'''

    # Add Report service URL for exam-svc
    if name == 'exam-svc':
        env_yaml += '''
        - name: REPORT_SVC_URL
          value: "http://report-svc.lumie-dev.svc:8000"'''

    # Add MinIO secrets for file-svc
    if name == 'file-svc':
        env_yaml += '''
        - name: MINIO_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-minio-secrets
              key: access-key
        - name: MINIO_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-minio-secrets
              key: secret-key
        - name: MINIO_ENDPOINT
          valueFrom:
            secretKeyRef:
              name: lumie-dev-minio-secrets
              key: endpoint
        - name: MINIO_BUCKET
          value: "lumie-dev"'''

    # Add Redis config for spreadsheet-svc
    if name == 'spreadsheet-svc':
        env_yaml += '''
        - name: REDIS_HOST
          value: "redis.lumie-cache.svc.cluster.local"
        - name: REDIS_PORT
          value: "6379"'''

    return '''
apiVersion: apps/v1
kind: Deployment
metadata:
  name: %s
  namespace: %s
  labels:
    app: %s
    environment: development
spec:
  replicas: 1
  selector:
    matchLabels:
      app: %s
  template:
    metadata:
      labels:
        app: %s
        environment: development
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
      - name: %s
        image: %s/dev/%s
        imagePullPolicy: Always
        ports:
        - name: http
          containerPort: %d
          protocol: TCP%s
        env:%s
        resources:
          requests:
            cpu: 50m
            memory: 50Mi
          limits:
            memory: 1Gi
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: http
          initialDelaySeconds: 90
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: http
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        startupProbe:
          httpGet:
            path: /actuator/health
            port: http
          periodSeconds: 5
          failureThreshold: 60
        securityContext:
          runAsNonRoot: true
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: false
          capabilities:
            drop:
              - ALL
''' % (name, NAMESPACE, name, name, name, name, REGISTRY, name, config['port'], grpc_port_yaml, env_yaml)

# Development domain
DEV_DOMAIN = 'dev.lumie0213.kro.kr'

# API path mapping for each service
# Note: spreadsheet-svc ingress is managed by ArgoCD (lumie-dev-api-protected) with JWT plugins
API_PATHS = {
    'tenant-svc': '/api/tenant',
    'auth-svc': '/api/auth',
    'billing-svc': '/api/billing',
    'academy-svc': '/api/academy',
    'exam-svc': '/api/exam',
    'content-svc': '/api/content',
    'file-svc': '/api/file',
}

# Function to generate service YAML
def generate_service(name, config):
    grpc_service = ''
    if config['grpc_port']:
        grpc_cluster_ip = GRPC_CLUSTER_IPS.get(name, '')
        grpc_service = '''
---
apiVersion: v1
kind: Service
metadata:
  name: %s-grpc
  namespace: %s
spec:
  type: ClusterIP
  clusterIP: %s
  selector:
    app: %s
  ports:
  - name: grpc
    port: 9090
    targetPort: grpc
    protocol: TCP
''' % (name, NAMESPACE, grpc_cluster_ip, name)

    return '''
apiVersion: v1
kind: Service
metadata:
  name: %s
  namespace: %s
spec:
  type: ClusterIP
  selector:
    app: %s
  ports:
  - name: http
    port: 8080
    targetPort: http
    protocol: TCP
%s''' % (name, NAMESPACE, name, grpc_service)

# Function to generate Ingress YAML for Kong
def generate_ingress(name, config):
    if name not in API_PATHS:
        return ''

    api_path = API_PATHS[name]
    ingress_yaml = '''
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: %s-ingress
  namespace: %s
  annotations:
    konghq.com/strip-path: "true"
    konghq.com/plugins: lumie-dev-cors
spec:
  ingressClassName: kong
  rules:
  - host: %s
    http:
      paths:
      - path: %s
        pathType: Prefix
        backend:
          service:
            name: %s
            port:
              number: 8080
''' % (name, NAMESPACE, DEV_DOMAIN, api_path, name)

    return ingress_yaml

# Build and deploy each service
for name, config in SERVICES.items():
    # Generate K8s resources
    deployment_yaml = generate_deployment(name, config)
    service_yaml = generate_service(name, config)
    ingress_yaml = generate_ingress(name, config)

    # Combine all YAML for this service
    combined_yaml = deployment_yaml + '\n---\n' + service_yaml
    if ingress_yaml:
        combined_yaml += '\n---\n' + ingress_yaml

    # Apply YAML using k8s_yaml (handles automatic image tag injection)
    k8s_yaml(blob(combined_yaml))

    # Docker build (rebuild image when jar changes)
    # Java doesn't support true hot-reload, so we rebuild on jar change
    docker_build(
        '%s/dev/%s' % (REGISTRY, name),
        context='.',
        dockerfile='Dockerfile.dev',
        build_args={
            'SERVICE_PATH': config['path'],
        },
        only=[
            config['path'] + '/build/libs/',
            'Dockerfile.dev',
        ],
    )

    # Resource configuration
    deps = [SERVICES[d]['path'] for d in config['deps']] if config['deps'] else []
    k8s_resource(
        name,
        port_forwards=['%d:8080' % (18080 + list(SERVICES.keys()).index(name))],
        resource_deps=[d for d in config['deps']] if config['deps'] else [],
        labels=['lumie-dev'],
    )

# Local resource for Gradle build
local_resource(
    'gradle-build',
    cmd='./gradlew bootJar -x test --parallel',
    deps=[
        'services/',
        'libs/',
        'build.gradle.kts',
        'settings.gradle.kts',
    ],
    ignore=[
        '**/build/',
        '**/.gradle/',
    ],
    labels=['build'],
)
