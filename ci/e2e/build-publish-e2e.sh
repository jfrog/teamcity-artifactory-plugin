#!/usr/bin/env bash
#
# End-to-end build-and-publish test.
#
# This is the "real" end-to-end test: it stands up a full, working pipeline and
# proves the freshly built Artifactory plugin can actually publish artifacts and
# build-info to a live Artifactory from a real TeamCity build - all on the JDK
# that TeamCity 2025.11 ships with (JDK 21).
#
# The flow, entirely automated:
#   1. Bring up Artifactory (OSS + PostgreSQL) and create a generic repo.
#      (Or point at an existing Artifactory via ARTIFACTORY_URL - see below.)
#   2. Bring up a TeamCity 2025.11 server with the plugin pre-installed and the
#      Artifactory connection pre-seeded (config/artifactory-config.xml).
#   3. Drive the first-run wizard, create an admin (via the superuser token),
#      bring up a TeamCity agent and authorize it.
#   4. Create a project + build config with a build step that produces an
#      artifact and uses the Artifactory plugin's generic upload spec, with
#      "deploy artifacts" and "publish build-info" enabled.
#   5. Run the build and wait for it to finish.
#   6. Assert the build SUCCEEDED and verify - directly against Artifactory's
#      REST API - that both the artifact and the build-info actually landed.
#
# Usage:
#   ci/e2e/build-publish-e2e.sh [path-to-plugin.zip]
#
# Environment overrides:
#   TEAMCITY_IMAGE     TeamCity server image   (default: jetbrains/teamcity-server:2025.11)
#   TEAMCITY_AGENT_IMAGE                        (default: jetbrains/teamcity-agent:2025.11)
#   TC_PORT            Host port for TeamCity   (default: 8111)
#   STARTUP_TIMEOUT    Seconds to wait per stage(default: 600)
#
#   # Artifactory: by default this script brings up its own Artifactory OSS.
#   # To test against an existing Artifactory instead, set ARTIFACTORY_URL and
#   # the script will skip the bring-up (BYO mode):
#   ARTIFACTORY_URL    e.g. http://my-artifactory:8081/artifactory (base, no trailing slash)
#   ARTIFACTORY_REPO   generic repo to publish  (default: teamcity-generic-local)
#   ARTIFACTORY_IMAGE  Pro image (self bring-up)(default: releases-docker.jfrog.io/jfrog/artifactory-pro:latest)
#   POSTGRES_IMAGE     (self bring-up)          (default: postgres:14)
#   BUILD_NAME         TeamCity build-type id     (default: e2e_Publish)
#
# Credentials - REQUIRED, no defaults (the CI workflow maps these from GitHub
# repository secrets; the script fails fast if any are missing):
#   ARTIFACTORY_USER / ARTIFACTORY_PASSWORD  runtime creds the plugin uses
#   ART_ADMIN_USER   / ART_ADMIN_PW          bootstrap admin for a spun-up container
#                                            (must match the image default admin creds)
#   TC_ADMIN_USER    / TC_ADMIN_PASSWORD     TeamCity admin on the throwaway server
#   ARTIFACTORY_LICENSE                      Artifactory Pro license (RTLIC), optional
#
set -euo pipefail

# --- Configuration ----------------------------------------------------------
TEAMCITY_IMAGE="${TEAMCITY_IMAGE:-jetbrains/teamcity-server:2025.11}"
TEAMCITY_AGENT_IMAGE="${TEAMCITY_AGENT_IMAGE:-jetbrains/teamcity-agent:2025.11}"
TC_PORT="${TC_PORT:-8111}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-600}"

ARTIFACTORY_IMAGE="${ARTIFACTORY_IMAGE:-releases-docker.jfrog.io/jfrog/artifactory-pro:latest}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:14}"
ARTIFACTORY_REPO="${ARTIFACTORY_REPO:-teamcity-generic-local}"
# TeamCity build-type id created for the test (project id 'e2e' + build 'publish').
BUILD_NAME="${BUILD_NAME:-e2e_Publish}"

