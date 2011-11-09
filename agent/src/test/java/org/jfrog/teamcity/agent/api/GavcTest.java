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

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Noam Y. Tenne
 */
@Test
public class GavcTest {

    public void testDefaultConstructor() {
        Gavc gavc = new Gavc();
        assertNull(gavc.artifactId, "default artifact ID should be null.");
        assertNull(gavc.classifier, "default classifier should be null.");
        assertNull(gavc.groupId, "default group ID should be null.");
        assertNull(gavc.type, "default type should be null.");
        assertNull(gavc.version, "default version should be null.");
        assertFalse(gavc.isValid(), "GAVC object constructed by default constructor should not be valid.");
    }

    public void testCopyConstructorWithDefaultCopy() {
        Gavc toCopy = new Gavc();
        Gavc copy = new Gavc(toCopy);
        assertNull(copy.artifactId, "Artifact ID should be null.");
        assertNull(copy.classifier, "Classifier should be null.");
        assertNull(copy.groupId, "Default group ID should be null.");
        assertNull(copy.type, "Default type should be null.");
        assertNull(copy.version, "Default version should be null.");
        assertFalse(copy.isValid(), "GAVC object constructed by a copy of a default should not be valid.");
    }

    public void testCopyConstructorWithValidCopy() {
        Gavc toCopy = new Gavc();
        toCopy.artifactId = "artifactId";
        toCopy.classifier = "classifier";
        toCopy.groupId = "groupId";
        toCopy.type = "type";
        toCopy.version = "version";
        assertTrue(toCopy.isValid(), "Object should be valid.");

        Gavc copy = new Gavc(toCopy);

        assertEquals(copy.artifactId, toCopy.artifactId, "Unexpected artifact ID.");
        assertEquals(copy.classifier, toCopy.classifier, "Unexpected classifier.");
        assertEquals(copy.groupId, toCopy.groupId, "Unexpected group ID.");
        assertEquals(copy.type, toCopy.type, "Unexpected type.");
        assertEquals(copy.version, toCopy.version, "Unexpected version.");
        assertTrue(copy.isValid(), "Copy of valid object should be valid.");
    }

    public void testValidityOnlyArtifactId() {
        Gavc gavc = new Gavc();
        gavc.artifactId = "artifactId";
        assertFalse(gavc.isValid(), "GAVC should not be valid.");
    }

    public void testValidityOnlyGroupId() {
        Gavc gavc = new Gavc();
        gavc.groupId = "groupId";
        assertFalse(gavc.isValid(), "GAVC should not be valid.");
    }

    public void testValidityOnlyVersion() {
        Gavc gavc = new Gavc();
        gavc.version = "version";
        assertFalse(gavc.isValid(), "GAVC should not be valid.");
    }

    public void testValidityArtifactIdAndGroupId() {
        Gavc gavc = new Gavc();
        gavc.artifactId = "artifactId";
        gavc.groupId = "groupId";
        assertFalse(gavc.isValid(), "GAVC should not be valid.");
    }

    public void testValidityGroupIdAndVersion() {
        Gavc gavc = new Gavc();
        gavc.groupId = "groupId";
        gavc.version = "version";
        assertFalse(gavc.isValid(), "GAVC should not be valid.");
    }

    public void testValidityVersionAndArtifactId() {
        Gavc gavc = new Gavc();
        gavc.artifactId = "artifactId";
        gavc.version = "version";
        assertFalse(gavc.isValid(), "GAVC should not be valid.");
    }

    public void testValidityAll() {
        Gavc gavc = new Gavc();
        gavc.artifactId = "artifactId";
        gavc.groupId = "groupId";
        gavc.version = "version";
        assertTrue(gavc.isValid(), "GAVC should be valid.");
    }
}