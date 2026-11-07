import java.security.MessageDigest
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class ExtractDownloaderRuntime : DefaultTask() {
    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE)
    abstract val archives: ConfigurableFileCollection
    @get:Input abstract val expectedSha256: Property<String>
    @get:OutputDirectory abstract val destination: DirectoryProperty

    @TaskAction fun extract() {
        val archive = archives.singleFile
        val digest = MessageDigest.getInstance("SHA-256").digest(archive.readBytes()).joinToString("") { "%02x".format(it) }
        check(digest == expectedSha256.get()) { "Upstream runtime AAR checksum mismatch" }
        val root = destination.get().asFile
        root.deleteRecursively()
        root.mkdirs()
        ZipFile(archive).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory && (it.name.startsWith("jni/") || it.name.startsWith("res/")) }.forEach { entry ->
                val target = java.io.File(root, entry.name)
                check(target.canonicalPath.startsWith(root.canonicalPath + java.io.File.separator)) { "Unsafe runtime archive entry" }
                target.parentFile.mkdirs()
                zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
            }
        }
    }
}
