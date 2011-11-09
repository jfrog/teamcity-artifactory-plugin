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

package org.jfrog.teamcity.agent;

import jetbrains.buildServer.agent.BuildProgressLogger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonParser;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.teamcity.agent.api.PatternResultFileSet;
import org.jfrog.teamcity.agent.util.TeamcityAgenBuildInfoLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Noam Y. Tenne
 */
public class PublishedDependencyClient {

    private String artifactoryUrl;

    private ArtifactoryHttpClient httpClient;

    public PublishedDependencyClient(String artifactoryUrl, String username, String password,
            BuildProgressLogger logger) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, username, password,
                new TeamcityAgenBuildInfoLog(logger));
    }

    public void setConnectionTimeout(int connectionTimeout) {
        httpClient.setConnectionTimeout(connectionTimeout);
    }

    public void setProxyConfiguration(String host, int port) {
        httpClient.setProxyConfiguration(host, port, null, null);
    }

    public void setProxyConfiguration(String host, int port, String username, String password) {
        httpClient.setProxyConfiguration(host, port, username, password);
    }

    public void shutdown() {
        if (httpClient != null) {
            httpClient.shutdown();
        }
    }

    public PatternResultFileSet searchArtifactsByPattern(String pattern) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String patternSearchUrl = artifactoryUrl + "/api/search/pattern?pattern=" + pattern;
        HttpGet httpget = new HttpGet(patternSearchUrl);
        HttpResponse response = client.execute(httpget);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                httpEntity.consumeContent();
            }
            throw new IOException("Failed to search artifact by the pattern '" + pattern + "': " +
                    response.getStatusLine());
        } else {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream content = entity.getContent();
                try {
                    JsonParser parser = httpClient.createJsonParser(content);
                    return parser.readValueAs(PatternResultFileSet.class);
                } finally {
                    IOUtils.closeQuietly(content);
                    entity.consumeContent();
                }
            }
        }

        return null;
    }

    public void downloadArtifact(String downloadUrl, File dest) throws IOException {
        HttpResponse response = executeGet(downloadUrl);

        if (dest.exists()) {
            dest.delete();
            dest.createNewFile();
        } else {
            dest.getParentFile().mkdirs();
            dest.createNewFile();
        }
        InputStream inputStream = response.getEntity().getContent();
        FileOutputStream fileOutputStream = new FileOutputStream(dest);
        try {
            IOUtils.copyLarge(inputStream, fileOutputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);
            response.getEntity().consumeContent();
        }
    }

    public String downloadChecksum(String downloadUrl, String checksumAlgorithm) throws IOException {
        HttpResponse response = executeGet(downloadUrl + "." + checksumAlgorithm);

        InputStream inputStream = response.getEntity().getContent();
        try {
            return IOUtils.toString(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            response.getEntity().consumeContent();
        }
    }

    private HttpResponse executeGet(String downloadUrl) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        HttpGet get = new HttpGet(downloadUrl);
        //Explicitly force keep alive
        get.setHeader("Connection", "Keep-Alive");

        HttpResponse response = client.execute(get);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                httpEntity.consumeContent();
            }
            throw new FileNotFoundException("Unable to find " + downloadUrl);
        }

        if (statusCode != HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                httpEntity.consumeContent();
            }
            throw new IOException("Error downloading " + downloadUrl + ". Code: " + statusCode + " Message: " +
                    statusLine.getReasonPhrase());
        }
        return response;
    }
}