# --- Credentials (must come from the environment / CI secrets) --------------
# No inline defaults: these are sourced exclusively from environment variables,
# which the CI workflow maps from GitHub repository secrets. Missing values
# fail fast (see require_env below) rather than silently using weak defaults.
#
#   ARTIFACTORY_USER / ARTIFACTORY_PASSWORD  runtime creds the plugin uses
#   ART_ADMIN_USER   / ART_ADMIN_PW          bootstrap admin for a spun-up container
#   TC_ADMIN_USER    / TC_ADMIN_PASSWORD     TeamCity admin on the throwaway server
#   ARTIFACTORY_LICENSE                      Artifactory Pro license (RTLIC)
ARTIFACTORY_USER="${ARTIFACTORY_USER:-}"
ARTIFACTORY_PASSWORD="${ARTIFACTORY_PASSWORD:-}"
ART_ADMIN_USER="${ART_ADMIN_USER:-}"
ART_ADMIN_PW="${ART_ADMIN_PW:-}"
TC_ADMIN_USER="${TC_ADMIN_USER:-}"
TC_ADMIN_PASSWORD="${TC_ADMIN_PASSWORD:-}"
ARTIFACTORY_LICENSE="${ARTIFACTORY_LICENSE:-}"

RUN_ID="$$"
NET="jfrog-tc-e2e-net-${RUN_ID}"
TC_SERVER="tc-e2e-server-${RUN_ID}"
TC_AGENT="tc-e2e-agent-${RUN_ID}"
ART_CONTAINER="art-e2e-${RUN_ID}"
PG_CONTAINER="art-pg-${RUN_ID}"

BASE_URL="http://localhost:${TC_PORT}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Fail fast if any required credential is missing from the environment. This
# keeps all secrets out of the script and surfaces a misconfigured CI job with a
# clear message instead of confusing auth failures deep in the run.
require_env() {
  local missing=0 name
  for name in "$@"; do
    if [[ -z "${!name:-}" ]]; then
      echo "ERROR: required environment variable '${name}' is not set (provide it via CI secrets)." >&2
      missing=1
    fi
  done
  [[ ${missing} -eq 0 ]] || { echo "Aborting: missing required credentials." >&2; exit 1; }
}

# BYO-Artifactory mode: if ARTIFACTORY_URL is provided we do NOT bring one up,
# and the runtime credentials are also used as the admin/bootstrap credentials.
OWN_ARTIFACTORY=1
if [[ -n "${ARTIFACTORY_URL:-}" ]]; then
  OWN_ARTIFACTORY=0
  ART_ADMIN_USER="${ARTIFACTORY_USER}"
  ART_ADMIN_PW="${ARTIFACTORY_PASSWORD}"
fi

# Runtime + TeamCity admin creds are always required. Bootstrap admin creds are
# only needed when we spin up our own Artifactory (in BYO mode they are derived
# from the runtime creds above).
require_env ARTIFACTORY_USER ARTIFACTORY_PASSWORD TC_ADMIN_USER TC_ADMIN_PASSWORD
if [[ ${OWN_ARTIFACTORY} -eq 1 ]]; then
  require_env ART_ADMIN_USER ART_ADMIN_PW
fi

# --- Locate the plugin zip --------------------------------------------------
PLUGIN_ZIP="${1:-}"
if [[ -z "${PLUGIN_ZIP}" ]]; then
  PLUGIN_ZIP="$(ls -1 "${REPO_ROOT}"/target/teamcity-artifactory-plugin-*.zip 2>/dev/null | head -n1 || true)"
fi
if [[ -z "${PLUGIN_ZIP}" || ! -f "${PLUGIN_ZIP}" ]]; then
  echo "ERROR: plugin zip not found. Build it first with:" >&2
  echo "       mvn -B -DskipTests -pl assembly -am package" >&2
  echo "       (or pass the path as the first argument)" >&2
  exit 1
fi
echo "Using plugin zip:     ${PLUGIN_ZIP}"
echo "Using TeamCity image: ${TEAMCITY_IMAGE}"

# --- Workspace --------------------------------------------------------------
WORK_DIR="$(mktemp -d)"
DATA_DIR="${WORK_DIR}/datadir"
LOGS_DIR="${WORK_DIR}/logs"
COOKIE_JAR="${WORK_DIR}/cookies.txt"
SERVER_LOG="${LOGS_DIR}/teamcity-server.log"
mkdir -p "${DATA_DIR}/plugins" "${DATA_DIR}/config" "${LOGS_DIR}"
cp "${PLUGIN_ZIP}" "${DATA_DIR}/plugins/"
# The TeamCity container runs as uid 1000 (tcuser) and must be able to write to
# the mounted datadir. These are throwaway temp dirs, so make them
# world-writable to avoid "directory is not writeable" failures on CI runners.
# The log dir is NOT bind-mounted (the container writes it owner-only as uid
# 1000, unreadable from the host); we snapshot it via `docker cp` (refresh_log).
chmod -R 777 "${WORK_DIR}"

