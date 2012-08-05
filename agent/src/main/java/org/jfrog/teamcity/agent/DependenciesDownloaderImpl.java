package org.jfrog.teamcity.agent;

import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryDependenciesClient;
import org.jfrog.build.util.DependenciesDownloader;
import org.jfrog.build.util.DependenciesDownloaderHelper;
import org.jfrog.teamcity.common.RunnerParameterKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.teamcity.common.ConstantValues.*;

/**
 * Primary implementation of dependencies downloader
 *
 * @author Shay Yaakov
 */
public class DependenciesDownloaderImpl implements DependenciesDownloader {

    private Map<String, String> runnerParams;
    private String serverUrl;
    private Log log;
    private BuildRunnerContext runnerContext;
    private ArtifactoryDependenciesClient client;

    public DependenciesDownloaderImpl(@NotNull BuildRunnerContext runnerContext, Log log) {
        this.runnerContext = runnerContext;
        this.log = log;
        this.runnerParams = runnerContext.getRunnerParameters();
        this.serverUrl = runnerParams.get(RunnerParameterKeys.URL);
        client = newClient();
    }

    public ArtifactoryDependenciesClient getClient() {
        return client;
    }

    public List<Dependency> download(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(this, log);
        return helper.downloadDependencies(downloadableArtifacts);
    }

    public String getTargetDir(String relativeDir) {
        final File targetDirFile = new File(relativeDir);
        final File workingDir = targetDirFile.isAbsolute() ? targetDirFile :
                new File(runnerContext.getWorkingDirectory(), relativeDir);
        return workingDir.getAbsolutePath();
    }

    public Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException {
        File dest = new File(getTargetDir(filePath));
        if (dest.exists()) {
            dest.delete();
            dest.createNewFile();
        } else {
            dest.getParentFile().mkdirs();
            dest.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(dest);
        try {
            IOUtils.copyLarge(is, fileOutputStream);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(fileOutputStream);
        }

        try {
            return FileChecksumCalculator.calculateChecksums(dest, "md5", "sha1");
        } catch (NoSuchAlgorithmException e) {
            log.warn("Could not find checksum algorithm: " + e.getLocalizedMessage());
        }

        return null;
    }

    /**
     * Retrieves Artifactory HTTP client.
     *
     * @return Artifactory HTTP client.
     */
    private ArtifactoryDependenciesClient newClient() {
        ArtifactoryDependenciesClient client =
                new ArtifactoryDependenciesClient(serverUrl,
                        runnerParams.get(RunnerParameterKeys.DEPLOYER_USERNAME),
                        runnerParams.get(RunnerParameterKeys.DEPLOYER_PASSWORD),
                        log);

        client.setConnectionTimeout(Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));

        if (runnerParams.containsKey(PROXY_HOST)) {
            if (StringUtils.isNotBlank(runnerParams.get(PROXY_USERNAME))) {
                client.setProxyConfiguration(runnerParams.get(PROXY_HOST),
                        Integer.parseInt(runnerParams.get(PROXY_PORT)), runnerParams.get(PROXY_USERNAME),
                        runnerParams.get(PROXY_PASSWORD));
            } else {
                client.setProxyConfiguration(runnerParams.get(PROXY_HOST),
                        Integer.parseInt(runnerParams.get(PROXY_PORT)));
            }
        }

        return client;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        client.shutdown();
    }
}
