## 2.10.2 (March 22, 2023)
-  Improve input validation of fields in Project's Artifactory connection - https://github.com/jfrog/teamcity-artifactory-plugin/pull/138
-  Bug fix - Triggers are not always unique https://github.com/jfrog/teamcity-artifactory-plugin/pull/133

## 2.10.1 (Februar 28, 2023)
- Improve input validation of Artifactory URL - https://github.com/jfrog/teamcity-artifactory-plugin/pull/134

## 2.10.0 (December 16, 2022)
- Adding support of Connections to set up Artifactory URL - https://github.com/jfrog/teamcity-artifactory-plugin/pull/115
- Improve input validation in the Artifactory server configuration - https://github.com/jfrog/teamcity-artifactory-plugin/pull/130
- Fix non-empty repository verification in free text mode - https://github.com/jfrog/teamcity-artifactory-plugin/pull/128
- Support federated repositories - https://github.com/jfrog/teamcity-artifactory-plugin/pull/129
- Update build-info to 2.38.1 / 4.30.1

## 2.9.5 (November 28, 2022) 
- Bug fix - Wrong build-info URL in Artifactory 7 - https://github.com/jfrog/teamcity-artifactory-plugin/pull/124

## 2.9.4 (August 14, 2022) 
- Bug fix - Artifactory deploy proprieties are not saved on deployed artifacts - https://github.com/jfrog/teamcity-artifactory-plugin/pull/116

## 2.9.3 (June 09, 2022) 
- Bug fix - Trigger configuration does not list repositories - https://github.com/jfrog/teamcity-artifactory-plugin/pull/105

## 2.9.1 (June 06, 2021) 
- Bug fix - Artifactory response parsing can fail due to missing JSON properties - https://github.com/jfrog/build-info/pull/502

## 2.9.0 (December 01, 2020) 
- New "Artifactory Docker" build step.

## 2.8.0 (January 03, 2019) 
- Support for TeamCity 2018.
- Allow downloading using file spec which contains "build" only ("pattern" or "aql" are not mandatory).
- Bug fixes.

## 2.7.1 (August 20, 2018) 
- Following changes in TeamCity 2018, the Artifactory plugin was updated to create the build name and build info URL correctly.
- A new fields has been added to the runners configuration, allowing to set the build name in Artifactory.
- Bug fixes.

## 2.6.0 (April 09, 2018) 
- File Specs - File larger than 5mb are now downloaded concurrently.
- Bug fixes.
