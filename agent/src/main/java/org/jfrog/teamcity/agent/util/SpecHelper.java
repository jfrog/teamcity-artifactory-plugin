package org.jfrog.teamcity.agent.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Dima Nevelev on 20/08/2018.
 */
public class SpecHelper {

    public static String getSpecFromFile(String workspace, String path) throws IOException {
        File specFile = new File(path);
        if (!specFile.isAbsolute()) {
            specFile = new File(workspace, path);
        }
        if (!specFile.isFile()) {
            throw new FileNotFoundException("Could not find Spec file at: " + specFile);
        }
        String spec = FileUtils.readFileToString(specFile, "UTF-8");
        if (StringUtils.isBlank(spec)) {
            throw new IllegalArgumentException("The file '" + specFile.getPath() + "' is not a valid File Spec.");
        }
        return spec;
    }
}
