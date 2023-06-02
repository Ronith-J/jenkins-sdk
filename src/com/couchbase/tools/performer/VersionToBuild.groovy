package com.couchbase.tools.performer

import com.couchbase.versions.ImplementationVersion

interface VersionToBuild {}

class VersionToBuildUtil {
    public static VersionToBuild from(String version, String sha) {
        if (version.startsWith("refs/")) {
            return new BuildGerrit(version)
        }
        else if (sha == null) {
            return new BuildVersion(version)
        }
        else if (version == null) {
            return new BuildSha(sha)
        }
        return new BuildShaVersion(version, sha)
    }
}

interface HasSha {
    String sha()
}

interface HasVersion {
    String version()

    default ImplementationVersion implementationVersion() {
        return ImplementationVersion.from(version())
    }
}

/**
 * Build the current main branch.  Usually used when we don't care about specific versions - e.g. when doing functional
 * testing.
 */
class BuildMain implements VersionToBuild {
    @Override
    public String toString() {
        return "BuildMain{}";
    }
}

/**
 * Build a specific SHA, e.g. "20e862d".  Usually used for a snapshot build.
 *
 * Note the tags processor doesn't get run when building a specific sha, since it's not easy to figure out which
 * SDK version it's associated with.  Generally this is not a problem, since building a specific sha is used for
 * building the latest snapshot, which generally does not require tagging.  If it ever does become a problem, then
 * finding a general solution will not be easy...
 */
record BuildSha(String sha) implements VersionToBuild, HasSha {
    @Override
    public String toString() {
        return "BuildSha{" +
                "sha='" + sha + '\'' +
                '}';
    }
}

/**
 * Build a specific SHA, e.g. "20e862d".  Usually used for a snapshot build.
 *
 * Unlike BuildSha, Here we do know the version this sha corresponds to, so the tag processor will be run.
 */
record BuildShaVersion(String version, String sha) implements VersionToBuild, HasSha, HasVersion {
    @Override
    public String toString() {
        return "BuildShaVersion{" +
                "version='" + version + '\'' +
                ", sha='" + sha + '\'' +
                '}';
    }
}

/**
 * Build a specific version, such as "3.4.6", of the SDK.
 */
record BuildVersion(String version) implements VersionToBuild, HasVersion {

    @Override
    public String toString() {
        return "BuildVersion{" +
                "version='" + version + '\'' +
                '}';
    }
}

/**
 * Build from a specific Gerrit changeset, e.g. "refs/changes/94/184294/1".
 *
 * For the same reasons as `BuildSha`, the tags processor is not run in this mode.
 */
record BuildGerrit(String gerrit) implements VersionToBuild {
    @Override
    public String toString() {
        return "BuildGerrit{" +
                "gerrit='" + gerrit + '\'' +
                '}';
    }
}