package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.json.JsonSlurper

class GithubSnapshotAttributes {
    public String sha
    public ImplementationVersion nearestRelease
    public int commitsSinceRelease

    GithubSnapshotAttributes(String sha, ImplementationVersion nearestRelease, int commitsSinceRelease) {
        this.sha = sha
        this.nearestRelease = nearestRelease
        this.commitsSinceRelease = commitsSinceRelease
    }

    ImplementationVersion toImplementationVersion(boolean maybeIncrementMinor) {
        int major = this.nearestRelease.major
        int minor = this.nearestRelease.minor
        int patch = this.nearestRelease.patch
        String snapshot
        if (this.commitsSinceRelease > 0) {
            if (this.nearestRelease.snapshot == null) {
                // This commit is _after_ a non-RC release - increment the patch or minor
                if (maybeIncrementMinor) {
                    patch = 0
                    minor++
                } else {
                    patch += 1
                }
                snapshot = "-${commitsSinceRelease}+${sha.substring(0, 7)}"
            } else {
                snapshot = "${nearestRelease.snapshot}.${commitsSinceRelease}+${sha.substring(0, 7)}"
            }
        } else {
            snapshot = this.nearestRelease.snapshot
        }
        return new ImplementationVersion(major, minor, patch, snapshot)
    }

    String toString() {
        return "sha=${this.sha}, nearestRelease=${this.nearestRelease}, commitsSinceRelease=${commitsSinceRelease}"
    }
}

class GithubVersions {
    @Deprecated // SDKs should use getLatestShaWithDatetime so that results are chronologically sorted
    static String getLatestSha(String repo, String branch) {
        def json = NetworkUtil.readJson("https://api.github.com/repos/${repo}/commits/${branch}")
        String sha = json.sha
        return sha.substring(0, 7)
    }

    /**
     * Makes a more useful snapshot version by putting the datetime in, so it appears in chronological order in graphs etc.
     *
     * Note this has some limitations.  A snapshot for 3.4.5 is actually _after_ 3.4.5 is released, rather than the
     * JVM definition of a SNAPSHOT build, which would come before 3.4.5 releases.  The latter is a more useful
     * definition, but is hard to achieve.
     */
    static String getLatestShaWithDatetime(String repo, String branch) {
        def json = NetworkUtil.readJson("https://api.github.com/repos/${repo}/commits/${branch}")
        String sha = json.sha
        String commitDate = json.commit.committer.date
        String[] parts = commitDate.split("T")
        String date = parts[0].replaceAll("[^0-9]", "")
        String time = parts[1].replaceAll("[^0-9]", "")
        // Why 7 characters here but 6 characters in getLatestSha?  Because it was in the code that's being refactored
        // here.  It's probably not strictly necessary, but keeping it so the SDK Performance results remain consistent.
        // Use '+' to append the SHA, to treat it as build metadata that's ignored in semver sorting.
        return date + "." + time + "+" + sha.substring(0, 7)
    }

    static GithubSnapshotAttributes getSnapshotAttributes(String repo, String shaOrBranch) {
        def tags = getAllReleasesWithCommitSha(repo)

        def url = "https://api.github.com/repos/${repo}/commits?sha=${shaOrBranch}"
        int commitsSinceTag = 0
        String snapshotSha = null

        while (url != null) {
            println url

            def commitsConnection = NetworkUtil.readRaw(url)
            def commitsParser = new JsonSlurper()
            def commitsJson = commitsParser.parseText(commitsConnection.getInputStream().getText())

            for (commit in commitsJson) {
                if (snapshotSha == null) {
                    snapshotSha = commit.sha.substring(0, 7)
                }

                if (tags.containsKey(commit.sha)) {
                    // This is the first commit we found that's been tagged as a release
                    return new GithubSnapshotAttributes(snapshotSha, tags.get(commit.sha), commitsSinceTag)
                }
                commitsSinceTag++
            }
            url = parseLinkHeaderForNext(commitsConnection)
        }
        throw new RuntimeException("Could not find the nearest tag for ${shaOrBranch} in ${repo}")
    }

    static Set<ImplementationVersion> getAllReleases(String repo) {
        return getAllReleasesWithCommitSha(repo).values()
    }

    private static Map<String, ImplementationVersion> getAllReleasesWithCommitSha(String repo) {
        def out = new HashMap<String, ImplementationVersion>()
        def baseUrl = "https://api.github.com/repos/${repo}/tags"
        def nextUrl = baseUrl

        while (nextUrl != null) {
            println nextUrl
            def connection = NetworkUtil.readRaw(nextUrl)
            def parser = new JsonSlurper()
            def json = parser.parseText(connection.getInputStream().getText())
            nextUrl = parseLinkHeaderForNext(connection)

            for (doc in json) {
                try {
                    out.put(doc.commit.sha, ImplementationVersion.from(doc.name))
                }
                catch (err) {
                    System.err.println("Failed to add ${repo} version ${doc}")
                }
            }
        }

        return out
    }

    private static String parseLinkHeaderForNext(URLConnection connection) {
        // <https://api.github.com/repositories/2071017/tags?page=2>; rel="next", <https://api.github.com/repositories/2071017/tags?page=7>; rel="last"
        try {
            if (connection.getHeaderFields() == null || !connection.getHeaderFields().containsKey("Link")) {
                return null
            }
            def linkHeaders = connection.getHeaderFields().get("Link").get(0)
            if (!linkHeaders.contains("rel=\"next\"")) {
                return null
            }
            linkHeaders.split(",")
                    .find { it.contains("rel=\"next\"") }
                    .split(";")[0]
                    .replace('<', '')
                    .replace('>', '')
        }
        catch (RuntimeException err) {
            err.printStackTrace()
            throw new RuntimeException("Unable to parse headers from ${connection.getHeaderFields()}")
        }
    }
}
