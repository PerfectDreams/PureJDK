#!/usr/bin/env kotlin

@file:DependsOn("org.jsoup:jsoup:1.24.1")
@file:DependsOn("org.jspecify:jspecify:1.0.1")
@file:DependsOn("org.jetbrains:annotations:26.1.0")

import org.jsoup.Jsoup
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlin.system.exitProcess

fun run(vararg cmd: String): String {
    val process = ProcessBuilder(*cmd)
        .inheritIO()
        .start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "${cmd.joinToString(" ")} failed" }
    return output.trim()
}

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun loadProperties(path: String): Properties {
    val props = Properties()
    val file = File(path)
    if (file.exists())
        file.inputStream().use { props.load(it) }
    return props
}

val BYPASS_STORED_VERSION_CHECK = System.getenv("BYPASS_STORED_VERSION_CHECK") == "true"
val OPENJDK_VERSION_REGEX = Regex("openjdk-([0-9.]+)")
val IMAGE_NAME = "ghcr.io/perfectdreams/purejdk"

val element = Jsoup.connect("https://jdk.java.net/")
    .get()
    .selectFirst("h1:contains(Ready for use: )")!!
    .select("a:matches(JDK [0-9]+)")

val link = Jsoup
    .connect("https://jdk.java.net${element.attr("href")}")
    .get()
    .selectFirst("a[href$=linux-x64_bin.tar.gz]")!!
    .attr("href")

val version = OPENJDK_VERSION_REGEX.find(link)!!.groupValues[1]
println("Latest version: $version")
val properties = loadProperties("openjdk.properties")

val storedVersion: String? = properties.getProperty("version")
val storedDate: String? = properties.getProperty("date")

if (!BYPASS_STORED_VERSION_CHECK && storedVersion == version) {
    println("Nothing to do, we are already up to date!")
    exitProcess(0)
}

val workDir = kotlin.io.path.createTempDirectory("openjdk-build").toFile()
val buildTimestamp = ZonedDateTime.now(ZoneOffset.UTC)
    .format(DateTimeFormatter.ofPattern("yyyyMMdd"))

var code = if (storedDate == buildTimestamp) {
    // Should NEVER be null here
    properties.getProperty("code").toInt() + 1
} else {
    1
}


try {
    val expectedHash = URI("$link.sha256").toURL().readText()

    val tarball = File(workDir, "openjdk.tar.gz")
    println("Downloading $link...")
    URI(link).toURL().openStream().use { input ->
        Files.copy(input, tarball.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    val actualHash = tarball.sha256()

    check(expectedHash == actualHash) { "Hash mismatch! Expected $expectedHash but found $actualHash" }

    val jdkDir = File(workDir, "openjdk")
    jdkDir.mkdirs()

    run("tar", "-zxvf", tarball.absolutePath, "--strip-components=1", "-C", jdkDir.path)

    val bases = listOf("ubuntu:26.04", "ubuntu:24.04")

    val builtTags = mutableListOf<String>()
    for (base in bases) {
        val tagsToBeBuilt = listOf(
            "$version-${base.replace(":", "-")}",
            "$version-${base.replace(":", "-")}-$buildTimestamp",
            "$version-${base.replace(":", "-")}-$buildTimestamp.$code"
        )

        run("docker", "build", "--pull", "-f", File("Dockerfile").absolutePath, "--build-arg", "BASE_IMAGE=${base}", *tagsToBeBuilt.flatMap { listOf("-t", "$IMAGE_NAME:$it") }.toTypedArray(), jdkDir.path)
        builtTags.addAll(tagsToBeBuilt)
    }

    for (tag in builtTags) {
        run("docker", "push", "$IMAGE_NAME:$tag")
    }

    properties.setProperty("version", version)
    properties.setProperty("sha256", actualHash)
    properties.setProperty("date", buildTimestamp)
    properties.setProperty("code", code.toString())

    File("openjdk.properties").outputStream().use { properties.store(it, null) }

    File("TAGS.md").writeText(
        buildString {
            appendLine("# Tags")

            for (tag in builtTags) {
                appendLine("* `$IMAGE_NAME:$tag`")
            }
        }
    )
    run("git", "add", "openjdk.properties")
    run("git", "add", "TAGS.md")

    val description = buildString {
        appendLine("Tags:")
        for (tag in builtTags) {
            appendLine("* $IMAGE_NAME:$tag")
        }
    }

    run("git", "commit", "-m", "Updated Upstream (OpenJDK $version / ${actualHash.take(7)})", "-m", description)
} finally {
    workDir.deleteRecursively()
}