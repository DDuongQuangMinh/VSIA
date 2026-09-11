package com.k1ngtle.vsia.client.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class W130CommandRegistry {
    private static final List<Command> COMMANDS = List.of(
            new Command("file.save", "File: Save", "Save the active file", "save"),
            new Command("file.close", "File: Close Editor", "Close the active editor tab", "close editor"),
            new Command("editor.format", "Editor: Format Document", "Format the active document", "format"),

            new Command("view.explorer", "View: Explorer", "Open Explorer", "explorer"),
            new Command("view.search", "View: Search", "Open workspace search", "search"),
            new Command("view.symbols", "View: Symbols", "Open W1.31 outline and symbol search", "symbols"),
            new Command("view.run", "View: Run and Debug", "Open Run and Debug", "run and debug"),
            new Command("view.scm", "View: Source Control", "Open Source Control", "source control"),
            new Command("view.extensions", "View: Extensions", "Open language capabilities", "extensions"),
            new Command("view.settings", "Preferences: Settings", "Open workspace settings", "settings"),
            new Command("view.terminal", "Terminal: Focus Terminal", "Open and focus the terminal", "terminal"),
            new Command("view.problems", "View: Problems", "Open diagnostics", "problems"),
            new Command("view.output", "View: Output", "Open build/task output", "output"),

            new Command("run.file", "Run: Run Current File", "Run current file without debugging", "run"),
            new Command("debug.start", "Debug: Start Debugging", "Start W1.30 debugger", "w130 debug start"),
            new Command("debug.continue", "Debug: Continue", "Continue to next breakpoint", "w130 debug continue"),
            new Command("debug.pause", "Debug: Pause", "Pause the debug session", "w130 debug pause"),
            new Command("debug.over", "Debug: Step Over", "Execute current statement", "w130 debug next"),
            new Command("debug.into", "Debug: Step Into", "Step into the next statement", "w130 debug step"),
            new Command("debug.out", "Debug: Step Out", "Step out of the current frame", "w130 debug out"),
            new Command("debug.restart", "Debug: Restart", "Restart the current debug session", "w130 debug restart"),
            new Command("debug.stop", "Debug: Stop", "Stop the current debug session", "w130 debug stop"),

            new Command("tasks.build", "Tasks: Run Build Task", "Run configured build task", "w130 task run build"),
            new Command("tasks.check", "Tasks: Run Check Task", "Run configured check task", "w130 task run check"),
            new Command("tasks.list", "Tasks: Show Tasks", "List configured workspace tasks", "w130 task list"),

            new Command("scm.status", "Source Control: Refresh", "Refresh working-tree state", "w130 scm status"),
            new Command("scm.stage", "Source Control: Stage All", "Stage all changes", "w130 scm stage *"),
            new Command("scm.unstage", "Source Control: Unstage All", "Unstage all changes", "w130 scm unstage *"),
            new Command("scm.log", "Source Control: Show History", "Show internal commit history", "w130 scm log"),
            new Command("scm.revert", "Source Control: Revert All", "Restore all files to HEAD", "w130 scm revert *"),

            new Command("workspace.status", "Workspace: Status", "Show W1.30 workspace configuration", "w130 workspace status"),
            new Command("workspace.index", "Workspace: Rebuild Symbol Index", "Index project definitions", "w131 index"),
            new Command("workspace.outline", "Workspace: Outline Current File", "List symbols in the active file", "w131 outline"),
            new Command("workspace.symbols", "Workspace: Search Symbols", "Search workspace definitions", "w131 symbols"),
            new Command("workspace.diagnostics", "Workspace: Diagnostics", "Validate the complete workspace", "w131 diagnostics"),
            new Command("workspace.recent", "Workspace: Recent Files", "Show persistent recent files", "w131 recent"),

            new Command("navigation.definition", "Go to Definition", "Navigate to symbol definition", "definition "),
            new Command("navigation.references", "Find References", "Find symbol references", "references "),
            new Command("navigation.complete", "Trigger Symbol Completion", "Show symbol completion candidates", "complete "),

            new Command("shell.pwd", "Terminal: Print Working Directory", "Show W1.31 shell cwd", "pwd"),
            new Command("shell.ls", "Terminal: List Directory", "List the current directory", "ls"),
            new Command("shell.tree", "Terminal: Workspace Tree", "Print project directory tree", "tree"),

            new Command("selftest.w130", "Developer: W1.30 Self Test", "Run W1.30 regression tests", "w130 selftest"),
            new Command("selftest.w131", "Developer: W1.31 Self Test", "Run project-intelligence self-test", "w131 selftest"),

            new Command("project.build", "Project: Build", "Build the web project", "build"),
            new Command("project.publish", "Project: Publish", "Publish project to Server Rack", "publish")
    );

    private W130CommandRegistry() {
    }

    public static List<Command> search(
            String query,
            int limit
    ) {
        String normalized =
                query == null
                        ? ""
                        : query.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        int cap =
                Math.max(
                        1,
                        limit
                );

        if (normalized.startsWith(">")) {
            normalized =
                    normalized.substring(1)
                            .trim();
        }

        if (normalized.isEmpty()) {
            return COMMANDS.stream()
                    .limit(cap)
                    .toList();
        }

        List<Scored> scored =
                new ArrayList<>();

        for (Command command :
                COMMANDS) {
            String haystack =
                    (
                            command.label()
                                    + " "
                                    + command.description()
                                    + " "
                                    + command.command()
                    )
                            .toLowerCase(
                                    Locale.ROOT
                            );

            int score =
                    score(
                            haystack,
                            normalized
                    );

            if (score >= 0) {
                scored.add(
                        new Scored(
                                command,
                                score
                        )
                );
            }
        }

        return scored.stream()
                .sorted(
                        Comparator.comparingInt(
                                        Scored::score
                                )
                                .thenComparing(
                                        value ->
                                                value.command()
                                                        .label()
                                )
                )
                .limit(cap)
                .map(
                        Scored::command
                )
                .toList();
    }

    private static int score(
            String haystack,
            String needle
    ) {
        if (haystack.startsWith(
                needle
        )) {
            return 0;
        }

        int direct =
                haystack.indexOf(
                        needle
                );

        if (direct >= 0) {
            return 10 + direct;
        }

        int position = 0;
        int gaps = 0;

        for (int i = 0;
             i < needle.length();
             i++) {
            int found =
                    haystack.indexOf(
                            needle.charAt(i),
                            position
                    );

            if (found < 0) {
                return -1;
            }

            gaps +=
                    found - position;

            position =
                    found + 1;
        }

        return 100 + gaps;
    }

    public record Command(
            String id,
            String label,
            String description,
            String command
    ) {
    }

    private record Scored(
            Command command,
            int score
    ) {
    }
}
