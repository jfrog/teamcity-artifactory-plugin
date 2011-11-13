package org.jfrog.teamcity.agent.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.BuildRunnerContextEx;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.agent.impl.BuildRunnerContextImpl;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Builder class to create {@link ArtifactoryClientConfiguration} from a map of runner parameters. That includes all
 * Build-Info fields, and publisher fields.
 *
 * @author Tomer Cohen
 */
public abstract class ArtifactoryClientConfigurationBuilder {

    private ArtifactoryClientConfigurationBuilder() {
        throw new IllegalAccessError();
    }

    /**
     * Create an {@link ArtifactoryClientConfiguration} object out of the TeamCity running parameters.
     *
     * @param runnerContext@return The populated property object from TeamCity values.X
     */
    public static ArtifactoryClientConfiguration create(BuildRunnerContext runnerContext) {
        ArtifactoryClientConfiguration clientConf = new ArtifactoryClientConfiguration(new NullLog());
        Map<String, String> runnerParameters = runnerContext.getRunnerParameters();
        String buildName = runnerParameters.get(ConstantValues.BUILD_NAME);
        clientConf.info.setBuildName(buildName);
        clientConf.publisher.addMatrixParam("build.name", buildName);

        String buildNumber = runnerParameters.get(ConstantValues.BUILD_NUMBER);
        clientConf.info.setBuildNumber(buildNumber);
        clientConf.publisher.addMatrixParam("build.number", buildNumber);

        String buildTimestamp = runnerParameters.get(ConstantValues.PROP_BUILD_TIMESTAMP);
        clientConf.info.setBuildTimestamp(buildTimestamp);
        clientConf.publisher.addMatrixParam("build.timestamp", buildTimestamp);

        String vcsRevision = runnerParameters.get(ConstantValues.PROP_VCS_REVISION);
        clientConf.info.setVcsRevision(vcsRevision);
        clientConf.publisher.addMatrixParam(BuildInfoFields.VCS_REVISION, vcsRevision);
        clientConf.info.setBuildUrl(runnerParameters.get(ConstantValues.BUILD_URL));

        addParentProperties(runnerParameters, clientConf);
        clientConf.info.setPrincipal(runnerParameters.get(ConstantValues.TRIGGERED_BY));
        clientConf.info.setAgentName(runnerParameters.get(ConstantValues.AGENT_NAME));
        clientConf.info.setAgentVersion(runnerParameters.get(ConstantValues.AGENT_VERSION));
        boolean runLicenseChecks =
                Boolean.parseBoolean(runnerParameters.get(RunnerParameterKeys.RUN_LICENSE_CHECKS));
        if (runLicenseChecks) {
            clientConf.info.licenseControl.setRunChecks(runLicenseChecks);
            String violationRecipients = runnerParameters.get(RunnerParameterKeys.LICENSE_VIOLATION_RECIPIENTS);
            if (StringUtils.isNotBlank(violationRecipients)) {
                clientConf.info.licenseControl.setViolationRecipients(violationRecipients);
            }

            String scopes = runnerParameters.get(RunnerParameterKeys.LIMIT_CHECKS_TO_SCOPES);
            if (StringUtils.isNotBlank(scopes)) {
                clientConf.info.licenseControl.setScopes(scopes);
            }

            boolean includePublishedArtifacts =
                    Boolean.parseBoolean(runnerParameters.get(RunnerParameterKeys.INCLUDE_PUBLISHED_ARTIFACTS));
            if (includePublishedArtifacts) {
                clientConf.info.licenseControl.setIncludePublishedArtifacts(includePublishedArtifacts);
            }
            clientConf.info.licenseControl.setAutoDiscover(
                    Boolean.valueOf(runnerParameters.get(RunnerParameterKeys.DISABLE_AUTO_LICENSE_DISCOVERY)));
        }

        addClientProperties(runnerParameters, clientConf);
        clientConf.setIncludeEnvVars(true);
        clientConf.info.fillCommonSysProps();

        addMatrixParamProperties(runnerContext, clientConf);
        addEnvVars(runnerContext, clientConf);
        return clientConf;
    }

