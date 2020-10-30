package org.jfrog.teamcity.agent;

/**
 * Created by Bar Belity on 28/10/2020.
 */
public class ServerConfig {
    private String url;
    private String username;
    private String password;
    private int timeout;

    public ServerConfig(String url, String username, String password, int timeout) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
