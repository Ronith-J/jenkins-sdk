package com.couchbase.versions

import groovy.transform.Memoized

class CppVersions {
    private final static String REPO = "couchbase/couchbase-cxx-client"
    private final static String BRANCH = "main"

    @Memoized
    static Set<ImplementationVersion> getAllReleases() {
        def out = GithubVersions.getAllReleases(REPO)
        return withoutUnsupportedVersions(out)
    }

    @Memoized
    static ImplementationVersion getLatestSnapshot() {
        return getSnapshot(ImplementationVersion.highest(getAllReleases()), GithubVersions.getLatestSha(REPO, BRANCH))
    }

    static ImplementationVersion getSnapshot(ImplementationVersion nearestRelease, String sha) {
        def commitsSinceRelease = GithubVersions.commitsSinceTag(REPO, sha, nearestRelease.toString())
        if (commitsSinceRelease == 0) {
            return nearestRelease
        }

        String snapshot
        int patch = nearestRelease.patch
        if (nearestRelease.snapshot == null) {
            // This commit is _after_ a non-RC release - increment the patch
            patch += 1
            snapshot = "-${commitsSinceRelease}+${sha.substring(0, 7)}"
        } else {
            snapshot = "${nearestRelease.snapshot}.${commitsSinceRelease}+${sha.substring(0, 7)}"
        }

        return new ImplementationVersion(nearestRelease.major, nearestRelease.minor, patch, snapshot)
    }

    /**
     * Removes the versions that are not supported by the C++ Performer. Currently the unsupported versions are
     * all pre-GA developer previews.
     */
    private static Set<ImplementationVersion> withoutUnsupportedVersions(Set<ImplementationVersion> allVersions) {
        return allVersions.findAll( (v) -> {
            return !(v.major == 1 && v.minor == 0 && v.patch == 0) || v.snapshot == null
        })
    }
}
