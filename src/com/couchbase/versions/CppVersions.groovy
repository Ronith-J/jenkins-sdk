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
        return getSnapshot(BRANCH, true) // The next release on main is now always a dot-minor.
    }

    static ImplementationVersion getSnapshot(String shaOrBranch, boolean nextReleaseIsDotMinor) {
        def attrs = GithubVersions.getSnapshotAttributes(REPO, shaOrBranch)
        return attrs.toImplementationVersion(nextReleaseIsDotMinor)
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