    private static void addParentProperties(Map<String, String> runParameters,
            ArtifactoryClientConfiguration clientConf) {
        String parentName = runParameters.get(ConstantValues.PROP_PARENT_NAME);
        if (StringUtils.isNotBlank(parentName)) {
            clientConf.info.setParentBuildName(parentName);
            clientConf.publisher.addMatrixParam(BuildInfoFields.BUILD_PARENT_NAME, parentName);

            String parentNumber = runParameters.get(ConstantValues.PROP_PARENT_NUMBER);
            clientConf.info.setParentBuildNumber(parentNumber);
            clientConf.publisher.addMatrixParam(BuildInfoFields.BUILD_PARENT_NUMBER, parentNumber);
        }
    }

    private static void addClientProperties(Map<String, String> runParameters,
            ArtifactoryClientConfiguration clientConf) {
        clientConf.setContextUrl(runParameters.get(RunnerParameterKeys.URL));
        String timeout = runParameters.get(RunnerParameterKeys.TIMEOUT);
        if (StringUtils.isNotBlank(timeout)) {
            clientConf.setTimeout(Integer.valueOf(timeout));
        }
        clientConf.publisher.setRepoKey(runParameters.get(RunnerParameterKeys.TARGET_REPO));
        String resolvingRepo = runParameters.get(RunnerParameterKeys.RESOLVING_REPO);
        if (StringUtils.isNotBlank(resolvingRepo)) {
            clientConf.resolver.setRepoKey(resolvingRepo);
        }
        clientConf.publisher.setIvy(Boolean.valueOf(runParameters.get(RunnerParameterKeys.PUBLISH_IVY_DESCRIPTORS)));
        clientConf.publisher.setMaven(
                Boolean.valueOf(runParameters.get(RunnerParameterKeys.PUBLISH_MAVEN_DESCRIPTORS)));
        String deployerUsername = runParameters.get(RunnerParameterKeys.DEPLOYER_USERNAME);
        if (StringUtils.isNotBlank(deployerUsername)) {
            clientConf.publisher.setUsername(deployerUsername);
            clientConf.publisher.setPassword(runParameters.get(RunnerParameterKeys.DEPLOYER_PASSWORD));
        }

        boolean deployArtifacts = Boolean.valueOf(runParameters.get(RunnerParameterKeys.DEPLOY_ARTIFACTS));
        clientConf.publisher.setPublishArtifacts(deployArtifacts);
        if (deployArtifacts) {

            String includePatterns = runParameters.get(RunnerParameterKeys.DEPLOY_INCLUDE_PATTERNS);
            if (StringUtils.isNotBlank(includePatterns)) {
                clientConf.publisher.setIncludePatterns(includePatterns);
            }

            String excludePatterns = runParameters.get(RunnerParameterKeys.DEPLOY_EXCLUDE_PATTERNS);
            if (StringUtils.isNotBlank(excludePatterns)) {
                clientConf.publisher.setExcludePatterns(excludePatterns);
            }

            clientConf.publisher.setM2Compatible(
                    Boolean.valueOf(runParameters.get(RunnerParameterKeys.USE_M2_COMPATIBLE_PATTERNS)));

            String ivyPattern = runParameters.get(RunnerParameterKeys.IVY_PATTERNS);
            if (StringUtils.isNotBlank(ivyPattern)) {
                clientConf.publisher.setIvyPattern(ivyPattern);
            }

            String artifactPattern = runParameters.get(RunnerParameterKeys.ARTIFACT_PATTERNS);
            if (StringUtils.isNotBlank(artifactPattern)) {
                clientConf.publisher.setIvyArtifactPattern(artifactPattern);
            }
        }
        clientConf.publisher.setPublishBuildInfo(true);
        clientConf.publisher.setPublishBuildInfo(Boolean.valueOf(
                runParameters.get(RunnerParameterKeys.PUBLISH_BUILD_INFO)));
        String proxyHost = runParameters.get(ConstantValues.PROXY_HOST);
        if (StringUtils.isNotBlank(proxyHost)) {
            clientConf.proxy.setHost(proxyHost);
            clientConf.proxy.setPort(Integer.valueOf(runParameters.get(ConstantValues.PROXY_PORT)));
            String proxyUsername = runParameters.get(ConstantValues.PROXY_USERNAME);
            if (StringUtils.isNotBlank(proxyUsername)) {
                clientConf.proxy.setUsername(proxyUsername);
                clientConf.proxy.setPassword(runParameters.get(ConstantValues.PROXY_PASSWORD));
            }
        }
    }

