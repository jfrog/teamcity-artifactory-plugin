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

package org.jfrog.teamcity.agent.api;

import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.*;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PatternResultFileSetTest {

    public void testInitDefaultConstructor() {
        PatternResultFileSet fileSet = new PatternResultFileSet();
        assertNull(fileSet.getRepoUri(), "Default repo URI should be null.");
        assertNull(fileSet.getSourcePattern(), "Default source pattern should be null.");
        assertNotNull(fileSet.getFiles(), "Default file set should not be null.");
        assertTrue(fileSet.getFiles().isEmpty(), "Default file set should be empty.");
    }

    public void testInitNoFileSetConstructor() {
        PatternResultFileSet fileSet = new PatternResultFileSet("repoUri", "sourcePattern");
        assertEquals(fileSet.getRepoUri(), "repoUri", "Unexpected repo URI.");
        assertEquals(fileSet.getSourcePattern(), "sourcePattern", "Unexpected source pattern.");
        assertNotNull(fileSet.getFiles(), "Default file set should not be null.");
        assertTrue(fileSet.getFiles().isEmpty(), "Default file set should be empty.");
    }

    public void testInitFullConstructor() {
        Set<String> files = Sets.newHashSet("bob");

        PatternResultFileSet fileSet = new PatternResultFileSet("repoUri", "sourcePattern", files);
        assertEquals(fileSet.getRepoUri(), "repoUri", "Unexpected repo URI.");
        assertEquals(fileSet.getSourcePattern(), "sourcePattern", "Unexpected source pattern.");
        assertEquals(files, fileSet.getFiles(), "Unexpected file set.");
    }

    public void testSetterMethods() {
        Set<String> files = Sets.newHashSet("bob");

        PatternResultFileSet fileSet = new PatternResultFileSet();
        fileSet.setRepoUri("repoUri");
        fileSet.setSourcePattern("sourcePattern");
        fileSet.setFiles(files);

        assertEquals(fileSet.getRepoUri(), "repoUri", "Unexpected repo URI.");
        assertEquals(fileSet.getSourcePattern(), "sourcePattern", "Unexpected source pattern.");
        assertEquals(files, fileSet.getFiles(), "Unexpected file set.");
    }

    public void testAdderMethods() {
        PatternResultFileSet fileSet = new PatternResultFileSet(null, null, null);
        fileSet.addFile("moo");

        assertNotNull(fileSet.getFiles(), "File set should not be null.");
        assertFalse(fileSet.getFiles().isEmpty(), "File set should not be empty.");
        assertEquals(fileSet.getFiles().size(), 1, "Unexpected file set size.");
        assertEquals(fileSet.getFiles().iterator().next(), "moo", "Unexpected file set content.");
    }
}