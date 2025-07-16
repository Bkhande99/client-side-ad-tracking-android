package com.mobitv.client.versioning

import java.lang.IllegalStateException

class Version(val versionName: String) {

    constructor(major: Int, minor: Int, patch: Int):
            this("$major.$minor.$patch")

    // Version is of format #.#.# = <major>.<minor>.<patch>
    val majorVersion: Int
    val minorVersion: Int
    val patchVersion: Int

    init {
        val tokens = versionName.split(".").map { Integer.parseInt(it) }
        if (tokens.size != 3) {
            throw IllegalStateException("Invalid version format: $versionName !" +
                    " Expected a 3 component version of format #.#.#")
        }
        majorVersion = tokens[0]
        minorVersion = tokens[1]
        patchVersion = tokens[2]
    }

    fun createNextVersion(): Version {
        return Version(majorVersion, minorVersion, patchVersion + 1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Version

        if (versionName != other.versionName) return false

        return true
    }

    override fun hashCode(): Int {
        return versionName.hashCode()
    }
}