# --- Cleanup ----------------------------------------------------------------
cleanup() {
  local exit_code=$?
  if [[ ${exit_code} -ne 0 ]]; then
    echo ""
    echo "======================== FAILURE DIAGNOSTICS ========================"
    echo "--- TeamCity server docker logs (tail) ---"
    docker logs --tail 80 "${TC_SERVER}" 2>&1 || true
    echo "--- teamcity-server.log (tail) ---"
    refresh_log
    tail -n 150 "${SERVER_LOG}" 2>/dev/null || true
    if [[ ${OWN_ARTIFACTORY} -eq 1 ]]; then
      echo "--- Artifactory docker logs (tail) ---"
      docker logs --tail 60 "${ART_CONTAINER}" 2>&1 || true
    fi
    echo "====================================================================="
  fi
  docker rm -f "${TC_AGENT}" "${TC_SERVER}" >/dev/null 2>&1 || true
  if [[ ${OWN_ARTIFACTORY} -eq 1 ]]; then
    docker rm -f "${ART_CONTAINER}" "${PG_CONTAINER}" >/dev/null 2>&1 || true
  fi
  docker network rm "${NET}" >/dev/null 2>&1 || true
  # datadir files may be owned by the container user (uid 1000); fall back to
  # sudo if the runner user cannot remove them.
  rm -rf "${WORK_DIR}" 2>/dev/null || sudo rm -rf "${WORK_DIR}" 2>/dev/null || true
}
trap cleanup EXIT

# --- Helpers ----------------------------------------------------------------
# Snapshot the server log out of the container to a host file the runner owns.
# The log dir is deliberately not bind-mounted: the container writes it as uid
# 1000 with owner-only permissions, unreadable from the host on CI runners.
refresh_log() {
  docker cp "${TC_SERVER}:/opt/teamcity/logs/teamcity-server.log" "${SERVER_LOG}" >/dev/null 2>&1 || true
}

mnt_get()   { curl -fsS -c "${COOKIE_JAR}" -b "${COOKIE_JAR}" -L "${BASE_URL}/mnt" 2>/dev/null || true; }
mnt_stage() { mnt_get | grep -oiE "Stage: [A-Z_]+" | head -n1 | awk '{print $2}'; }

mnt_do() {
  local cmd="$1"; shift
  local resp
  resp="$(curl -fsS -c "${COOKIE_JAR}" -b "${COOKIE_JAR}" -X POST "${BASE_URL}/mnt/do/${cmd}" "$@" 2>/dev/null || true)"
  if [[ "${resp}" != "OK" ]]; then
    echo "ERROR: maintenance command '${cmd}' did not return OK (got: '${resp}')" >&2
    return 1
  fi
}

wait_for_stage() {
  local want="$1" deadline=$(( $(date +%s) + STARTUP_TIMEOUT )) stage=""
  while [[ $(date +%s) -lt ${deadline} ]]; do
    stage="$(mnt_stage || true)"
    if [[ -n "${stage}" ]] && echo "${stage}" | grep -qiE "${want}"; then
      echo "  -> reached stage: ${stage}"; return 0
    fi
    sleep 5
  done
  echo "ERROR: timed out waiting for stage matching '${want}' (last stage: '${stage}')" >&2
  return 1
}

wait_for_log() {
  local pattern="$1" deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
  while [[ $(date +%s) -lt ${deadline} ]]; do
    refresh_log
    if [[ -f "${SERVER_LOG}" ]] && grep -qE "${pattern}" "${SERVER_LOG}"; then return 0; fi
    sleep 5
  done
  echo "ERROR: timed out waiting for log pattern: ${pattern}" >&2
  return 1
}

# authenticated curl against TeamCity REST as the admin we create
tc_admin() { curl -fsS -u "${TC_ADMIN_USER}:${TC_ADMIN_PASSWORD}" -H "Accept: application/json" -H "Content-Type: application/json" "$@"; }
# authenticated curl against Artifactory REST
art_curl() { curl -fsS -u "${ARTIFACTORY_USER}:${ARTIFACTORY_PASSWORD}" "$@"; }

# --- Docker network ---------------------------------------------------------
docker network create "${NET}" >/dev/null 2>&1 || true

