package org.jfrog.teamcity.agent;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
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
    private boolean flatDownload;

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

    public String getTargetDir(String targetDir, String relativeDir) throws IOException {
        /*
        * In Linux, an empty target directory cause to root destination path.
        */
        if (StringUtils.isBlank(targetDir)) {
            targetDir = runnerContext.getWorkingDirectory().getAbsolutePath();
        }
        if (this.flatDownload) {
            relativeDir = StringUtils.substringAfterLast(relativeDir, "/");
        }

        final File targetDirFile = new File(targetDir, relativeDir);
        final File workingDir = targetDirFile.isAbsolute() ? targetDirFile :
                new File(runnerContext.getWorkingDirectory(), targetDirFile.getPath());
        log.info("Target directory: " + workingDir.getAbsolutePath());

        return workingDir.getAbsolutePath();
    }

    public Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException {
        File dest = new File(filePath);
        if (dest.exists()) {
            dest.delete();
        } else {
            dest.getParentFile().mkdirs();
        }

        FileOutputStream fileOutputStream = null;
        try {
            dest.createNewFile();
            fileOutputStream = new FileOutputStream(dest);
            IOUtils.copyLarge(is, fileOutputStream);
        } catch (IOException e) {
            throw new IOException("Could not create or write to file: " + dest.getCanonicalPath() + ". " +
                e.getMessage());
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

    public boolean isFileExistsLocally(String filePath, String md5, String sha1) throws IOException {
        File dest = new File(filePath);
        if (!dest.exists()) {
            return false;
        }

        // If it's a folder return true since we don't care about it, not going to download a folder anyway
        if (dest.isDirectory()) {
            throw new IOException(String.format("File can't override an existing directory: %s", dest.toString()));
        }

        try {
            Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(dest, "md5", "sha1");
            boolean isExists = checksumsMap != null &&
                    StringUtils.isNotBlank(md5) && StringUtils.equals(md5, checksumsMap.get("md5")) &&
                    StringUtils.isNotBlank(sha1) && StringUtils.equals(sha1, checksumsMap.get("sha1"));
            if (isExists) {
                return true;
            } else {
                log.info(String.format("Overriding existing in destination file: %s", dest.toString()));
                return false;
            }

        } catch (NoSuchAlgorithmException e) {
            log.warn("Could not find checksum algorithm: " + e.getLocalizedMessage());
        }

        return false;
    }

    public void removeUnusedArtifactsFromLocal(Set<String> allResolvesFiles, Set<String> forDeletionFiles)
            throws IOException {
        for (String resolvedFile : forDeletionFiles) {
            File resolvedFileParent = new File(resolvedFile).getParentFile();
            if (!resolvedFileParent.exists()) {
                continue;
            }

            File[] fileSiblings = resolvedFileParent.listFiles();
            if (fileSiblings == null) {
                continue;
            }

            for (File sibling : fileSiblings) {
                String siblingPath = sibling.getAbsolutePath();
                if (!isResolvedOrParentOfResolvedFile(allResolvesFiles, siblingPath)) {
                    FileUtils.forceDelete(sibling);
                    log.info("Deleted unresolved file '" + siblingPath + "'");
                }
            }
        }
    }

    public void setFlatDownload(boolean flat) {
        this.flatDownload = flat;
    }

    private boolean isResolvedOrParentOfResolvedFile(Set<String> resolvedFiles, final String path) {
        return Iterables.any(resolvedFiles, new Predicate<String>() {
            public boolean apply(String filePath) {
                return StringUtils.equals(filePath, path) || StringUtils.startsWith(filePath, path);
            }
        });
    }

    /**
     * Retrieves Artifactory HTTP client.
     *
     * @return Artifactory HTTP client.
     */
    private ArtifactoryDependenciesClient newClient() {
        ArtifactoryDependenciesClient cl =
                new ArtifactoryDependenciesClient(serverUrl,
                        runnerParams.get(RunnerParameterKeys.RESOLVER_USERNAME),
                        runnerParams.get(RunnerParameterKeys.RESOLVER_PASSWORD),
                        log);

        cl.setConnectionTimeout(Integer.parseInt(runnerParams.get(RunnerParameterKeys.TIMEOUT)));

        if (runnerParams.containsKey(PROXY_HOST)) {
            if (StringUtils.isNotBlank(runnerParams.get(PROXY_USERNAME))) {
                cl.setProxyConfiguration(runnerParams.get(PROXY_HOST),
                        Integer.parseInt(runnerParams.get(PROXY_PORT)), runnerParams.get(PROXY_USERNAME),
                        runnerParams.get(PROXY_PASSWORD));
            } else {
                cl.setProxyConfiguration(runnerParams.get(PROXY_HOST),
                        Integer.parseInt(runnerParams.get(PROXY_PORT)));
            }
        }

        return cl;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        client.close();
    }
}
