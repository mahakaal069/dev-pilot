package com.learn.dev_bot.utils.indexing;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class CodeFileFilter {
    private static final Set<String> SKIP_DIR_PARTS = Set.of(
            "node_modules", ".git", "dist", "build",
            "target", ".next", "vendor", "__pycache__",
            ".idea", ".vscode", "coverage", "out"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "java", "kt", "kts", "scala",
            "ts", "tsx", "js", "jsx", "mjs", "cjs",
            "py", "go", "rs", "rb", "php", "c", "h", "cpp",
            "hpp", "cs", "swift", "m", "mm", "md", "mdx", "txt",
            "yml", "yaml", "json", "toml", "xml", "properties", "gradle", "sql",
            "sh", "bash", "zsh", "dockerfile", "makefile", "html", "css",
            "scss", "sass", "vue", "svelte"
    );

    private static final Set<String> SKIP_FILENAMES = Set.of(
            "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "composer.lock", "cargo.lock", "poetry.lock"
    );

    public boolean isEligible(String path, long sizeBytes, long maxFileBytes) {
        if(path == null || path.isBlank())
            return false;

        String normalized = path.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);

        for (String part : lower.split("/"))
            if (SKIP_DIR_PARTS.contains(part))
                return false;

        String filename = lower.substring(lower.lastIndexOf('/') + 1);

        if (SKIP_FILENAMES.contains(filename))
            return false;

        if (filename.startsWith("."))
            return false;

        if (sizeBytes > maxFileBytes)
            return false;

        if ("dockerfile".equals(filename) || "makefile".equals(filename))
            return true;

        int dot = filename.lastIndexOf('.');

        if (dot < 0)
            return false;

        String ext = filename.substring(dot + 1);

        return ALLOWED_EXTENSIONS.contains(ext);
    }

    public String detectLanguage(@NonNull String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String filename = lower.substring(lower.indexOf('/') + 1);

        if ("dockerfile".equals(filename))
            return "dockerfile";

        if ("makefile".equals(filename))
            return "makefile";

        int dot = filename.lastIndexOf('.');

        if (dot < 0)
            return "text";

        return filename.substring(dot + 1);
    }
}
