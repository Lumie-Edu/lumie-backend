# Lumie Backend Development with Tilt
# =====================================
# Usage: tilt up

# Configuration
REGISTRY = 'zot.lumie-infra.com'
NAMESPACE = 'lumie-dev'

# Environment variables
COMMON_ENV = {
    'SPRING_PROFILES_ACTIVE': 'dev',
    # Database via CNPG native Pooler
    'DB_HOST': 'lumie-dev-pooler.lumie-dev.svc.cluster.local',
    'DB_PORT': '5432',
    'DB_NAME': 'lumie',
    # Read-only DB
    'RO_DB_HOST': 'lumie-dev-db-rw.lumie-dev.svc.cluster.local',
    'RO_DB_PORT': '5432',
    'RO_DB_NAME': 'lumie',
    # RabbitMQ (dev-only instance)
    'RABBITMQ_HOST': 'rabbitmq-dev.lumie-dev.svc.cluster.local',
    'RABBITMQ_PORT': '5672',
    # RABBITMQ_USERNAME/PASSWORD come from the Topology-Operator-generated
    # `lumie-user-credentials` secret (operator-managed app user) —
    # see secretKeyRef env below. No literal username, no Vault path.
    # Redis (Bitnami Helm chart standalone)
    'REDIS_HOST': 'redis-dev-master.lumie-dev.svc.cluster.local',
    'REDIS_PORT': '6379',
    # AI service
    'GRADING_SVC_URL': 'http://grading-svc.lumie-dev.svc:8000',
    'REPORT_SVC_URL': 'http://report-svc.lumie-dev.svc:8000',
    # MinIO
    'MINIO_BUCKET_NAME': 'lumie-dev',
    'MINIO_EXTERNAL_ENDPOINT': 'https://dev.lumie-infra.com',
    # Frontend callback
    'FRONTEND_CALLBACK_URL': 'https://dev.lumie-infra.com/auth/callback',
    # OAuth redirect URIs (also register these in each provider's console)
    'GOOGLE_REDIRECT_URI': 'https://dev.lumie-infra.com/api/auth/v1/oauth2/google/callback',
    'KAKAO_REDIRECT_URI': 'https://dev.lumie-infra.com/api/auth/v1/oauth2/kakao/callback',
    'NAVER_REDIRECT_URI': 'https://dev.lumie-infra.com/api/auth/v1/oauth2/naver/callback',
}

def generate_env_yaml():
    env_yaml = ''
    for key, value in COMMON_ENV.items():
        env_yaml += '''
        - name: %s
          value: "%s"''' % (key, value)
    return env_yaml

# Deployment YAML
deployment_yaml = '''
apiVersion: apps/v1
kind: Deployment
metadata:
  name: lumie-backend
  namespace: %s
  labels:
    app: lumie-backend
    environment: development
spec:
  replicas: 1
  selector:
    matchLabels:
      app: lumie-backend
  template:
    metadata:
      labels:
        app: lumie-backend
        environment: development
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
      - name: lumie-backend
        image: %s/dev/lumie-backend
        imagePullPolicy: Always
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        env:%s
        # Runtime app role (lumie_app NOSUPERUSER NOBYPASSRLS) — RLS-active.
        # Migrator role (postgres SUPERUSER) is reserved for Flyway only.
        # See lumie-infra applications/lumie/backend common-values.yaml for the
        # equivalent prod wiring, and HARD_RULES_CROSS.md for why the split exists.
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: lumie-dev-app-db-secrets
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-dev-app-db-secrets
              key: password
        - name: DB_MIGRATOR_USERNAME
          valueFrom:
            secretKeyRef:
              name: lumie-dev-db-superuser
              key: username
        - name: DB_MIGRATOR_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-dev-db-superuser
              key: password
        - name: RO_DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: lumie-dev-app-db-secrets
              key: username
        - name: RO_DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-dev-app-db-secrets
              key: password
        - name: RABBITMQ_USERNAME
          valueFrom:
            secretKeyRef:
              name: lumie-user-credentials
              key: username
        - name: RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: lumie-user-credentials
              key: password
        - name: JWT_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-jwt-secrets
              key: secret
        - name: JWT_ACCESS_EXPIRATION
          value: "3600"
        - name: JWT_REFRESH_EXPIRATION
          value: "604800"
        - name: LUMIE_INTERNAL_HMAC_SECRET
          valueFrom:
            secretKeyRef:
              name: lumie-dev-internal-secrets
              key: LUMIE_INTERNAL_HMAC_SECRET
        - name: GOOGLE_CLIENT_ID
          value: "dev-google-client-id"
        - name: GOOGLE_CLIENT_SECRET
          value: "dev-google-client-secret"
        - name: KAKAO_CLIENT_ID
          value: "dev-kakao-client-id"
        - name: KAKAO_CLIENT_SECRET
          value: "dev-kakao-client-secret"
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
        - name: DEEPSEEK_API_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-ai-token-secrets
              key: DEEPSEEK_API_KEY
        - name: GEMINI_API_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-ai-token-secrets
              key: GOOGLE_API_KEY
        # Phase 4 cutover (dev only): route chat to the LangGraph chatbot-svc.
        - name: LUMIE_AI_LANGGRAPH_ENABLED
          value: "true"
        - name: CHATBOT_SVC_URL
          value: "http://chatbot-svc.lumie-dev.svc:8000"
        resources:
          requests:
            cpu: 100m
            memory: 512Mi
          limits:
            memory: 2Gi
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
''' % (NAMESPACE, REGISTRY, generate_env_yaml())

# Service YAML
service_yaml = '''
apiVersion: v1
kind: Service
metadata:
  name: lumie-backend
  namespace: %s
spec:
  type: ClusterIP
  selector:
    app: lumie-backend
  ports:
  - name: http
    port: 8080
    targetPort: http
    protocol: TCP
''' % NAMESPACE

k8s_yaml(blob(deployment_yaml + '\n---\n' + service_yaml))

# Docker build (rebuild when jar changes)
docker_build(
    '%s/dev/lumie-backend' % REGISTRY,
    context='.',
    dockerfile='Dockerfile.dev',
    only=[
        'app/build/libs/',
        'Dockerfile.dev',
    ],
)

k8s_resource(
    'lumie-backend',
    port_forwards=['18080:8080'],
    labels=['lumie-dev'],
    resource_deps=['gradle-build'],
)

# Gradle build
local_resource(
    'gradle-build',
    cmd='./gradlew :app:bootJar -x test --parallel',
    deps=[
        'modules/',
        'libs/',
        'app/',
        'build.gradle.kts',
        'settings.gradle.kts',
    ],
    ignore=[
        '**/build/',
        '**/.gradle/',
    ],
    labels=['build'],
)