# --- Stage 1: Artifactory ---------------------------------------------------
if [[ ${OWN_ARTIFACTORY} -eq 1 ]]; then
  echo ""
  echo ">>> Bringing up PostgreSQL for Artifactory..."
  docker run -d --name "${PG_CONTAINER}" --network "${NET}" \
    -e POSTGRES_DB=artifactory \
    -e POSTGRES_USER=artifactory \
    -e POSTGRES_PASSWORD=password \
    "${POSTGRES_IMAGE}" >/dev/null

  echo ">>> Bringing up Artifactory (${ARTIFACTORY_IMAGE})..."
  ART_MASTER_KEY="$(openssl rand -hex 32)"
  docker run -d --name "${ART_CONTAINER}" --network "${NET}" \
    -e JF_SHARED_SECURITY_MASTERKEY="${ART_MASTER_KEY}" \
    -e JF_SHARED_DATABASE_TYPE=postgresql \
    -e JF_SHARED_DATABASE_DRIVER=org.postgresql.Driver \
    -e JF_SHARED_DATABASE_URL="jdbc:postgresql://${PG_CONTAINER}:5432/artifactory" \
    -e JF_SHARED_DATABASE_USERNAME=artifactory \
    -e JF_SHARED_DATABASE_PASSWORD=password \
    -p 8082:8082 -p 8081:8081 \
    "${ARTIFACTORY_IMAGE}" >/dev/null

  # Host-side URL (verification / repo creation) and in-network URL (used by
  # TeamCity server + agent containers).
  ARTIFACTORY_URL="http://localhost:8082/artifactory"
  ARTIFACTORY_NET_URL="http://${ART_CONTAINER}:8082/artifactory"

  echo ">>> Waiting for Artifactory to become ready (this can take a few minutes)..."
  deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
  until [[ "$(curl -s -o /dev/null -w '%{http_code}' "${ARTIFACTORY_URL}/api/system/ping" 2>/dev/null)" == "200" ]]; do
    [[ $(date +%s) -lt ${deadline} ]] || { echo "ERROR: Artifactory did not become ready" >&2; docker logs --tail 60 "${ART_CONTAINER}" || true; exit 1; }
    sleep 6
  done
  echo "  -> Artifactory is up."
else
  ARTIFACTORY_URL="${ARTIFACTORY_URL%/}"
  ARTIFACTORY_NET_URL="${ARTIFACTORY_URL}"
  echo ">>> Using existing Artifactory: ${ARTIFACTORY_URL}"
fi

# A bare /api/system/ping can return 200 before the repository/access services
# are ready to authenticate and serve, so poll the authenticated repositories
# endpoint (as the admin/bootstrap user) before configuring anything.
echo ">>> Waiting for the Artifactory REST API to accept authenticated requests..."
deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
api_code=""
while [[ $(date +%s) -lt ${deadline} ]]; do
  api_code="$(curl -s -o /dev/null -w '%{http_code}' -u "${ART_ADMIN_USER}:${ART_ADMIN_PW}" "${ARTIFACTORY_URL}/api/repositories" 2>/dev/null || echo 000)"
  [[ "${api_code}" == "200" ]] && break
  if [[ "${api_code}" == "401" || "${api_code}" == "403" ]]; then
    echo "ERROR: Artifactory rejected admin credentials for user '${ART_ADMIN_USER}' (HTTP ${api_code})." >&2
    exit 1
  fi
  sleep 5
done
[[ "${api_code}" == "200" ]] || { echo "ERROR: Artifactory REST API not ready (last HTTP ${api_code})." >&2; exit 1; }
echo "  -> REST API ready."

# Apply the Pro license (RTLIC) to a spun-up container so Pro REST features
# (e.g. repository creation) are available. No-op for BYO Artifactory.
if [[ ${OWN_ARTIFACTORY} -eq 1 && -n "${ARTIFACTORY_LICENSE}" ]]; then
  echo ">>> Applying Artifactory Pro license..."
  lic_payload="${WORK_DIR}/license.json"
  # Safely JSON-encode the license key (may contain newlines/special chars).
  if command -v python3 >/dev/null 2>&1; then
    ARTIFACTORY_LICENSE="${ARTIFACTORY_LICENSE}" python3 - "${lic_payload}" <<'PY'
