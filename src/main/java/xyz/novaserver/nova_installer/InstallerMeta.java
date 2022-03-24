package xyz.novaserver.nova_installer;

import org.json.JSONException;
import org.json.JSONObject;
import xyz.novaserver.nova_installer.updater.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InstallerMeta {
    private final String metaUrl;
    private final List<String> gameVersions = new ArrayList<>();
    private final List<InstallerMeta.Edition> editions = new ArrayList<>();

    public InstallerMeta(String url) {
        this.metaUrl = url;
    }

    public void load() throws IOException, JSONException {
        JSONObject json = JsonReader.readJsonFromUrl(this.metaUrl);
        json.getJSONArray("editions").forEach(object -> editions.add(new Edition((JSONObject) object)));
        editions.forEach(edition -> {
            if (!gameVersions.contains(edition.compatibleVersion))
                gameVersions.add(edition.compatibleVersion);
        });
    }

    public List<String> getGameVersions() {
        return this.gameVersions;
    }

    public List<InstallerMeta.Edition> getEditions() {
        return this.editions;
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
