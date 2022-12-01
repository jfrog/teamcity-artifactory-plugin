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

import com.google.common.collect.Lists;
import org.jfrog.build.client.DeployDetailsArtifact;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Y. Tenne
 */
@Test
public class ExtractedBuildInfoTest {

    public void testInitWithNulls() {
        ExtractedBuildInfo extractedBuildInfo = new ExtractedBuildInfo(null, null);
        assertNull(extractedBuildInfo.getBuildInfo(), "Default build info object should be null.");
        assertNull(extractedBuildInfo.getDeployableArtifacts(), "Default deployable artifacts object should be null.");
    }

    public void testInitWithValidValues() {
        BuildInfo buildInfo = new BuildInfo();
        List<DeployDetailsArtifact> deployableArtifacts = Lists.newArrayList();

        ExtractedBuildInfo extractedBuildInfo = new ExtractedBuildInfo(buildInfo, deployableArtifacts);
        assertEquals(extractedBuildInfo.getBuildInfo(), buildInfo,
                "Extracted build info object should be equal to the object passed to the constructor.");
        assertEquals(extractedBuildInfo.getDeployableArtifacts(), deployableArtifacts,
                "Extracted deployable artifacts object should be equal to the object passed to the constructor.");
    }
}