import json, os, sys
with open(sys.argv[1], "w") as f:
    f.write(json.dumps({"licenseKey": os.environ["ARTIFACTORY_LICENSE"]}))
PY
  else
    printf '{"licenseKey":"%s"}' "$(printf '%s' "${ARTIFACTORY_LICENSE}" | tr -d '\r' | sed ':a;N;$!ba;s/\n/\\n/g')" > "${lic_payload}"
  fi
  lic_resp="${WORK_DIR}/license-resp.txt"
  lic_code="$(curl -s -o "${lic_resp}" -w '%{http_code}' -u "${ART_ADMIN_USER}:${ART_ADMIN_PW}" \
    -X POST "${ARTIFACTORY_URL}/api/system/licenses" \
    -H "Content-Type: application/json" -d @"${lic_payload}" 2>/dev/null || echo 000)"
  if [[ "${lic_code}" == "200" ]] || grep -qi "successfully\|already" "${lic_resp}" 2>/dev/null; then
    echo "  -> license applied (HTTP ${lic_code}): $(head -c 160 "${lic_resp}" 2>/dev/null)"
  else
    echo "  -> WARNING: license not applied (HTTP ${lic_code}): $(head -c 200 "${lic_resp}" 2>/dev/null)" >&2
    echo "     Continuing; will fall back to an existing generic repo if creation is blocked." >&2
  fi
fi

# Ensure a generic local repository to publish into.
#   * On Artifactory Pro, PUT /api/repositories/{key} creates a dedicated repo.
#   * On Artifactory OSS, that REST API is disabled ("available only in
#     Artifactory Pro"), so we fall back to an existing generic local repo
#     (OSS ships with 'example-repo-local').
echo ">>> Ensuring a generic local repository is available (target: '${ARTIFACTORY_REPO}')..."
repo_body="${WORK_DIR}/repo-resp.txt"
create_code="$(curl -s -o "${repo_body}" -w '%{http_code}' -u "${ART_ADMIN_USER}:${ART_ADMIN_PW}" \
  -X PUT "${ARTIFACTORY_URL}/api/repositories/${ARTIFACTORY_REPO}" \
  -H "Content-Type: application/json" \
  -d '{"rclass":"local","packageType":"generic"}' 2>/dev/null || echo 000)"

if [[ "${create_code}" == "200" || "${create_code}" == "201" ]]; then
  echo "  -> created repository '${ARTIFACTORY_REPO}' (HTTP ${create_code})."
elif grep -qi "already exists" "${repo_body}" 2>/dev/null; then
  echo "  -> repository '${ARTIFACTORY_REPO}' already exists."
else
  echo "  -> could not create repo (HTTP ${create_code}: $(head -c 160 "${repo_body}" 2>/dev/null))."
  echo "  -> looking for an existing generic local repository to reuse..."
  repos_json="${WORK_DIR}/repos.json"
  curl -s -u "${ART_ADMIN_USER}:${ART_ADMIN_PW}" \
    "${ARTIFACTORY_URL}/api/repositories?type=local&packageType=generic" -o "${repos_json}" 2>/dev/null || true
  existing="$(grep -oE '"key"[[:space:]]*:[[:space:]]*"[^"]+"' "${repos_json}" 2>/dev/null | head -1 | sed -E 's/.*"([^"]+)"$/\1/')"
  if [[ -n "${existing}" ]]; then
    ARTIFACTORY_REPO="${existing}"
    echo "  -> reusing existing generic local repository '${ARTIFACTORY_REPO}'."
  else
    echo "ERROR: could not create a repository and no existing generic local repo was found." >&2
    echo "       Repo creation via REST needs Artifactory Pro (check the RTLIC license)." >&2
    echo "       Alternatively set ARTIFACTORY_REPO to an existing generic repo." >&2
    exit 1
  fi
fi

