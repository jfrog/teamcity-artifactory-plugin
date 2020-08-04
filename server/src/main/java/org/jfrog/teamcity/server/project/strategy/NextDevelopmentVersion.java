package org.jfrog.teamcity.server.project.strategy;

import org.codehaus.mojo.buildhelper.versioning.VersionInformation;
import org.jfrog.teamcity.common.ConstantValues;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class NextDevelopmentVersion {
    private final static String PREFIX = ConstantValues.PLUGIN_PREFIX + "releaseManagement.";
    public final static String PARAMETER_NAME = PREFIX + "nextDevelopmentVersionStrategy";

    public enum StrategyEnum implements NextDevelopmentVersionStrategy {
        DEFAULT(),
        IGNORE_ZEROS();

        public List<Integer> apply(String releaseVersion) {
            final VersionInformation versionInformation = new VersionInformation(releaseVersion);
            final List<Integer> versionParts = new ArrayList<Integer>();

            versionParts.add(versionInformation.getBuildNumber());
            versionParts.add(versionInformation.getPatch());
            versionParts.add(versionInformation.getMinor());
            versionParts.add(versionInformation.getMajor());

            ListIterator<Integer> listIterator = versionParts.listIterator();
            boolean done = false;

            while (!done && listIterator.hasNext()) {
                switch (this) {

                    case IGNORE_ZEROS:
                        done = applyIgnoreZero(listIterator);
                        break;

                    case DEFAULT:
                    default:
                        done = applyDefault(listIterator);
                        break;
                }
            }

            return versionParts;
        }
    }

    private static boolean applyDefault(ListIterator<Integer> i) {
        int versionPart = i.next();
        boolean done = false;

        if (i.previousIndex() + versionPart > 0) {
            i.set(versionPart + 1);
            done = true;
        }

        return done;
    }

    private static boolean applyIgnoreZero(ListIterator<Integer> i) {
        int versionPart = i.next();
        boolean done = false;

        if (versionPart > 0) {
            i.set(versionPart + 1);
            done = true;
        }

        return done;
    }
}
