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

package org.jfrog.teamcity.common;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.jfrog.teamcity.common.PublishedItemsHelper.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PublishedArtifactsHelperTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRemoveDoubleDotsFromNullPattern() throws Exception {
        removeDoubleDotsFromPattern(null);
    }

    public void testRemoveDoubleDots() {
        testDoubleDotRemover("", "");
        testDoubleDotRemover("a", "a");
        testDoubleDotRemover("a/b", "a/b");
        testDoubleDotRemover("../a/b", "a/b");
        testDoubleDotRemover("a/../b", "a/b");
        testDoubleDotRemover("a/b/..", "a/b");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParsePatternsFromNullProperty() {
        parsePatternsFromProperty(null);
    }

    public void testParsePatternsFromProperty() {
        testParseEmptyPattern("");
        testParseEmptyPattern(" ");
        testParseEmptyPattern(" \n ");
        testParseEmptyPattern(" , ");
        testParseEmptyPattern(" , \n ");

        testParsePattern("a", "a");
        testParsePattern(" a ", "a");
        testParsePattern(" a \n b ", "a", "b");
        testParsePattern(" a , b ", "a", "b");
        testParsePattern(" a , b \n", "a", "b");
        testParsePattern("\n a ,b\n", "a", "b");
    }

    public void testGetPublishedArtifactsPatternPairs() {
        testParseEmptyPatternToMap(null);
        testParseEmptyPatternToMap("");
        testParseEmptyPatternToMap(" ");

        testParseNoTargetPatternToMap("a", "a");
        testParseNoTargetPatternToMap("a,", "a");
        testParseNoTargetPatternToMap("a,\n", "a");
        testParseNoTargetPatternToMap(",a\n", "a");
        testParseNoTargetPatternToMap("\n,a", "a");
        testParseNoTargetPatternToMap("\n,a=>", "a");
        testParseNoTargetPatternToMap("\n,a => ", "a");

        testParseSinglePatternToMap("\n,a => b", "a", "b");
        testParseSinglePatternToMap("\n,a => b ", "a", "b");
        testParseSinglePatternToMap("\n,a => b ,", "a", "b");
        testParseSinglePatternToMap("\n,a => b ,\n", "a", "b");

        Map<String, String> pairs = getPublishedItemsPatternPairs("\n,a => b , d =>c");
        assertTrue(pairs.size() == 2, "Unexpected pattern parser result map size.");
        assertEquals(pairs.get("a"), "b", "Unexpected pattern parser result.");
        assertEquals(pairs.get("d"), "c", "Unexpected pattern parser result.");

        pairs = getPublishedItemsPatternPairs("\n,a => b \n d =>c");
        assertTrue(pairs.size() == 2, "Unexpected pattern parser result map size.");
        assertEquals(pairs.get("a"), "b", "Unexpected pattern parser result.");
        assertEquals(pairs.get("d"), "c", "Unexpected pattern parser result.");

        pairs = getPublishedItemsPatternPairs("\n,a => b ,\n d=> c");
        assertTrue(pairs.size() == 2, "Unexpected pattern parser result map size.");
        assertEquals(pairs.get("a"), "b", "Unexpected pattern parser result.");
        assertEquals(pairs.get("d"), "c", "Unexpected pattern parser result.");

        Map<String, String> triple = getPublishedItemsPatternPairs("\n,a => b , d =>c \n g=>z");
        assertTrue(triple.size() == 3, "Unexpected pattern parser result map size.");
        assertEquals(triple.get("a"), "b", "Unexpected pattern parser result.");
        assertEquals(triple.get("d"), "c", "Unexpected pattern parser result.");
        assertEquals(triple.get("g"), "z", "Unexpected pattern parser result.");

        triple = getPublishedItemsPatternPairs("\n,a => b , d \n g=>");
        assertTrue(triple.size() == 3, "Unexpected pattern parser result map size.");
        assertEquals(triple.get("a"), "b", "Unexpected pattern parser result.");
        assertTrue(StringUtils.isBlank(triple.get("d")), "Unexpected pattern parser result.");
        assertTrue(StringUtils.isBlank(triple.get("g")), "Unexpected pattern parser result.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCalculateTargetPathWithNullRelativePath() {
        calculateTargetPath(null, "");
    }

    public void testCalculateTargetPathWithBlankPattern() {
        File file = new File("moo");
        String relativePath = "a";
        testBlankPatternTargetCalculation(relativePath, null);
        testBlankPatternTargetCalculation(relativePath, "");
        testBlankPatternTargetCalculation(relativePath, " ");
    }

    public void testCalculateTargetPathWithUnixSeparators() {
        String relativePath = "c/d/e/f/g.zip";
        String message = "Unexpected calculated target path";

        testTargetCalculation(relativePath, "momo", "momo/c/d/e/f/g.zip", message);
        testTargetCalculation(relativePath, "momo/*", "momo/g.zip", message);
        testTargetCalculation(relativePath, "momo/*.tar", "momo/g.tar", message);

        testTargetCalculation(relativePath, "**/momo", "c/d/e/f/momo/g.zip", message);
        testTargetCalculation(relativePath, "momo/**/popo", "momo/c/d/e/f/popo/g.zip", message);
        testTargetCalculation(relativePath, "momo/**", "momo/c/d/e/f/g.zip", message);

        testTargetCalculation(relativePath, "**/momo/*", "c/d/e/f/momo/g.zip", message);
        testTargetCalculation(relativePath, "**/momo/*.war", "c/d/e/f/momo/g.war", message);

        testTargetCalculation(relativePath, "momo/**/popo/*", "momo/c/d/e/f/popo/g.zip", message);
        testTargetCalculation(relativePath, "momo/**/popo/*.war", "momo/c/d/e/f/popo/g.war", message);
    }

    public void testCalculateTargetPathWithWindowsSeparators() {
        String relativePath = "c\\d\\e\\f\\g.zip";
        String message = "Unexpected calculated target path";

        testTargetCalculation(relativePath, "momo", "momo/c/d/e/f/g.zip", message);
        testTargetCalculation(relativePath, "momo\\*", "momo/g.zip", message);
        testTargetCalculation(relativePath, "momo\\*.tar", "momo/g.tar", message);

        testTargetCalculation(relativePath, "**\\momo", "c/d/e/f/momo/g.zip", message);
        testTargetCalculation(relativePath, "momo\\**\\popo", "momo/c/d/e/f/popo/g.zip", message);
        testTargetCalculation(relativePath, "momo\\**", "momo/c/d/e/f/g.zip", message);

        testTargetCalculation(relativePath, "**\\momo\\*", "c/d/e/f/momo/g.zip", message);
        testTargetCalculation(relativePath, "**\\momo\\*.war", "c/d/e/f/momo/g.war", message);

        testTargetCalculation(relativePath, "momo\\**\\popo\\*", "momo/c/d/e/f/popo/g.zip", message);
        testTargetCalculation(relativePath, "momo\\**\\popo\\*.war", "momo/c/d/e/f/popo/g.war", message);
    }

    private void testBlankPatternTargetCalculation(String relativePath, String targetPattern) {
        testTargetCalculation(relativePath, targetPattern, relativePath,
                "The relative path should have been returned since the pattern is blank");
    }

    private void testTargetCalculation(String relativePath, String targetPattern, String result, String message) {
        assertEquals(calculateTargetPath(relativePath, targetPattern), result, message);
    }

    private void testDoubleDotRemover(String pattern, String result) {
        assertEquals(removeDoubleDotsFromPattern(pattern), result, "Unexpected pattern double dot remover result.");
    }

    private void testParseEmptyPattern(String emptyPattern) {
        assertTrue(parsePatternsFromProperty(emptyPattern).isEmpty(),
                "Empty pattern should be parsed to an empty list.");
    }

    private void testParsePattern(String patternToParse, String... expectedResults) {
        List<String> patternList = parsePatternsFromProperty(patternToParse);

        assertTrue(patternList.size() == expectedResults.length, "Unexpected pattern parser result list size.");
        for (int i = 0; i < expectedResults.length; i++) {
            assertEquals(patternList.get(i), expectedResults[i], "Unexpected pattern parser result.");
        }
    }

    private void testParseEmptyPatternToMap(String pattern) {
        assertTrue(getPublishedItemsPatternPairs(pattern).isEmpty(),
                "Empty pattern should be parsed to an empty map.");
    }

    private void testParseNoTargetPatternToMap(String pattern, String expectedSource) {
        Map<String, String> pairs = getPublishedItemsPatternPairs(pattern);
        assertTrue(pairs.size() == 1, "Unexpected pattern parser result map size.");
        assertTrue(StringUtils.isBlank(pairs.get(expectedSource)), "Unexpected pattern parser result.");
    }

    private void testParseSinglePatternToMap(String pattern, String expectedSource, String expectedTarget) {
        Map<String, String> pairs = getPublishedItemsPatternPairs(pattern);
        assertTrue(pairs.size() == 1, "Unexpected pattern parser result map size.");
        assertEquals(pairs.get(expectedSource), expectedTarget, "Unexpected pattern parser result.");
    }
}