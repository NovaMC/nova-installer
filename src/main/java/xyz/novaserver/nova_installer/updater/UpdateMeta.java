package xyz.novaserver.nova_installer.updater;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Properties;

public class UpdateMeta {
    private final String repoApiUrl;
    private final Properties info = new Properties();
    private JSONObject latestJson;

    public UpdateMeta(String repoApiUrl) {
        this.repoApiUrl = repoApiUrl;
    }

    public void load() throws IOException {
        info.load(getClass().getClassLoader().getResourceAsStream("info.properties"));
        latestJson = JsonReader.readJsonFromUrl(repoApiUrl);
    }

    public String getCurrentVersion() {
        return info.getProperty("version");
    }

    public String getLatestVersion() {
        return latestJson.getString("tag_name");
    }

    public boolean hasLatestVersion() {
        Version currentVersion = new Version(getCurrentVersion());
        Version latestVersion = new Version(getLatestVersion().replaceAll("[^0-9.]",""));
        return currentVersion.compareTo(latestVersion) >= 0;
    }

    public String getUpdateUrl() {
        return latestJson.getString("html_url");
    }
}
