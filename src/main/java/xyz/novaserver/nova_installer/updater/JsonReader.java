package xyz.novaserver.nova_installer.updater;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JsonReader {

    public static String readAll(Reader reader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int codePoint;
        while ((codePoint = reader.read()) != -1) {
            stringBuilder.append((char) codePoint);
        }
        return stringBuilder.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException {
        return new JSONObject(readAll(getReader(url)));
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException {
        return new JSONArray(readAll(getReader(url)));
    }

    private static BufferedReader getReader(String url) throws IOException {
        return new BufferedReader(new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8));
    }
}
