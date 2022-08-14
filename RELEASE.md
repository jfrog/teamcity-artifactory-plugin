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
