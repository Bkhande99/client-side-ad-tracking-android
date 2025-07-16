package com.mobitv.client.versioning

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*

const val VERSION_KEY = "VERSION_NAME"

const val DEBUG = false
@Suppress("UNCHECKED_CAST")
class VersionInfo(val versionPropertyFile: File) {

    private val properties = Properties()
    // Current Version information
    private val version: Version
    // Updated Version information
    private var updatedVersion: Version? = null

    init {
        properties.load(FileInputStream(versionPropertyFile))
        version = Version(properties[VERSION_KEY] as String)
    }

    fun getVersion(): String = version.versionName
    fun getUpdatedVersion(): String? = updatedVersion?.versionName

    // Convert today's date to an integer for use as version code. e.g. 10/29/2020 -> 20201029
    private fun createDateVersionCode(): Int {
        val today = SimpleDateFormat("yyyyMMdd").format(Date())
        return today.toInt()
    }

    private fun createNextVersion(version: String): String {
        val tokens = version.split(".").map { Integer.parseInt(it) }
        val major = tokens[0]
        val minor = tokens[1]
        val nextPatch = tokens[2] + 1
        return "$major.$minor.$nextPatch"
    }

    fun increaseVersionProperties() {
        val nextVersion = version.createNextVersion()
        updatedVersion = nextVersion
        writeUpdatedVersionProperties(versionPropertyFile)
    }

    private fun writeUpdatedVersionProperties(file: File) {
        val properties = Properties()
        properties[VERSION_KEY] = updatedVersion?.versionName ?: getVersion()
        properties.store(FileOutputStream(file), null)
    }
}