package xyz.novaserver.nova_installer.updater;

import org.json.JSONObject;

import java.io.IOException;

public class UpdateMeta {
    private JSONObject latestJson;
    private final String repoApiUrl;
    private final Version currentVersion;
    private Version latestVersion;

    public UpdateMeta(String repoApiUrl, Version currentVersion) {
        this.repoApiUrl = repoApiUrl;
        this.currentVersion = currentVersion;
    }

    public void load() throws IOException {
        latestJson = JsonReader.readJsonFromUrl(repoApiUrl);
        latestVersion = new Version(latestJson.getString("tag_name"));
    }

    public Version getCurrentVersion() {
        return currentVersion;
    }

    public Version getLatestVersion() {
        return latestVersion;
    }

    public boolean hasLatestVersion() {
        return latestVersion == null || currentVersion.compareTo(getLatestVersion()) >= 0;
    }

    public String getUpdateUrl() {
        return latestJson.getString("html_url");
    }
}
