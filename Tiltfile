# Lumie Backend Development with Tilt
# =====================================
# Usage: tilt up [service-name]
# Example: tilt up auth-svc
#          tilt up  (starts all services)

# Configuration
REGISTRY = 'zot0213.kro.kr'
NAMESPACE = 'lumie-dev'

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
        'grpc_port': None,
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
        'deps': ['tenant-svc', 'billing-svc'],
    },
    'exam-svc': {
        'path': 'services/core/exam-svc',
        'port': 8080,
        'grpc_port': 9090,
        'deps': ['tenant-svc', 'billing-svc'],
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
}

# Environment variables for all services
# Database: lumie-dev-db (dev-only, managed by ArgoCD)
# RabbitMQ/Redis: prod infrastructure (shared)
COMMON_ENV = {
    'SPRING_PROFILES_ACTIVE': 'dev',
    # Database (lumie-dev namespace)
    'DB_HOST': 'lumie-dev-db-rw.lumie-dev.svc.cluster.local',
    'DB_PORT': '5432',
    'DB_NAME': 'lumie',
    # RabbitMQ (prod, shared)
    'RABBITMQ_HOST': 'rabbitmq.lumie-event.svc.cluster.local',
    'RABBITMQ_PORT': '5672',
    # Redis Sentinel (prod, shared)
    'REDIS_SENTINEL_MASTER': 'mymaster',
    'REDIS_SENTINEL_NODES': 'redis.lumie-cache.svc.cluster.local:26379',
    # gRPC endpoints (within lumie-dev namespace)
    'TENANT_SVC_GRPC_HOST': 'tenant-svc-grpc.lumie-dev.svc.cluster.local',
    'BILLING_SVC_GRPC_HOST': 'billing-svc-grpc.lumie-dev.svc.cluster.local',
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
        image: %s/dev/%s:dev
        imagePullPolicy: Always
        ports:
        - name: http
          containerPort: %d
          protocol: TCP%s
        env:%s
        resources:
          requests:
            cpu: 15m
            memory: 256Mi
          limits:
            cpu: 500m
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

# Function to generate service YAML
def generate_service(name, config):
    grpc_service = ''
    if config['grpc_port']:
        grpc_service = '''
---
apiVersion: v1
kind: Service
metadata:
  name: %s-grpc
  namespace: %s
spec:
  type: ClusterIP
  selector:
    app: %s
  ports:
  - name: grpc
    port: 9090
    targetPort: grpc
    protocol: TCP
''' % (name, NAMESPACE, name)

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

# Build and deploy each service
for name, config in SERVICES.items():
    # Generate K8s resources
    deployment_yaml = generate_deployment(name, config)
    service_yaml = generate_service(name, config)

    k8s_yaml(blob(deployment_yaml))
    k8s_yaml(blob(service_yaml))

    # Docker build with restart on jar change
    # For Java, we rebuild the image when jar changes (no true hot-reload)
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
        live_update=[
            # Sync the jar file and restart container
            sync(config['path'] + '/build/libs/', '/app/'),
            restart_container(),
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
