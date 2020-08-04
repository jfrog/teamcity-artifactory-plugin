package org.jfrog.teamcity.server.project;

import org.jfrog.teamcity.server.project.strategy.NextDevelopmentVersion.StrategyEnum;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.jfrog.teamcity.server.project.strategy.NextDevelopmentVersion.StrategyEnum.DEFAULT;
import static org.jfrog.teamcity.server.project.strategy.NextDevelopmentVersion.StrategyEnum.IGNORE_ZEROS;

public class ReleaseManagementConfigModelTest {
    private final static StrategyEnum UNKNOWN = null;

    private ReleaseManagementConfigModel instance = new ReleaseManagementConfigModel() {
        @Override
        public String getDefaultTagUrl() {
            return null;
        }

        @Override
        public String getDefaultReleaseBranch() {
            return null;
        }
    };

    @DataProvider(name = "data-provider")
    public Object[][] dataProviderMethod() {
        return new Object[][]{
                {UNKNOWN, "6.0.0", "6.0.1-SNAPSHOT"},
                {UNKNOWN, "6.1.0", "6.1.1-SNAPSHOT"},
                {UNKNOWN, "6.0.1", "6.0.2-SNAPSHOT"},
                {UNKNOWN, "6.1.0-0", "6.1.1-SNAPSHOT"},
                {UNKNOWN, "6.1.0-22", "6.1.0-23-SNAPSHOT"},
                {UNKNOWN, "6.1", "6.1.1-SNAPSHOT"},
                {UNKNOWN, "6", "6.0.1-SNAPSHOT"},

                {DEFAULT, "6.0.0", "6.0.1-SNAPSHOT"},
                {DEFAULT, "6.1.0", "6.1.1-SNAPSHOT"},
                {DEFAULT, "6.0.1", "6.0.2-SNAPSHOT"},
                {DEFAULT, "6.1.0-0", "6.1.1-SNAPSHOT"},
                {DEFAULT, "6.1.0-22", "6.1.0-23-SNAPSHOT"},
                {DEFAULT, "6.1", "6.1.1-SNAPSHOT"},
                {DEFAULT, "6", "6.0.1-SNAPSHOT"},

                {IGNORE_ZEROS, "6.0.0", "7.0.0-SNAPSHOT"},
                {IGNORE_ZEROS, "6.1.0", "6.2.0-SNAPSHOT"},
                {IGNORE_ZEROS, "6.0.1", "6.0.2-SNAPSHOT"},
                {IGNORE_ZEROS, "6.1.0-0", "6.2.0-SNAPSHOT"},
                {IGNORE_ZEROS, "6.1.0-22", "6.1.0-23-SNAPSHOT"},
                {IGNORE_ZEROS, "6.1", "6.2.0-SNAPSHOT"},
                {IGNORE_ZEROS, "6", "7.0.0-SNAPSHOT"}};
    }

    @Test(dataProvider = "data-provider")
    public void testGetNextDevelopmentVersion(StrategyEnum strategy, String releaseVersion, String expected) {
        instance.setNextDevelopmentVersionStrategy(strategy);
        instance.setCurrentVersion(releaseVersion);

        final String current = instance.getNextDevelopmentVersion();

        Assert.assertEquals(current, expected);
    }
}