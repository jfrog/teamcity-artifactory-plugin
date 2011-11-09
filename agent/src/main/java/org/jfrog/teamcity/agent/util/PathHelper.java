/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.teamcity.agent.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public abstract class PathHelper {

    public static Map<String, String> getPublishedArtifactsPatternPairs(String publishedArtifactsPropertyValue) {
        Map<String, String> patternPairMap = Maps.newHashMap();
        if (StringUtils.isNotBlank(publishedArtifactsPropertyValue)) {

            List<String> patternPairs = mappingPatternsFormPath(publishedArtifactsPropertyValue);
            for (String patternPair : patternPairs) {

                String[] splitPattern = patternPair.split("=>");

                String sourcePattern = "";
                String targetPattern = "";

                if (splitPattern.length > 0) {
                    sourcePattern = removeDoubleDotsFromPattern(splitPattern[0].trim().replace('\\', '/'));
                }
                if (splitPattern.length > 1) {
                    targetPattern = removeDoubleDotsFromPattern(splitPattern[1].trim().replace('\\', '/'));
                }

                if (StringUtils.isNotBlank(sourcePattern)) {
                    patternPairMap.put(sourcePattern, targetPattern);
                }
            }
        }
        return patternPairMap;
    }

    public static List<String> mappingPatternsFormPath(String path) {

        List<String> patterns = Lists.newArrayList();

        if (StringUtils.isBlank(path)) {
            return patterns;
        }

        String[] newLineTokens = path.split("\n");
        for (String lineToken : newLineTokens) {

            if (StringUtils.isNotBlank(lineToken)) {
                String[] commaTokens = lineToken.trim().split(",");

                for (String commaToken : commaTokens) {

                    if (StringUtils.isNotBlank(commaToken)) {
                        patterns.add(commaToken.trim());
                    }
                }
            }
        }

        return patterns;
    }

    public static String removeDoubleDotsFromPattern(String pattern) {

        if (pattern == null) {
            throw new IllegalArgumentException("Cannot remove double dots from a null pattern.");
        }

        if (!pattern.contains("..")) {
            return pattern;
        }

        String[] splitPattern = pattern.split("/");

        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < splitPattern.length; i++) {

            if (!"..".equals(splitPattern[i])) {
                patternBuilder.append(splitPattern[i]);
                if (i != (splitPattern.length - 1)) {
                    patternBuilder.append("/");
                }
            }
        }

        return StringUtils.removeEnd(patternBuilder.toString(), "/");
    }

    public static String calculateTargetPath(File file, String relativePath, String targetPattern) {

        if (file == null) {
            throw new IllegalArgumentException("Cannot calculate a target path given a null file.");
        }

        if (relativePath == null) {
            throw new IllegalArgumentException("Cannot calculate a target path given a null relative path.");
        }

        if (StringUtils.isBlank(targetPattern)) {
            return relativePath;
        }
        StringBuilder artifactPathBuilder = new StringBuilder();

        String[] targetTokens = targetPattern.split("/");

        boolean addedRelativeParent = false;
        for (int i = 0; i < targetTokens.length; i++) {

            boolean lastToken = (i == (targetTokens.length - 1));

            String targetToken = targetTokens[i];

            if ("**".equals(targetToken)) {
                if (!lastToken) {
                    String relativeParentPath = StringUtils.remove(relativePath, file.getName());
                    relativeParentPath = StringUtils.removeEnd(relativeParentPath, "/");
                    artifactPathBuilder.append(relativeParentPath);
                    addedRelativeParent = true;
                } else {
                    artifactPathBuilder.append(relativePath);
                }
            } else if (targetToken.startsWith("*.")) {
                String newFileName = FilenameUtils.removeExtension(file.getName()) + targetToken.substring(1);
                artifactPathBuilder.append(newFileName);
            } else if ("*".equals(targetToken)) {
                artifactPathBuilder.append(file.getName());
            } else {
                if (StringUtils.isNotBlank(targetToken)) {
                    artifactPathBuilder.append(targetToken);
                }
                if (lastToken) {
                    if (artifactPathBuilder.length() > 0) {
                        artifactPathBuilder.append("/");
                    }
                    if (addedRelativeParent) {
                        artifactPathBuilder.append(file.getName());
                    } else {
                        artifactPathBuilder.append(relativePath);
                    }
                }
            }

            if (!lastToken) {
                artifactPathBuilder.append("/");
            }
        }
        return artifactPathBuilder.toString();
    }
}