# Ensure the runtime user (USER_NAME/PASSWORD) exists as an admin, so the plugin
# can authenticate with those credentials. Only needed for a spun-up container
# where the runtime creds differ from the admin/bootstrap creds.
if [[ ${OWN_ARTIFACTORY} -eq 1 && ( "${ARTIFACTORY_USER}" != "${ART_ADMIN_USER}" || "${ARTIFACTORY_PASSWORD}" != "${ART_ADMIN_PW}" ) ]]; then
  echo ">>> Ensuring runtime user '${ARTIFACTORY_USER}' exists (admin)..."
  user_payload="{\"name\":\"${ARTIFACTORY_USER}\",\"email\":\"${ARTIFACTORY_USER}@e2e.local\",\"password\":\"${ARTIFACTORY_PASSWORD}\",\"admin\":true}"
  # PUT creates (or replaces); POST updates an existing user's password. Do both
  # so it works whether or not the user already exists (e.g. 'admin').
  curl -s -o /dev/null -u "${ART_ADMIN_USER}:${ART_ADMIN_PW}" \
    -X PUT "${ARTIFACTORY_URL}/api/security/users/${ARTIFACTORY_USER}" \
    -H "Content-Type: application/json" -d "${user_payload}" 2>/dev/null || true
  curl -s -o /dev/null -u "${ART_ADMIN_USER}:${ART_ADMIN_PW}" \
    -X POST "${ARTIFACTORY_URL}/api/security/users/${ARTIFACTORY_USER}" \
    -H "Content-Type: application/json" -d "${user_payload}" 2>/dev/null || true
  # Verify the runtime credentials now authenticate (use an authenticated
  # endpoint; /api/system/ping does not require auth).
  deadline=$(( $(date +%s) + 120 ))
  until [[ "$(curl -s -o /dev/null -w '%{http_code}' -u "${ARTIFACTORY_USER}:${ARTIFACTORY_PASSWORD}" "${ARTIFACTORY_URL}/api/repositories" 2>/dev/null || echo 000)" == "200" ]]; do
    [[ $(date +%s) -lt ${deadline} ]] || { echo "ERROR: runtime user '${ARTIFACTORY_USER}' cannot authenticate." >&2; exit 1; }
    sleep 3
  done
  echo "  -> runtime user ready."
fi

# --- Stage 2: pre-seed plugin config + start TeamCity server ----------------
echo ""
echo ">>> Pre-seeding the Artifactory server connection (config/artifactory-config.xml)..."
cat > "${DATA_DIR}/config/artifactory-config.xml" <<EOF
<serializableServers>
  <serializableServers>
    <serializableServer>
      <id>0</id>
      <url>${ARTIFACTORY_NET_URL}</url>
      <defaultDeployerCredentials>
        <username>${ARTIFACTORY_USER}</username>
        <password>${ARTIFACTORY_PASSWORD}</password>
      </defaultDeployerCredentials>
      <useDifferentResolverCredentials>false</useDifferentResolverCredentials>
      <defaultResolverCredentials><username></username><password></password></defaultResolverCredentials>
      <timeout>300</timeout>
    </serializableServer>
  </serializableServers>
</serializableServers>
EOF

echo ">>> Starting TeamCity server container..."
docker run -d --name "${TC_SERVER}" --network "${NET}" \
  -e TEAMCITY_SERVER_MEM_OPTS="-Xmx1g -XX:MaxMetaspaceSize=400m" \
  -v "${DATA_DIR}:/data/teamcity_server/datadir" \
  -p "${TC_PORT}:8111" \
  "${TEAMCITY_IMAGE}" >/dev/null

echo ">>> Waiting for the server web endpoint to respond..."
deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
until curl -fsS -o /dev/null "${BASE_URL}/mnt" 2>/dev/null; do
  [[ $(date +%s) -lt ${deadline} ]] || { echo "ERROR: server did not become reachable" >&2; exit 1; }
  sleep 5
done
curl -fsS -c "${COOKIE_JAR}" -b "${COOKIE_JAR}" -o /dev/null "${BASE_URL}/" 2>/dev/null || true
curl -fsS -c "${COOKIE_JAR}" -b "${COOKIE_JAR}" -o /dev/null "${BASE_URL}/mnt" 2>/dev/null || true

# --- Stage 3: first-run wizard ---------------------------------------------
echo ""
echo ">>> Confirming first start..."
wait_for_stage "FIRST_START_SCREEN"
mnt_do "goNewInstallation" -d "restore=false"

echo ">>> Selecting internal (HSQLDB) database..."
wait_for_stage "DB_SETTINGS_SCREEN"
mnt_do "goNewDatabase" -d "dbType=HSQLDB2"

echo ">>> Accepting license agreement..."
wait_for_stage "LICENSE_AGREEMENT_SCREEN"
mnt_do "acceptLicenseAgreement"

echo ">>> Waiting for plugin initialization to complete..."
wait_for_log "Plugins initialization completed"

