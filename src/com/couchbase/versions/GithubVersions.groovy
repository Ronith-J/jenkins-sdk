package com.couchbase.versions

import com.couchbase.tools.network.NetworkUtil
import groovy.json.JsonSlurper

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

    static String parseLinkHeaderForNext(URLConnection connection) {
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

    static Set<ImplementationVersion> getAllReleases(String repo) {
        def out = new HashSet<ImplementationVersion>()
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
                    out.add(ImplementationVersion.from(doc.name))
                }
                catch (err) {
                    System.err.println("Failed to add ${repo} version ${doc}")
                }
            }
        }

        return out
    }

    static int commitsSinceTag(String repo, String sha, String tag) {
        def tagSha = shaForTag(repo, tag)

        def url = "https://api.github.com/repos/${repo}/commits?sha=${sha}"
        int commitsSinceTag = 0

        while (url != null) {
            println url

            def commitsConnection = NetworkUtil.readRaw(url)
            def commitsParser = new JsonSlurper()
            def commitsJson = commitsParser.parseText(commitsConnection.getInputStream().getText())

            for (commit in commitsJson) {
                if (commit.sha == tagSha) {
                    return commitsSinceTag
                }
                commitsSinceTag++
            }
            url = parseLinkHeaderForNext(commitsConnection)
        }
        throw new RuntimeException("Could not find tag `${tag}` in GitHub commits for `${repo}`")
    }

    private static String shaForTag(String repo, String tagName) {
        def baseUrl = "https://api.github.com/repos/${repo}/tags"
        def nextUrl = baseUrl

        while (nextUrl != null) {
            println nextUrl
            def connection = NetworkUtil.readRaw(nextUrl)
            def parser = new JsonSlurper()
            def json = parser.parseText(connection.getInputStream().getText())
            nextUrl = parseLinkHeaderForNext(connection)

            for (tag in json) {
                if (tag.name == tagName) {
                    return tag.commit.sha
                }
            }
        }

        throw new RuntimeException("Could not find tag `${tagName}` in GitHub repo `${repo}`")
    }
}
