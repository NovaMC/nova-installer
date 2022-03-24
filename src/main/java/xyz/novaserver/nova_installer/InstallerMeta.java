package xyz.novaserver.nova_installer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.novaserver.nova_installer.updater.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InstallerMeta {
    private final String metaUrl;
    private final String downloadApiUrl;
    private final List<InstallerMeta.Edition> editions = new ArrayList<>();

    public InstallerMeta(String metaUrl, String downloadApiUrl) {
        this.metaUrl = metaUrl;
        this.downloadApiUrl = downloadApiUrl;
    }

    public void load() throws IOException, JSONException {
        JSONObject metaJson = JsonReader.readJsonFromUrl(this.metaUrl);
        metaJson.getJSONArray("editions").forEach(object -> editions.add(new Edition((JSONObject) object)));
    }

    public List<InstallerMeta.Edition> getEditions() {
        return this.editions;
    }

    public String getDownloadUrl(String editionName) throws IOException {
        JSONArray downloadJson = JsonReader.readJsonArrayFromUrl(this.downloadApiUrl);

        for (Object obj1 : downloadJson) {
            JSONObject release = (JSONObject) obj1;
            if (release.getString("tag_name").endsWith(editionName)) {
                JSONArray assets = release.getJSONArray("assets");
                for (Object obj2 : assets) {
                    JSONObject asset = (JSONObject) obj2;
                    if (asset.getString("name").equals("pack.zip")) {
                        return asset.getString("browser_download_url");
                    }
                }
            }
        }
        return null;
    }

    public static class Edition {
        public final String name;
        public final String displayName;
        public final String compatibleVersion;
        public boolean unstable;

        public Edition(JSONObject jsonObject) {
            this.name = jsonObject.getString("name");
            this.displayName = jsonObject.getString("display_name");
            this.compatibleVersion = jsonObject.getString("minecraft_version");

            try {
                this.unstable = jsonObject.getBoolean("unstable");
            } catch (JSONException e) {
                System.out.println("No unstable value found for " + name + "! Using the default value of false.");
                this.unstable = false;
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