# --- Stage 4: create admin via the superuser token --------------------------
echo ""
echo ">>> Waiting for the superuser authentication token in the log..."
SU_TOKEN=""
deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
while [[ $(date +%s) -lt ${deadline} ]]; do
  refresh_log
  SU_TOKEN="$(grep -aE "Super user authentication token" "${SERVER_LOG}" 2>/dev/null | grep -oE '[0-9]{6,}' | tail -1 || true)"
  [[ -n "${SU_TOKEN}" ]] && break
  sleep 5
done
[[ -n "${SU_TOKEN}" ]] || { echo "ERROR: could not find superuser token in server log" >&2; exit 1; }

echo ">>> Creating admin user '${TC_ADMIN_USER}'..."
create_admin() {
  curl -fsS -u ":${SU_TOKEN}" -H "Accept: application/json" -H "Content-Type: application/json" \
    -X POST "${BASE_URL}/app/rest/users" \
    -d "{\"username\":\"${TC_ADMIN_USER}\",\"password\":\"${TC_ADMIN_PASSWORD}\",\"roles\":{\"role\":[{\"roleId\":\"SYSTEM_ADMIN\",\"scope\":\"g\"}]}}" >/dev/null
}
# REST may not be immediately ready right after init; retry briefly.
for i in $(seq 1 20); do
  if create_admin 2>/dev/null; then echo "  -> admin created."; break; fi
  [[ $i -eq 20 ]] && { echo "ERROR: failed to create admin user via REST" >&2; exit 1; }
  sleep 5
done

# --- Stage 5: agent bring-up + authorization --------------------------------
echo ""
echo ">>> Starting TeamCity agent container..."
docker run -d --name "${TC_AGENT}" --network "${NET}" \
  -e SERVER_URL="http://${TC_SERVER}:8111" \
  -e TEAMCITY_AGENT_MEM_OPTS="-Xmx512m" \
  "${TEAMCITY_AGENT_IMAGE}" >/dev/null

echo ">>> Waiting for the agent to register..."
deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
until tc_admin "${BASE_URL}/app/rest/agents?locator=authorized:any" 2>/dev/null | grep -qiE "\"count\":[1-9]"; do
  [[ $(date +%s) -lt ${deadline} ]] || { echo "ERROR: agent did not register" >&2; exit 1; }
  sleep 5
done

echo ">>> Authorizing the agent..."
tc_admin -X PUT "${BASE_URL}/app/rest/agents/id:1/authorizedInfo" \
  -d '{"status":true,"comment":{"text":"e2e auto"}}' >/dev/null

echo ">>> Waiting for the agent to be connected + authorized..."
deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
until tc_admin "${BASE_URL}/app/rest/agents?locator=authorized:true,connected:true" 2>/dev/null | grep -qiE "\"count\":[1-9]"; do
  [[ $(date +%s) -lt ${deadline} ]] || { echo "ERROR: agent never became connected+authorized" >&2; exit 1; }
  sleep 5
done
echo "  -> agent ready."

# --- Stage 6: project + build config + step ---------------------------------
echo ""
echo ">>> Creating project + build configuration..."
tc_admin -X POST "${BASE_URL}/app/rest/projects" -d '{"name":"e2e","id":"e2e"}' >/dev/null
tc_admin -X POST "${BASE_URL}/app/rest/buildTypes" -d '{"name":"publish","project":{"id":"e2e"}}' >/dev/null

