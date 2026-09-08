#!/usr/bin/env bash
set -euo pipefail

# Runs the teamcity-artifactory-plugin snapshot flow. Used by
# .github/workflows/snapshot.yml, and can be run directly on a developer
# machine from a checkout of this repo by exporting the same environment
# variables and executing this script.
#
# Expected environment variables:
#   GITHUB_RUN_NUMBER - build/run number used to tag the snapshot release
#                        bundle (set automatically by GitHub Actions; export
#                        manually when running locally, e.g.
#                        GITHUB_RUN_NUMBER=123)
#
# Also relies on (typically already set in the environment / CI job):
#   CI, JFROG_CLI_BUILD_NAME, JFROG_CLI_BUILD_NUMBER, JFROG_CLI_BUILD_PROJECT
#
# Prerequisite: the JFrog CLI ("jf") must already be configured/authenticated
# (e.g. via `jf c add`) before running this script.

jf mvnc \
  --repo-resolve-releases ecosys-teamcity-repos --repo-resolve-snapshots ecosys-teamcity-repos \
  --repo-deploy-releases ecosys-oss-release-local --repo-deploy-snapshots ecosys-oss-snapshot-local

jf audit

# Delete former snapshots to make sure the release bundle will not contain stale artifacts
jf rt del "ecosys-oss-snapshot-local/org/jfrog/teamcity/teamcity-artifactory-plugin/*" --quiet

jf mvn install -U -B

jf rt bag
jf rt bp

jf ds rbc ecosystem-teamcity-artifactory-plugin-snapshot "${GITHUB_RUN_NUMBER}" --spec=./ci/specs/dev-rbc-filespec.json --sign
jf ds rbd ecosystem-teamcity-artifactory-plugin-snapshot "${GITHUB_RUN_NUMBER}" --site="releases.jfrog.io" --sync
