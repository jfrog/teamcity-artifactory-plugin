package org.jfrog.teamcity.server.project.strategy;

import java.util.List;

public interface NextDevelopmentVersionStrategy {
    List<Integer> apply(String releaseVersion);
}
