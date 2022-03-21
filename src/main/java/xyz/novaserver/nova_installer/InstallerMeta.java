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
        editions.forEach(edition -> edition.compatibleVersions.forEach(version -> {
            if (!gameVersions.contains(version))
                gameVersions.add(version);
        }));
    }

    public List<String> getGameVersions() {
        return this.gameVersions;
    }

    public List<InstallerMeta.Edition> getEditions() {
        return this.editions;
    }

    public static class Edition {
        String name;
        String displayName;
        boolean unstable;
        List<String> compatibleVersions = new ArrayList<>();

        public Edition(JSONObject jsonObject) {
            this.name = jsonObject.getString("name");
            this.displayName = jsonObject.getString("display_name");

            try {
                this.unstable = jsonObject.getBoolean("unstable");
            } catch (JSONException e) {
                System.out.println("No key/value found for unstable! Using the default value of false.");
                this.unstable = false;
            }

            for (int i = 0; i < jsonObject.getJSONArray("compatible_versions").toList().size(); i++){
                compatibleVersions.add(jsonObject.getJSONArray("compatible_versions").toList().get(i).toString());
            }
        }
    }
}
