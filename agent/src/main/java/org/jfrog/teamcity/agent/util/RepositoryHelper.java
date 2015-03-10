package org.jfrog.teamcity.agent.util;

import jetbrains.buildServer.parameters.ValueResolver;

import java.util.Map;

/**
 * @author Aviad Shikloshi
 */
public class RepositoryHelper {

    /**
     * Choose the correct repository we want to use from build view in TeamCity -
     * Either the drop down list or the text box
     *
     * @param dynamicFlagParam   Indicator for the method we chose. true for textbox value, false for option from drop down.
     * @param fromTextParam      Value written in text box.
     * @param fromSelectParam    Option from dropdown.
     * @param runParam           Variable to value map as it was configured in TeamCity view.
     * @param repositoryResolver
     * @return The chosen repository name.
     */
    public static String getRepository(String dynamicFlagParam, String fromTextParam, String fromSelectParam,
                                       Map<String, String> runParam, ValueResolver repositoryResolver) {
        String repository = runParam.get(fromSelectParam);
        if (Boolean.valueOf(runParam.get(dynamicFlagParam))) {
            repository = repositoryResolver.resolve(runParam.get(fromTextParam)).getResult();
        }
        return repository;
    }

}
