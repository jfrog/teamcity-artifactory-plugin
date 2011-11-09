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

package org.jfrog.build.client;

import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Y. Tenne
 */
@Test
public class DeployDetailsArtifactTest {

    public void testInitWithNullDeployDetails() {
        DeployDetailsArtifact deployDetailsArtifact = new DeployDetailsArtifact(null);
        assertNull(deployDetailsArtifact.getDeployDetails(), "Wrapped deploy details object should be null.");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testInitWithNullDeployDetailsAndGetPath() {
        DeployDetailsArtifact deployDetailsArtifact = new DeployDetailsArtifact(null);
        deployDetailsArtifact.getDeploymentPath();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testInitWithNullDeployDetailsAndGetFile() {
        DeployDetailsArtifact deployDetailsArtifact = new DeployDetailsArtifact(null);
        deployDetailsArtifact.getFile();
    }

    public void testInitWithEmptyDeployDetails() {
        DeployDetails deployDetails = new DeployDetails();

        DeployDetailsArtifact deployDetailsArtifact = new DeployDetailsArtifact(deployDetails);
        assertEquals(deployDetailsArtifact.getDeployDetails(), deployDetails,
                "Wrapped deploy details object should be identical to the given one.");

        assertNull(deployDetailsArtifact.getDeploymentPath(), "Deployment path should not have been set.");
        assertNull(deployDetailsArtifact.getFile(), "File should not have been set.");
    }

    public void testInitWithValidDeployDetails() {
        DeployDetails deployDetails = new DeployDetails();

        deployDetails.artifactPath = "moo";

        File file = new File("/this/is/a/file");
        deployDetails.file = file;

        DeployDetailsArtifact deployDetailsArtifact = new DeployDetailsArtifact(deployDetails);
        assertEquals(deployDetailsArtifact.getDeployDetails(), deployDetails,
                "Wrapped deploy details object should be identical to the given one.");

        assertEquals(deployDetailsArtifact.getDeploymentPath(), "moo",
                "Wrapped deployment path should be identical to the given one.");
        assertEquals(deployDetailsArtifact.getFile(), file, "Wrapped file should be identical to the given one.");
    }
}