    private static void addMatrixParamProperties(BuildRunnerContext runnerContext,
            ArtifactoryClientConfiguration clientConf) {
        Properties fileAndSystemProperties =
                BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(new Properties());
        Properties filteredMatrixParams = BuildInfoExtractorUtils
                .filterDynamicProperties(fileAndSystemProperties, BuildInfoExtractorUtils.MATRIX_PARAM_PREDICATE);
        Enumeration<Object> propertyKeys = filteredMatrixParams.keys();
        while (propertyKeys.hasMoreElements()) {
            String key = propertyKeys.nextElement().toString();
            clientConf.publisher.addMatrixParam(key, filteredMatrixParams.getProperty(key));
        }

        HashMap<String, String> allParamMap = Maps.newHashMap(runnerContext.getBuildParameters().getAllParameters());
        allParamMap.putAll(((BuildRunnerContextImpl) runnerContext).getConfigParameters());
        gatherBuildInfoParams(allParamMap, clientConf.publisher, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX,
                Constants.ENV_PREFIX, Constants.SYSTEM_PREFIX);
    }

    private static void gatherBuildInfoParams(Map<String, String> allParamMap,
            ArtifactoryClientConfiguration.PublisherHandler configuration, final String propPrefix,
            final String... propTypes) {
        Map<String, String> filteredProperties = Maps.filterKeys(allParamMap, new Predicate<String>() {
            public boolean apply(String key) {
                if (StringUtils.isNotBlank(key)) {
                    if (key.startsWith(propPrefix)) {
                        return true;
                    }
                    for (String propType : propTypes) {
                        if (key.startsWith(propType + propPrefix)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        filteredProperties = Maps.filterValues(filteredProperties, new Predicate<String>() {
            public boolean apply(String value) {
                return StringUtils.isNotBlank(value);
            }
        });

        for (Map.Entry<String, String> entryToAdd : filteredProperties.entrySet()) {
            String key = entryToAdd.getKey();
            for (String propType : propTypes) {
                key = StringUtils.remove(key, propType);
            }
            key = StringUtils.remove(key, propPrefix);
            configuration.addMatrixParam(key, entryToAdd.getValue());
        }
    }

    private static void addEnvVars(BuildRunnerContext runnerContext, ArtifactoryClientConfiguration clientConf) {
        Map<String, String> allParamMap = Maps.newHashMap();
        allParamMap.putAll(runnerContext.getBuildParameters().getAllParameters());
        allParamMap.putAll(((BuildRunnerContextEx) runnerContext).getConfigParameters());

        for (Map.Entry<String, String> entryToAdd : allParamMap.entrySet()) {
            String key = entryToAdd.getKey();
            if (key.startsWith(Constants.ENV_PREFIX)) {
                key = StringUtils.removeStartIgnoreCase(key, Constants.ENV_PREFIX);
            } else if (key.startsWith(Constants.SYSTEM_PREFIX)) {
                key = StringUtils.removeStartIgnoreCase(key, Constants.SYSTEM_PREFIX);
            }
            clientConf.info.addBuildVariable(key, entryToAdd.getValue());
        }
    }
}
