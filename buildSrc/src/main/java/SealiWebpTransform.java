import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.inject.Inject;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.ExecOperations;

/** Preserve original Maven component/metadata while replacing its AAR bytes. */
public abstract class SealiWebpTransform implements TransformAction<SealiWebpTransform.Parameters> {
    public interface Parameters extends TransformParameters {
        @InputDirectory
        @PathSensitive(PathSensitivity.RELATIVE)
        DirectoryProperty getRecipeDirectory();
    }
    @InputArtifact
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract Provider<FileSystemLocation> getInputArtifact();
    @Inject protected abstract ExecOperations getExecOperations();
    private static final String ORIGINAL = "0a87ffa6cf912b0fe76c1a99b9107f543ee2f247935fae2c71f0822eb7bc5f49";
    private static final String CANDIDATE = "6755c855f9e86a942b6819374093d9cee6265fed1723e7c326f3969f68b36af3";
    private static String hash(File file) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[1024 * 1024];
                for (int read; (read = input.read(buffer)) >= 0;) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) { throw new GradleException("Cannot hash native input", exception); }
    }
    @Override public void transform(TransformOutputs outputs) {
        File input = getInputArtifact().get().getAsFile();
        if (!input.getName().startsWith("ffmpeg-")) {
            outputs.file(input);
            return;
        }
        if (!input.getName().equals("ffmpeg-0.18.1.aar") || !hash(input).equals(ORIGINAL))
            throw new GradleException("Unexpected FFmpeg input. Refusing native substitution.");
        File target = outputs.file("ffmpeg-0.18.1-seali-webp16k.aar");
        File output = target.getParentFile();
        File work = new File(output, "fresh-work");
        File recipe = new File(getParameters().getRecipeDirectory().get().getAsFile(), "rebuild-webp.sh");
        // timeout uses a process group. TERM then KILL protects the standard host
        // from an unbounded download/compiler; recipe performs no publication.
        getExecOperations().exec(exec -> {
            exec.setEnvironment(java.util.Map.of("PATH", "/usr/bin:/bin", "LC_ALL", "C", "TZ", "UTC"));
            exec.commandLine("/usr/bin/timeout", "--signal=TERM", "--kill-after=30s", "900s",
                    "/bin/bash", recipe.getAbsolutePath(), work.getAbsolutePath());
        }).assertNormalExitValue();
        File candidate = new File(work, "ffmpeg-0.18.1-seali-webp16k-candidate.aar");
        if (!candidate.isFile() || !hash(candidate).equals(CANDIDATE))
            throw new GradleException("Native candidate differs from reviewed reproducible result.");
        // Only register the AAR, not the expanded work/toolchain, as artifact.
                try { Files.copy(candidate.toPath(), target.toPath()); }
        catch (IOException exception) { throw new GradleException("Cannot stage native candidate", exception); }
    }
}
