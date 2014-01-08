package org.jfrog.teamcity.server.trigger;

/**
 * @author Shay Yaakov
 */
public class BuildWatchedItem {

    private String itemPath;
    private long itemLastModified;

    public BuildWatchedItem(String itemPath, long itemLastModified) {
        this.itemPath = itemPath;
        this.itemLastModified = itemLastModified;
    }

    public String getItemPath() {
        return itemPath;
    }

    public void setItemPath(String itemPath) {
        this.itemPath = itemPath;
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

        return true;
    }

    @Override
    public int hashCode() {
        return itemPath != null ? itemPath.hashCode() : 0;
    }
}
