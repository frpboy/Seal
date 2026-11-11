import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/** Verify actual resolved AAR input files, not only the local repository copies. */
abstract class VerifyAlpha7MavenTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles abstract val selectedAars: ConfigurableFileCollection
    @get:InputFile abstract val artifactLock: RegularFileProperty
    @get:InputFile abstract val verifierScript: RegularFileProperty
    @get:Internal abstract val repositoryRoot: DirectoryProperty
    init { outputs.upToDateWhen { false } }
    @TaskAction fun verify() {
        val args = listOf("python3",verifierScript.get().asFile.absolutePath,
            "--root",repositoryRoot.get().asFile.absolutePath,
            "--lock",artifactLock.get().asFile.absolutePath) +
            selectedAars.files.sortedBy { it.absolutePath }.flatMap { listOf("--selected-aar",it.absolutePath) }
        check(selectedAars.files.size == 3) { "Exactly three reviewed Maven AARs required" }
        exec.exec { commandLine(args) }.assertNormalExitValue()
    }
}