STEP_JSON="${WORK_DIR}/step.json"
cat > "${STEP_JSON}" <<EOF
{
  "name": "publish-step",
  "type": "simpleRunner",
  "properties": {
    "property": [
      { "name": "use.custom.script", "value": "true" },
      { "name": "script.content", "value": "echo hello-e2e > artifact.txt\ncat artifact.txt" },
      { "name": "teamcity.step.mode", "value": "default" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.urlId", "value": "0" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.url", "value": "${ARTIFACTORY_NET_URL}" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.timeout", "value": "300" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.useSpecs", "value": "true" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.uploadSpec", "value": "{\"files\":[{\"pattern\":\"artifact.txt\",\"target\":\"${ARTIFACTORY_REPO}/e2e/\"}]}" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.deployArtifacts", "value": "true" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.publishBuildInfo", "value": "true" },
      { "name": "org.jfrog.artifactory.selectedDeployableServer.overrideDefaultDeployerCredentials", "value": "false" }
    ]
  }
}
EOF

echo ">>> Adding the Artifactory publish step..."
tc_admin -X POST "${BASE_URL}/app/rest/buildTypes/id:${BUILD_NAME}/steps" --data-binary @"${STEP_JSON}" >/dev/null

# --- Stage 7: run the build -------------------------------------------------
echo ""
echo ">>> Triggering the build..."
RESP="$(tc_admin -X POST "${BASE_URL}/app/rest/buildQueue" -d "{\"buildType\":{\"id\":\"${BUILD_NAME}\"}}")"
BID="$(printf '%s' "${RESP}" | grep -oE '"id":[0-9]+' | head -1 | grep -oE '[0-9]+' || true)"
[[ -n "${BID}" ]] || { echo "ERROR: could not determine queued build id. Response: ${RESP}" >&2; exit 1; }
echo "  -> queued build id: ${BID}"

echo ">>> Waiting for the build to finish..."
STATE="" STATUS=""
deadline=$(( $(date +%s) + STARTUP_TIMEOUT ))
# NOTE: keep these extractions tolerant of empty responses (a queued build's
# id may 404 on /builds until it starts running) and of grep exit codes; under
# 'set -euo pipefail' an unguarded failing pipeline would abort the whole loop.
while [[ $(date +%s) -lt ${deadline} ]]; do
  INFO="$(tc_admin "${BASE_URL}/app/rest/builds/id:${BID}" 2>/dev/null || true)"
  STATE="$(printf '%s' "${INFO}" | grep -oE -m1 '"state":"[a-z]+"' | cut -d'"' -f4 || true)"
  STATUS="$(printf '%s' "${INFO}" | grep -oE -m1 '"status":"[A-Z]+"' | cut -d'"' -f4 || true)"
  echo "  [build ${BID}] state=${STATE:-queued?} status=${STATUS:-?}"
  [[ "${STATE}" == "finished" ]] && break
  sleep 6
done

# --- Stage 8: assertions ----------------------------------------------------
echo ""
echo ">>> Verifying results..."
FAILED=0

if [[ "${STATE}" != "finished" ]]; then
  echo "FAIL: build did not finish within the timeout." >&2
  FAILED=1
elif [[ "${STATUS}" != "SUCCESS" ]]; then
  echo "FAIL: build finished with status '${STATUS}' (expected SUCCESS)." >&2
  BUILD_LOG="${WORK_DIR}/build.log"
  tc_admin "${BASE_URL}/downloadBuildLog.html?buildId=${BID}" >"${BUILD_LOG}" 2>/dev/null || true
  echo "----- artifactory/jfrog lines from build log -----" >&2
  grep -inE "artifactory|jfrog|spec|deploy|upload|unauthorized|forbidden|not found|40[0-9]|could not|exception" "${BUILD_LOG}" 2>/dev/null | tail -n 50 >&2 || true
  echo "----- build log (tail) -----" >&2
  tail -n 50 "${BUILD_LOG}" 2>/dev/null >&2 || true
  FAILED=1
else
  echo "PASS: build finished with status SUCCESS."
fi

# Artifact must exist in Artifactory.
ART_CODE="$(art_curl -o /dev/null -w '%{http_code}' "${ARTIFACTORY_URL}/api/storage/${ARTIFACTORY_REPO}/e2e/artifact.txt" 2>/dev/null || echo 000)"
if [[ "${ART_CODE}" == "200" ]]; then
  echo "PASS: artifact published -> ${ARTIFACTORY_REPO}/e2e/artifact.txt"
else
  echo "FAIL: artifact not found in Artifactory (HTTP ${ART_CODE})." >&2
  FAILED=1
fi

# Build-info must exist in Artifactory.
BI_CODE="$(art_curl -o /dev/null -w '%{http_code}' "${ARTIFACTORY_URL}/api/build/${BUILD_NAME}" 2>/dev/null || echo 000)"
if [[ "${BI_CODE}" == "200" ]]; then
  echo "PASS: build-info published -> ${BUILD_NAME}"
else
  echo "FAIL: build-info not found in Artifactory (HTTP ${BI_CODE})." >&2
  FAILED=1
fi

echo ""
echo ">>> TeamCity server JVM:"
refresh_log
grep -oE "Java: [0-9][^;]*" "${SERVER_LOG}" | head -n1 || true

echo ""
if [[ ${FAILED} -ne 0 ]]; then
  echo "E2E build-and-publish test: FAILED"
  exit 1
fi
echo "E2E build-and-publish test: PASSED"
