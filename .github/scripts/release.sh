#!/usr/bin/env bash
set -euo pipefail

# Runs the teamcity-artifactory-plugin release flow. Used by
# .github/workflows/release.yml, and can be run directly on a developer
# machine from a checkout of this repo (with an "origin" remote and push
# access) by exporting the same environment variables and executing this
# script.
#
# Expected environment variables:
#   NEXT_VERSION             - version to release (e.g. 5.1.0)
#   NEXT_DEVELOPMENT_VERSION - next development version (e.g. 5.1.x-SNAPSHOT)
#   AUDIT_FAIL                - "true"/"false", passed to `jf audit --fail`
#   JETBRAINS_TOKEN          - Bearer token for the JetBrains Marketplace API
#
# Also relies on (typically already set in the environment / CI job):
#   CI, JFROG_CLI_BUILD_NAME, JFROG_CLI_BUILD_NUMBER, JFROG_CLI_BUILD_PROJECT
#
# Prerequisite: the JFrog CLI ("jf") must already be configured/authenticated
# (e.g. via `jf c add`) before running this script.

git config user.name "jfrog-ecosystem-automation"
git config user.email "eco-system@jfrog.com"

git fetch origin release
git checkout release

# Make sure versions were provided
test -n "$NEXT_VERSION"
test -n "$NEXT_DEVELOPMENT_VERSION"

jf mvnc \
  --repo-resolve-releases ecosys-teamcity-repos --repo-resolve-snapshots ecosys-teamcity-repos \
  --repo-deploy-releases ecosys-oss-release-local --repo-deploy-snapshots ecosys-oss-snapshot-local

git merge origin/master

mvn versions:set -DnewVersion="${NEXT_VERSION}" -B

git commit -am "[artifactory-release] Release version ${NEXT_VERSION} [skipRun]" --allow-empty
git tag "${NEXT_VERSION}"

jf audit --fail="${AUDIT_FAIL}"

jf mvn clean install -U -B

jf rt u --flat=true "target/teamcity-artifactory-plugin-${NEXT_VERSION}.zip" \
  "ecosys-oss-release-local/org/jfrog/teamcity/teamcity-artifactory-plugin/${NEXT_VERSION}/"

jf rt bag
jf rt bce
jf rt bp

jf ds rbc ecosystem-teamcity-artifactory-plugin "$NEXT_VERSION" --spec=./ci/specs/prod-rbc-filespec.json --spec-vars="version=$NEXT_VERSION" --sign
jf ds rbd ecosystem-teamcity-artifactory-plugin "$NEXT_VERSION" --site="releases.jfrog.io" --sync

# Deliberate behavior change: JetBrains Marketplace upload, ported as-is from the
# JFrog Pipelines step (Bearer token sourced from the `jetbrains` integration).
curl -i --header "Authorization:Bearer ${JETBRAINS_TOKEN}" \
  -F pluginId=9082 \
  -F file=@"target/teamcity-artifactory-plugin-${NEXT_VERSION}.zip" \
  https://plugins.jetbrains.com/plugin/uploadPlugin

mvn versions:set -DnewVersion="${NEXT_DEVELOPMENT_VERSION}" -B

git commit -am "[artifactory-release] Next development version [skipRun]"
git push origin release

git checkout master
git merge origin/release
git push origin master
git push origin --tags
