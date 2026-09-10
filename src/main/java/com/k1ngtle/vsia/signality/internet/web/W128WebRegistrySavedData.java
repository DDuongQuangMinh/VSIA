package com.k1ngtle.vsia.signality.internet.web;

import com.k1ngtle.vsia.signality.internet.provider.InternetDnsAnswer;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryValidators;
import com.k1ngtle.vsia.signality.internet.provider.RegisteredDomain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class W128WebRegistrySavedData extends SavedData {
    public static final String DATA_NAME = "vsia_w128_web_registry";
    public static final int MAX_PROJECTS_PER_PLAYER = 16;
    public static final int MAX_FILES_PER_PROJECT = 128;
    public static final int MAX_FILE_CHARACTERS = 262_144;
    public static final int MAX_PROJECT_CHARACTERS = 1_048_576;

    private final Map<String, W128WebProject> projects = new LinkedHashMap<>();

    public W128WebRegistrySavedData() {
    }

    public W128WebRegistrySavedData(CompoundTag tag) {
        loadFromTag(tag);
    }

    public static W128WebRegistrySavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        W128WebRegistrySavedData::new,
                        W128WebRegistrySavedData::new,
                        DATA_NAME
                );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        projects.values().forEach(project -> list.add(project.save()));
        tag.put("Projects", list);
        return tag;
    }

    public synchronized Collection<W128WebProject> projects() {
        return List.copyOf(projects.values());
    }

    public synchronized Optional<W128WebProject> project(String host) {
        return Optional.ofNullable(projects.get(normalizeHost(host)));
    }

    public synchronized List<W128WebProject> projectsOwnedBy(UUID ownerUuid) {
        return projects.values()
                .stream()
                .filter(project -> project.ownerUuid().equals(ownerUuid))
                .sorted(Comparator.comparing(W128WebProject::host))
                .toList();
    }

    public synchronized W128WebBuildResult create(
            ServerLevel level,
            UUID ownerUuid,
            String ownerName,
            String requestedHost,
            W128WebMode mode,
            long nowMillis
    ) {
        String host = normalizeHost(requestedHost);
        if (!InternetRegistryValidators.validHostname(host)) {
            return W128WebBuildResult.fail("Invalid website hostname.");
        }

        if (projects.containsKey(host)) {
            return W128WebBuildResult.fail("A web project already exists for " + host + ".");
        }

        if (projectsOwnedBy(ownerUuid).size() >= MAX_PROJECTS_PER_PLAYER) {
            return W128WebBuildResult.fail("Web project ownership limit reached.");
        }

        String root = InternetRegistryValidators.registrableRoot(host);
        if (root.isBlank()) {
            return W128WebBuildResult.fail("The hostname is not inside a supported registrable domain.");
        }

        RegisteredDomain registration = InternetRegistrySavedData.get(level)
                .domain(root)
                .orElse(null);

        if (registration == null || registration.expired(nowMillis) || !registration.active()) {
            return W128WebBuildResult.fail("The ISP1 domain " + root + " is not active.");
        }

        if (!registration.ownedBy(ownerUuid)) {
            return W128WebBuildResult.fail("You must own " + root + " through ISP1 before creating this website.");
        }

        W128WebProject project = new W128WebProject(
                host,
                root,
                ownerUuid,
                ownerName,
                mode,
                nowMillis
        );

        installDefaults(project, nowMillis);
        projects.put(host, project);
        setDirty();

        return W128WebBuildResult.ok(
                "Web project created: " + host + " | mode=" + mode.name().toLowerCase()
        );
    }

    public synchronized W128WebBuildResult delete(UUID actor, String requestedHost) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        projects.remove(project.host());
        setDirty();
        return W128WebBuildResult.ok("Web project deleted: " + project.host());
    }

    public synchronized W128WebBuildResult putFile(
            UUID actor,
            String requestedHost,
            String requestedPath,
            String content,
            long nowMillis
    ) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        String path;
        try {
            path = normalizePath(requestedPath);
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail(exception.getMessage());
        }

        String value = content == null ? "" : content;
        if (value.length() > MAX_FILE_CHARACTERS) {
            return W128WebBuildResult.fail("File exceeds the 262144-character W1.28 limit.");
        }

        W128WebFile old = project.file(path);
        if (old == null && project.files().size() >= MAX_FILES_PER_PROJECT) {
            return W128WebBuildResult.fail("Project file limit reached.");
        }

        int projected = project.totalCharacters()
                - (old == null ? 0 : old.content().length())
                + value.length();

        if (projected > MAX_PROJECT_CHARACTERS) {
            return W128WebBuildResult.fail("Project exceeds the 1048576-character W1.28 limit.");
        }

        project.putFile(
                new W128WebFile(
                        path,
                        value,
                        W128MimeTypes.forPath(path),
                        nowMillis,
                        false
                )
        );
        project.setPublished(false, nowMillis);
        setDirty();

        return W128WebBuildResult.ok(
                "Saved " + path + " | " + value.length() + " characters | publish required"
        );
    }

    public synchronized W128WebBuildResult removeFile(
            UUID actor,
            String requestedHost,
            String requestedPath,
            long nowMillis
    ) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        String path;
        try {
            path = normalizePath(requestedPath);
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail(exception.getMessage());
        }

        if (!project.removeFile(path, nowMillis)) {
            return W128WebBuildResult.fail("File not found: " + path);
        }

        project.setPublished(false, nowMillis);
        setDirty();
        return W128WebBuildResult.ok("Removed " + path + " | publish required");
    }

    public synchronized W128WebBuildResult bind(
            UUID actor,
            String requestedHost,
            String serverIp,
            long nowMillis
    ) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }
        if (!InternetRegistryValidators.validIpv4(serverIp)) {
            return W128WebBuildResult.fail("Server Rack IPv4 address is invalid.");
        }

        project.setBoundServerIp(serverIp.trim(), nowMillis);
        project.setPublished(false, nowMillis);
        setDirty();
        return W128WebBuildResult.ok(
                "Web project bound to Server Rack IPv4 " + serverIp.trim() + " | publish required"
        );
    }

    public synchronized W128WebBuildResult build(
            UUID actor,
            String requestedHost,
            long nowMillis
    ) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        if (project.mode() == W128WebMode.STATIC) {
            W128WebFile index = project.file("/index.html");
            if (index == null || index.content().isBlank()) {
                return W128WebBuildResult.fail("Static project requires /index.html.");
            }
            project.markBuilt(nowMillis);
            project.setPublished(false, nowMillis);
            setDirty();
            return W128WebBuildResult.ok(
                    "Static build passed | revision=" + project.buildRevision()
            );
        }

        W128WebFile source = project.file("/src/App.jsx");
        if (source == null) {
            return W128WebBuildResult.fail("React project requires /src/App.jsx.");
        }

        String compiled;
        try {
            compiled = W128JsxCompiler.compile(source.content());
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail("JSX build failed: " + exception.getMessage());
        }

        String appJs = "\"use strict\";\n"
                + compiled
                + "\nconst __vsiaRoot = ReactDOM.createRoot(document.getElementById(\"root\"));\n"
                + "__vsiaRoot.render(React.createElement(App, null));\n";

        String indexHtml = "<!doctype html>\n"
                + "<html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escapeHtml(project.host()) + "</title>"
                + "<link rel=\"stylesheet\" href=\"/styles.css\"></head>"
                + "<body><div id=\"root\"></div>"
                + "<script src=\"/react-runtime.js\"></script>"
                + "<script src=\"/app.js\"></script></body></html>";

        putGeneratedUnlessManual(project, "/react-runtime.js", W128ReactRuntime.source(), nowMillis);
        putGeneratedUnlessManual(project, "/app.js", appJs, nowMillis);
        putGeneratedUnlessManual(project, "/index.html", indexHtml, nowMillis);
        project.markBuilt(nowMillis);
        project.setPublished(false, nowMillis);
        setDirty();

        return W128WebBuildResult.ok(
                "React JSX build passed | revision=" + project.buildRevision()
                        + " | app.js=" + appJs.length() + " chars"
        );
    }

    public synchronized W128WebBuildResult publish(
            ServerLevel level,
            UUID actor,
            String requestedHost,
            long nowMillis
    ) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        W128WebBuildResult authority = validateAuthority(level, project, nowMillis);
        if (!authority.success()) {
            return authority;
        }

        if (project.boundServerIp().isBlank()) {
            return W128WebBuildResult.fail("Bind the project to a Server Rack IPv4 address first.");
        }

        W128WebFile index = project.file("/index.html");
        if (index == null || index.content().isBlank()) {
            return W128WebBuildResult.fail("Build is missing /index.html.");
        }

        Optional<InternetDnsAnswer> answer = InternetRegistrySavedData.get(level)
                .resolveFirst(project.host(), "A", nowMillis);

        if (answer.isEmpty()) {
            return W128WebBuildResult.fail("ISP1 DNS has no A resolution for " + project.host() + ".");
        }

        if (!project.boundServerIp().equals(answer.get().value())) {
            return W128WebBuildResult.fail(
                    "DNS A mismatch: " + project.host() + " resolves to " + answer.get().value()
                            + " but the web project is bound to " + project.boundServerIp() + "."
            );
        }

        project.setPublished(true, nowMillis);
        setDirty();
        return W128WebBuildResult.ok(
                "Published http://" + project.host()
                        + " to " + project.boundServerIp()
                        + " | revision=" + project.buildRevision()
        );
    }

    public synchronized W128WebBuildResult unpublish(
            UUID actor,
            String requestedHost,
            long nowMillis
    ) {
        W128WebProject project = projects.get(normalizeHost(requestedHost));
        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(actor)) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        project.setPublished(false, nowMillis);
        setDirty();
        return W128WebBuildResult.ok("Website unpublished: " + project.host());
    }

    public synchronized W128WebBuildResult validateAuthority(
            ServerLevel level,
            W128WebProject project,
            long nowMillis
    ) {
        RegisteredDomain registration = InternetRegistrySavedData.get(level)
                .domain(project.rootDomain())
                .orElse(null);

        if (registration == null) {
            return W128WebBuildResult.fail("Registered domain no longer exists.");
        }
        if (registration.expired(nowMillis) || !registration.active()) {
            return W128WebBuildResult.fail("Registered domain is expired or inactive.");
        }
        if (!registration.ownerUuid().equals(project.ownerUuid())) {
            return W128WebBuildResult.fail("Registered domain ownership changed; this web project is no longer authorized.");
        }

        return W128WebBuildResult.ok("Domain authority valid.");
    }

    public static String normalizeHost(String value) {
        String host = value == null ? "" : value.trim().toLowerCase();
        int scheme = host.indexOf("://");
        if (scheme >= 0) {
            host = host.substring(scheme + 3);
        }
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        return InternetRegistryValidators.normalizeDomain(host);
    }

    public static String normalizePath(String value) {
        String path = value == null ? "/" : value.trim();
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int fragment = path.indexOf('#');
        if (fragment >= 0) {
            path = path.substring(0, fragment);
        }
        path = path.replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }

        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("Path traversal is not allowed.");
            }
            if (segment.length() > 128) {
                throw new IllegalArgumentException("A path segment exceeds 128 characters.");
            }
            segments.add(segment);
        }

        String normalized = "/" + String.join("/", segments);
        if (normalized.length() > 512) {
            throw new IllegalArgumentException("Path exceeds 512 characters.");
        }
        return "/".equals(normalized) ? "/index.html" : normalized;
    }

    private void installDefaults(W128WebProject project, long nowMillis) {
        if (project.mode() == W128WebMode.REACT) {
            project.putFile(new W128WebFile(
                    "/src/App.jsx",
                    "function App() {\n"
                            + "  const [count, setCount] = React.useState(0);\n"
                            + "  return <main className=\"app\"><h1>" + project.host() + "</h1>"
                            + "<p>VS:IA W1.28 React-compatible site</p>"
                            + "<button onClick={() => setCount(count + 1)}>Clicks: {count}</button></main>;\n"
                            + "}\n",
                    "text/javascript; charset=utf-8",
                    nowMillis,
                    false
            ));
            project.putFile(new W128WebFile(
                    "/styles.css",
                    "body{font-family:sans-serif;background:#101419;color:#eef2f6;margin:0;padding:32px}"
                            + ".app{max-width:720px;margin:auto}button{padding:8px 14px}",
                    "text/css; charset=utf-8",
                    nowMillis,
                    false
            ));
        } else {
            project.putFile(new W128WebFile(
                    "/index.html",
                    "<!doctype html><html><head><meta charset=\"utf-8\">"
                            + "<link rel=\"stylesheet\" href=\"/styles.css\"></head>"
                            + "<body><h1>" + escapeHtml(project.host()) + "</h1>"
                            + "<p>Hosted by VS:IA W1.28.</p>"
                            + "<script src=\"/app.js\"></script></body></html>",
                    "text/html; charset=utf-8",
                    nowMillis,
                    false
            ));
            project.putFile(new W128WebFile(
                    "/styles.css",
                    "body{font-family:sans-serif;background:#101419;color:#eef2f6;padding:32px}",
                    "text/css; charset=utf-8",
                    nowMillis,
                    false
            ));
            project.putFile(new W128WebFile(
                    "/app.js",
                    "console.log('VS:IA W1.28 website loaded');\n",
                    "text/javascript; charset=utf-8",
                    nowMillis,
                    false
            ));
        }
    }

    private void putGeneratedUnlessManual(
            W128WebProject project,
            String path,
            String content,
            long nowMillis
    ) {
        W128WebFile existing = project.file(path);

        if (existing != null && !existing.generated()) {
            return;
        }

        putGenerated(
                project,
                path,
                content,
                nowMillis
        );
    }

    private void putGenerated(
            W128WebProject project,
            String path,
            String content,
            long nowMillis
    ) {
        project.putFile(new W128WebFile(
                path,
                content,
                W128MimeTypes.forPath(path),
                nowMillis,
                true
        ));
    }

    private void loadFromTag(CompoundTag tag) {
        if (!tag.contains("Projects", Tag.TAG_LIST)) {
            return;
        }

        ListTag list = tag.getList("Projects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            W128WebProject project = W128WebProject.load(list.getCompound(i));
            if (!project.host().isBlank()) {
                projects.put(project.host(), project);
            }
        }
    }

    private static String escapeHtml(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
