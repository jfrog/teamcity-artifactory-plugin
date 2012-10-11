package org.jfrog.teamcity.server.project;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import jetbrains.buildServer.BuildTypeDescriptor;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.vcs.VcsRootInstance;
import jetbrains.buildServer.web.util.SessionUser;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jfrog.teamcity.common.CustomDataStorageKeys;
import org.jfrog.teamcity.common.ReleaseManagementParameterKeys;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class BaseReleaseManagementController extends BaseFormXmlController {

    private ProjectManager projectManager;
    private SBuildServer server;

    public BaseReleaseManagementController(ProjectManager projectManager, SBuildServer server) {
        this.projectManager = projectManager;
        this.server = server;
    }

    @Override
    protected ModelAndView doGet(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response, Element responseElement) {
        String buildTypeId = request.getParameter("buildTypeId");

        ActionErrors errors = new ActionErrors();

        if (StringUtils.isBlank(buildTypeId)) {
            errors.addError("runError",
                    "Unable to find the selected build type ID within the request parameters ('buildTypeId').");
            errors.serialize(responseElement);
            return;
        }

        SBuildType buildType = projectManager.findBuildTypeById(buildTypeId);
        if (buildType == null) {
            errors.addError("runError", "Unable to find the type of the selected build with the ID '" + buildTypeId +
                    "'");
            errors.serialize(responseElement);
            return;
        }

        Map<String, String> customParameters = Maps.newHashMap();
        gatherReleaseManagementParameters(request, buildType, errors, customParameters);
        if (errors.hasErrors()) {
            errors.serialize(responseElement);
            return;
        }

        customParameters.put(ReleaseManagementParameterKeys.MANAGEMENT_ACTIVATED, Boolean.TRUE.toString());

        BuildTypeDescriptor.CheckoutType existingCheckType = buildType.getCheckoutType();
        if (!BuildTypeDescriptor.CheckoutType.ON_AGENT.equals(existingCheckType)) {
            if (buildType.isTemplateBased()) {
                errors.addError("runError", "For Release Management to work the checkout type must be 'On Agent'. Please change the checkout type in template or detach template for automatic change.");
                errors.serialize(responseElement);
                return;
            }
            buildType.setCheckoutType(BuildTypeDescriptor.CheckoutType.ON_AGENT);
            CustomDataStorage checkChangeHistory =
                    buildType.getCustomDataStorage(CustomDataStorageKeys.CHECKOUT_CONFIGURATION_CHANGE_HISTORY);
            checkChangeHistory.putValue(buildTypeId, existingCheckType.name());
        }

        BuildPromotionEx promotion = ((BuildTypeEx) buildType).createBuildPromotion();
        promotion.setCustomParameters(customParameters);
        promotion.addToQueue(SessionUser.getUser(request).getUsername());
        WebLinks webLinks = new WebLinks(server);
        Element redirectUrl = new Element("redirectUrl");
        redirectUrl.setText(webLinks.getConfigurationHomePageUrl(buildType));
        responseElement.addContent(redirectUrl);
    }

    private void gatherReleaseManagementParameters(HttpServletRequest request, SBuildType buildType,
                                                   ActionErrors errors, Map<String, String> customParameters) {
        List<VcsRootInstance> roots = buildType.getVcsRootInstances();
        boolean gitVcs = false;
        boolean svnVcs = false;
        boolean perforceVcs = false;

        for (VcsRootInstance vcsRootInstance : roots) {
            String vcsName = vcsRootInstance.getVcsName();
            gitVcs = "jetbrains.git".equals(vcsName);
            svnVcs = "svn".equals(vcsName);
            perforceVcs = "perforce".equals(vcsName);
            if (gitVcs || svnVcs || perforceVcs) {
                break;
            }
        }

        if (!gitVcs && !svnVcs && !perforceVcs) {
            errors.addError("runError",
                    "Promotion can only be performed on builds configured with a Git, Subversion or Perforce VCS root.");
            return;
        }

        customParameters.put(ReleaseManagementParameterKeys.GIT_VCS, Boolean.toString(gitVcs));
        customParameters.put(ReleaseManagementParameterKeys.SVN_VCS, Boolean.toString(svnVcs));
        customParameters.put(ReleaseManagementParameterKeys.PERFORCE_VCS, Boolean.toString(perforceVcs));

        handleVersioning(request, customParameters, errors);

        if (gitVcs) {
            handleReleaseBranch(request, customParameters, errors);
        }

        handleVcsSettings(request, customParameters, errors);

        customParameters.put(ReleaseManagementParameterKeys.NEXT_DEVELOPMENT_VERSION_COMMENT,
                request.getParameter(ReleaseManagementParameterKeys.NEXT_DEVELOPMENT_VERSION_COMMENT));

        handleStagingSettings(request, customParameters, errors);
    }

    protected abstract void handleVersioning(HttpServletRequest request, Map<String, String> customParameters,
                                             ActionErrors errors);

    private void handleReleaseBranch(HttpServletRequest request, Map<String, String> customParameters,
                                     ActionErrors errors) {
        boolean createReleaseBranch =
                Boolean.valueOf(request.getParameter(ReleaseManagementParameterKeys.CREATE_RELEASE_BRANCH));
        if (createReleaseBranch) {
            customParameters.put(ReleaseManagementParameterKeys.CREATE_RELEASE_BRANCH, Boolean.TRUE.toString());
            String releaseBranch = request.getParameter(ReleaseManagementParameterKeys.RELEASE_BRANCH);
            if (StringUtils.isNotBlank(releaseBranch)) {
                customParameters.put(ReleaseManagementParameterKeys.RELEASE_BRANCH, releaseBranch);
            } else {
                errors.addError("releaseBranchError", "Release branch name is mandatory.");
            }
        }
    }

    private void handleVcsSettings(HttpServletRequest request, Map<String, String> customParameters,
                                   ActionErrors errors) {
        boolean createVcsTag = Boolean.valueOf(request.getParameter(ReleaseManagementParameterKeys.CREATE_VCS_TAG));
        if (createVcsTag) {
            customParameters.put(ReleaseManagementParameterKeys.CREATE_VCS_TAG, Boolean.TRUE.toString());
            String tagUrlOrName = request.getParameter(ReleaseManagementParameterKeys.TAG_URL_OR_NAME);
            if (StringUtils.isNotBlank(tagUrlOrName)) {
                customParameters.put(ReleaseManagementParameterKeys.TAG_URL_OR_NAME, tagUrlOrName);
            } else {
                errors.addError("tagUrlOrNameError", "Tag URL/name is mandatory.");
            }
            customParameters.put(ReleaseManagementParameterKeys.TAG_COMMENT,
                    request.getParameter(ReleaseManagementParameterKeys.TAG_COMMENT));
        }
    }

    private void handleStagingSettings(HttpServletRequest request, Map<String, String> customParameters,
                                       ActionErrors errors) {
        String stagingRepositoryKey = request.getParameter(ReleaseManagementParameterKeys.STAGING_REPOSITORY);
        if (StringUtils.isNotBlank(stagingRepositoryKey)) {
            customParameters.put(ReleaseManagementParameterKeys.STAGING_REPOSITORY, stagingRepositoryKey);
        } else {
            errors.addError("stagingRepositoryError", "Staging repository is mandatory.");
        }

        customParameters.put(ReleaseManagementParameterKeys.STAGING_COMMENT,
                request.getParameter(ReleaseManagementParameterKeys.STAGING_COMMENT));
    }

    protected void handlePerModuleVersion(HttpServletRequest request, Map<String, String> customParameters,
                                          ActionErrors errors) {
        customParameters.put(ReleaseManagementParameterKeys.USE_PER_MODULE_VERSION, Boolean.TRUE.toString());

        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String[]> releaseAndNextDevVersion = Maps.filterKeys(parameterMap, new Predicate<String>() {
            public boolean apply(String input) {
                return input.startsWith(ReleaseManagementParameterKeys.PER_MODULE_RELEASE_VERSION_PREFIX) ||
                        input.startsWith(ReleaseManagementParameterKeys.PER_MODULE_NEXT_DEVELOPMENT_VERSION_PREFIX);
            }
        });

        for (Map.Entry<String, String[]> requestParam : releaseAndNextDevVersion.entrySet()) {
            String parameterName = requestParam.getKey();
            String parameterValue = requestParam.getValue()[0];

            if (StringUtils.isBlank(parameterValue)) {
                StringBuilder errorMessageBuilder = new StringBuilder("The ");
                String propertyNamePrefix;
                if (parameterName.startsWith(ReleaseManagementParameterKeys.PER_MODULE_RELEASE_VERSION_PREFIX)) {
                    propertyNamePrefix = ReleaseManagementParameterKeys.PER_MODULE_RELEASE_VERSION_PREFIX;
                    errorMessageBuilder.append("release version");
                } else {
                    propertyNamePrefix = ReleaseManagementParameterKeys.PER_MODULE_NEXT_DEVELOPMENT_VERSION_PREFIX;
                    errorMessageBuilder.append("next development version");
                }
                errors.addError("perModuleError", errorMessageBuilder.append(" of module '").
                        append(StringUtils.removeStart(parameterName, propertyNamePrefix)).append("' is mandatory.").
                        toString());
                return;
            } else {
                customParameters.put(parameterName, parameterValue);
            }
        }
    }
}