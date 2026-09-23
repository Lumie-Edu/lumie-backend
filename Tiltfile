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
  strategy:
    type: Recreate
  selector:
    matchLabels:
      app: lumie-backend
  template:
    metadata:
      labels:
        app: lumie-backend
        environment: development
    spec:
      nodeSelector:
        lumie.io/account: second
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
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-ai-token-secrets
              key: OPENAI_API_KEY
        # Phase 4 cutover (dev only): route chat to the LangGraph chatbot-svc.
        - name: LUMIE_AI_LANGGRAPH_ENABLED
          value: "true"
        - name: CHATBOT_SVC_URL
          value: "http://chatbot-svc.lumie-dev.svc:8000"
        # Toss Payments — billing + tuition modules. Sandbox keys from Vault
        # via VSO (see lumie-infra applications/lumie/develop/common-values.yaml
        # `lumie-dev-toss-vss`). The TOSS_SANDBOX flag flips the BE to the
        # sandbox API host and relaxes signature checks for test webhooks.
        - name: TOSS_SANDBOX
          value: "true"
        - name: TOSS_CLIENT_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-toss-secrets
              key: TOSS_CLIENT_KEY
        - name: TOSS_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: lumie-dev-toss-secrets
              key: TOSS_SECRET_KEY
        - name: TOSS_WEBHOOK_SECRET
          valueFrom:
            secretKeyRef:
              name: lumie-dev-toss-secrets
              key: TOSS_WEBHOOK_SECRET
        resources:
          requests:
            cpu: 250m
            memory: 768Mi
          limits:
            memory: 768Mi
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

# Remote BuildKit build. Gradle and Docker layers stay on the staging node.
# Java and resource changes are synced into the development image, compiled in
# place, and picked up by Spring Boot DevTools. Build scripts, dependencies,
# Dockerfile changes, and deployment configuration are deliberately omitted so
# they retain the safer full remote-build-and-redeploy path.
custom_build(
    '%s/dev/lumie-backend' % REGISTRY,
    './scripts/remote-buildkit-build.sh Dockerfile.dev .tilt-image-ref zot.lumie-infra.com/dev/lumie-backend',
    deps=['.'],
    outputs_image_ref_to='.tilt-image-ref',
    skips_local_docker=True,
    disable_push=True,
    live_update=[
        sync('app/src/main', '/workspace/app/src/main'),
        sync('libs', '/workspace/libs'),
        sync('modules', '/workspace/modules'),
        run(
            './gradlew -Dorg.gradle.jvmargs=-Xmx256m -Dorg.gradle.workers.max=1 :devClasses --no-daemon',
            trigger=['app/src/main/**', 'libs/**', 'modules/**'],
        ),
    ],
    ignore=[
        '.git',
        '**/build/',
        '**/.gradle/',
        '.tilt-image-ref',
    ],
)

k8s_resource(
    'lumie-backend',
    port_forwards=['18080:8080'],
    labels=['lumie-dev'],
)
