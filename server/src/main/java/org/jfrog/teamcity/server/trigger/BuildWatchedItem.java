package org.jfrog.teamcity.server.trigger;

/**
 * @author Shay Yaakov
 */
public class BuildWatchedItem {

    private String itemPath;
    private String artifactoryUrlId;
    private String triggerId;
    private long itemLastModified;

    public BuildWatchedItem(String itemPath, String artifactoryUrlId, String triggerId, long itemLastModified) {
        this.itemPath = itemPath;
        this.artifactoryUrlId = artifactoryUrlId;
        this.triggerId = triggerId;
        this.itemLastModified = itemLastModified;
    }

    public String getItemPath() {
        return itemPath;
    }

    public long getItemLastModified() {
        return itemLastModified;
    }

    public void setItemLastModified(long itemLastModified) {
        this.itemLastModified = itemLastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildWatchedItem that = (BuildWatchedItem) o;

        if (itemPath != null ? !itemPath.equals(that.itemPath) : that.itemPath != null) return false;
        if (artifactoryUrlId != null ? !artifactoryUrlId.equals(that.artifactoryUrlId) : that.artifactoryUrlId != null)
            return false;
        return !(triggerId != null ? !triggerId.equals(that.triggerId) : that.triggerId != null);

    }

    @Override
    public int hashCode() {
        int result = itemPath != null ? itemPath.hashCode() : 0;
        result = 31 * result + (artifactoryUrlId != null ? artifactoryUrlId.hashCode() : 0);
        result = 31 * result + (triggerId != null ? triggerId.hashCode() : 0);
        return result;
    }
}
