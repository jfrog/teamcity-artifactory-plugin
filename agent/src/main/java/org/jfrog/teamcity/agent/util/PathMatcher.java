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
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * An Ant based path matcher util class.
 *
 * @author Yossi Shaul
 */
public abstract class PathMatcher {
    
    private PathMatcher() {}

    private static AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static final List<String> DEFAULT_EXCLUDES = Lists.newArrayList(
            "**/*~",
            "**/#*#",
            "**/.#*",
            "**/%*%",
            "**/._*",
            "**/CVS",
            "**/CVS/**",
            "**/.cvsignore",
            "**/SCCS",
            "**/SCCS/**",
            "**/vssver.scc",
            "**/.svn",
            "**/.svn/**",
            "**/.DS_Store");

    public static boolean matches(String relativePath, String include) {
        return matches(cleanPath(relativePath), include, DEFAULT_EXCLUDES);
    }

    private static String cleanPath(String relativePath) {
        relativePath = relativePath.replace('\\', '/');
        if (relativePath.startsWith("/") && relativePath.length() > 1) {
            return relativePath.substring(1);
        }
        return relativePath;
    }

    private static boolean matches(String path, String include, List<String> excludes) {
        if (excludes != null && !excludes.isEmpty()) {
            for (String exclude : excludes) {
                boolean match = antPathMatcher.match(exclude, path);
                if (match) {
                    return false;
                }
            }
        }

        if (StringUtils.isNotBlank(include)) {
            boolean match = antPathMatcher.match(include, path);
            if (match) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }
}