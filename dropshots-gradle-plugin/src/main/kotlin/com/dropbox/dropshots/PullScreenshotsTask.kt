package com.dropbox.dropshots

import java.io.ByteArrayOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public abstract class PullScreenshotsTask : DefaultTask() {

  @get:Input
  public abstract val adbExecutable: Property<String>

  @get:Input
  public abstract val screenshotDir: Property<String>

  @get:OutputDirectory
  public abstract val outputDirectory: DirectoryProperty

  @Inject
  protected abstract fun getExecOperations(): ExecOperations

  init {
    description = "Pull screenshots from the test device."
    group = "verification"
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public fun pullScreenshots() {
    val outputDir = outputDirectory.get().asFile
    outputDir.mkdirs()

    val adb = adbExecutable.get()
    val dir = screenshotDir.get()
    val checkResult = getExecOperations().exec {
      it.executable = adb
      it.args = listOf("shell", "test", "-d", dir)
      it.isIgnoreExitValue = true
    }

    if (checkResult.exitValue == 0) {
      val output = ByteArrayOutputStream()
      getExecOperations().exec {
        it.executable = adb
        it.args = listOf("pull", "$dir/.", outputDir.path)
        it.standardOutput = output
      }

      val fileCount = """^$dir/?\./: ([0-9]*) files pulled,.*$""".toRegex()
      val matchResult = fileCount.find(output.toString(Charsets.UTF_8))
      if (matchResult != null && matchResult.groups.size > 1) {
        println("${matchResult.groupValues[1]} screenshots saved at ${outputDir.path}")
      } else {
        println("Unknown result executing adb: $adb pull $dir/. ${outputDir.path}")
        print(output.toString(Charsets.UTF_8))
      }
    }
  }
}

