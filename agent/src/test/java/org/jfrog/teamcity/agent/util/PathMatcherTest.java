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

import org.testng.annotations.Test;

import static org.jfrog.teamcity.agent.util.PathMatcher.matches;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PathMatcherTest {

    public static final String DEFAULT_INCLUDES = "a/**/*.jar";

    public void testPatternsAreCleaned() {
        assertTrue(matches("/a/b/d/c.jar", DEFAULT_INCLUDES),
                "Path matcher should have cleaned leading slash.");

        assertTrue(matches("\\a/b/d/c.jar", DEFAULT_INCLUDES),
                "Path matcher should have cleaned leading and backwards slashes.");

        assertTrue(matches("/a\\b/d\\c.jar", DEFAULT_INCLUDES),
                "Path matcher should have cleaned leading and backwards slashes.");

        assertTrue(matches("\\a\\b\\d\\c.jar", DEFAULT_INCLUDES),
                "Path matcher should have cleaned leading and backwards slashes.");
    }
}
