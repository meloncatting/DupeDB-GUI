package net.chamosmp.dupedbgui.client.api;

import com.google.gson.*;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DupeDBApi {
    private static final String BASE = "https://dupedb.net";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson GSON = new Gson();

    public static CompletableFuture<ExploitPage> fetchExploits(int page, String sort, String order) {
        String url = BASE + "/api/public/exploits?page=" + page + "&sort=" + sort + "&order=" + order;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "DupeDB-GUI-Mod/1.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() != 200) return null;
                    try {
                        JsonObject json = GSON.fromJson(res.body(), JsonObject.class);
                        ExploitPage page2 = new ExploitPage();
                        page2.exploits = new ArrayList<>();

                        JsonArray arr = json.getAsJsonArray("exploits");
                        for (JsonElement el : arr) {
                            page2.exploits.add(GSON.fromJson(el, Exploit.class));
                        }

                        JsonObject pag = json.getAsJsonObject("pagination");
                        page2.page = pag.get("page").getAsInt();
                        page2.totalPages = pag.get("pages").getAsInt();
                        page2.total = pag.get("total").getAsInt();
                        page2.hasMore = pag.get("hasMore").getAsBoolean();
                        return page2;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .exceptionally(e -> null);
    }

    public static CompletableFuture<String> fetchExploitDetail(String exploitId, String token) {
        String url = BASE + "/api/exploits/" + exploitId;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "DupeDB-GUI-Mod/1.0")
                .header("X-App-Token", token)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() != 200) return null;
                    try {
                        JsonObject json = GSON.fromJson(res.body(), JsonObject.class);
                        if (json == null || !json.has("description")) return null;
                        JsonElement desc = json.get("description");
                        if (desc.isJsonNull()) return null;
                        return desc.getAsString();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .exceptionally(e -> null);
    }

    public static CompletableFuture<String> fetchPageDescription(String exploitId) {
        String url = BASE + "/exploit/" + exploitId;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; DupeDB-GUI-Mod/1.0)")
                .header("Accept", "text/html")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() != 200) return null;
                    String html = res.body();
                    int idx = html.indexOf("<meta name=\"description\" content=\"");
                    if (idx < 0) return null;
                    int start = idx + "<meta name=\"description\" content=\"".length();
                    int end = html.indexOf("\"", start);
                    if (end < 0) return null;
                    String desc = html.substring(start, end);
                    if (desc.startsWith("## Steps:") || desc.startsWith("## Steps\n")) {
                        desc = desc.replaceFirst("##\\s*Steps:?\\s*", "").trim();
                    }
                    return desc.isEmpty() ? null : desc;
                })
                .exceptionally(e -> null);
    }

    public static CompletableFuture<byte[]> fetchBytes(String url) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "DupeDB-GUI-Mod/1.0")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(res -> res.statusCode() == 200 ? res.body() : null)
                .exceptionally(e -> null);
    }

    public static class ExploitPage {
        public List<Exploit> exploits;
        public boolean hasMore;
        public int page;
        public int totalPages;
        public int total;
    }
}
