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

import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.Agent;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.BuildAgent;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.TimeZone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Guards the "non-negotiable": the Build-Info JSON produced by the bundled JFrog build-info
 * Java client must stay <b>byte-for-byte identical</b> across client upgrades for a fixed input.
 * <p>
 * The golden resource {@code build-info/expected-build-info.json} was generated with the previous
 * client (build-info 2.38.1) and verified to be byte-identical to the output of the current client
 * (2.43.9). This test rebuilds the exact same deterministic {@link BuildInfo} with whatever client
 * is on the classpath and asserts the serialized bytes match the golden file, so any future
 * build-info bump that changes the serialized schema/format (field order, new non-null fields,
 * timestamp formatting, etc.) fails the build.
 * <p>
 * Note on the 2.38.1 -> 2.43.9 upgrade specifically: 2.43.9 adds an {@code originalDeploymentRepo}
 * field to {@code Artifact}. Because the client serializes with {@code @JsonInclude(NON_NULL)},
 * the new field is omitted when unset and the output remains byte-identical - which this test proves.
 */
public class BuildInfoByteEquivalenceTest {

    private static final String GOLDEN_RESOURCE = "/build-info/expected-build-info.json";

    private TimeZone originalTimeZone;

    @BeforeClass
    public void pinTimeZone() {
        // The 'started' timestamp is formatted with the JVM default time zone at build() time.
        // Pin to UTC so the assertion is stable regardless of the CI machine's time zone.
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public void restoreTimeZone() {
        if (originalTimeZone != null) {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    public void buildInfoJsonIsByteEquivalentToGolden() throws Exception {
        String actual = BuildInfoExtractorUtils.buildInfoToJsonString(buildDeterministicBuildInfo());
        String expected = readGolden();
        assertEquals(actual, expected,
                "Build-Info JSON output changed - the byte-equivalence guarantee is broken. " +
                        "If this change is intentional, regenerate " + GOLDEN_RESOURCE + " and review the diff.");
    }

    /**
     * Builds a deterministic BuildInfo using the same builder API the plugin uses
     * ({@link BuildInfoBuilder}/{@link ModuleBuilder}/{@link ArtifactBuilder}/{@link DependencyBuilder}).
     */
    static BuildInfo buildDeterministicBuildInfo() {
        Artifact art = new ArtifactBuilder("demo-1.0.jar")
                .type("jar")
                .sha1("0123456789abcdef0123456789abcdef01234567")
                .sha256("0000111122223333444455556666777788889999aaaabbbbccccddddeeeeffff")
                .md5("0123456789abcdef0123456789abcdef")
                .remotePath("org/example/demo/1.0/demo-1.0.jar")
                .build();

        Dependency dep = new DependencyBuilder()
                .id("com.google.guava:guava:32.0.1-jre")
                .type("jar")
                .sha1("89abcdef0123456789abcdef0123456789abcdef")
                .sha256("ffffeeeeddddccccbbbbaaaa9999888877776666555544443333222211110000")
                .md5("fedcba9876543210fedcba9876543210")
                .addScope("compile")
                .build();

        Module module = new ModuleBuilder()
                .id("org.example:demo:1.0")
                .repository("libs-release-local")
                .addArtifact(art)
                .addDependency(dep)
                .build();

        BuildInfoBuilder b = new BuildInfoBuilder("teamcity-equiv-fixture")
                .number("42")
                .startedDate(new Date(1600000000000L)) // fixed instant: 2020-09-13T12:26:40Z
                .durationMillis(123456L)
                .url("http://teamcity/viewLog.html?buildId=42")
                .principal("triggeredByUser")
                .artifactoryPrincipal("deployerUser")
                .artifactoryPluginVersion("2.11.x")
                .agent(new Agent("TeamCity", "2025.11"))
                .buildAgent(new BuildAgent("Maven", "3.9.12"))
                .vcsRevision("abc123def456")
                .vcsUrl("https://git.example.com/repo.git")
                .addModule(module);

        b.addProperty("buildInfo.env.ALPHA", "one");
        b.addProperty("buildInfo.env.BETA", "two");
        b.addProperty("buildInfo.env.GAMMA", "three");

        return b.build();
    }

    private String readGolden() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(GOLDEN_RESOURCE)) {
            assertNotNull(in, "Missing golden resource " + GOLDEN_RESOURCE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
