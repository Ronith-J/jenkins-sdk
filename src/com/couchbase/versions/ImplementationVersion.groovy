package com.couchbase.versions

import groovy.transform.CompileStatic

@CompileStatic
class ImplementationVersion implements Comparable<ImplementationVersion> {
    public final int major;
    public final int minor;
    public final int patch;
    public final String snapshot;

    ImplementationVersion(int major, int minor, int patch, String snapshot) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.snapshot = snapshot
    }

    static ImplementationVersion from(String s) {
        int first = s.indexOf('.', 0)
        int second = s.indexOf('.', first + 1)
        String majorRaw = s.substring(0, first)
        String minorRaw = s.substring(first + 1, second)
        String restRaw = s.substring(second + 1)
        int major = Integer.parseInt(majorRaw)
        int minor = Integer.parseInt(minorRaw)

        if (restRaw.contains("-")) {
            // "3.3.3-20220715.074746-6"
            int index = restRaw.indexOf("-")
            return new ImplementationVersion(
                    major,
                    minor,
                    Integer.parseInt(restRaw.substring(0, index)),
                    restRaw.substring(index))
        }
        else {
            return new ImplementationVersion(major, minor, Integer.parseInt(restRaw), null)
        }
    }

    @Override
    String toString() {
        return "${major}.${minor}.${patch}${if (snapshot != null) snapshot else ""}"
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ImplementationVersion that = (ImplementationVersion) o

        if (major != that.major) return false
        if (minor != that.minor) return false
        if (patch != that.patch) return false
        if (snapshot != that.snapshot) return false

        return true
    }

    int hashCode() {
        int result
        result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (snapshot != null ? snapshot.hashCode() : 0)
        return result
    }

    public int compareTo(ImplementationVersion other) {
        if (equals(other)) return 0

        // Comparing the implementation versions following the SemVer specification (https://semver.org/)
        if (major < other.major) return -1
        if (major > other.major) return 1
        if (minor < other.minor) return -1
        if (minor > other.minor) return 1
        if (patch < other.patch) return -1;
        if (patch > other.patch) return 1;

        if (snapshot && other.snapshot) {
            // Anything after '+' is build metadata that doesn't affect ordering
            var identifiers = snapshot.substring(1).split('\\+').first().split('\\.')
            var otherIdentifiers = other.snapshot.substring(1).split('\\+').first().split('\\.')
            var shortestLength = Math.min(identifiers.length, otherIdentifiers.length)

            for (int i = 0; i < shortestLength; i++) {
                var identifier = identifiers[i]
                var otherIdentifier = otherIdentifiers[i]

                boolean isNumeric = identifier.chars().allMatch(Character::isDigit)
                boolean isOtherNumeric = otherIdentifier.chars().allMatch(Character::isDigit)

                // Numeric identifiers always have lower precedence than non-numeric identifiers.
                if (isNumeric && !isOtherNumeric) return -1
                if (!isNumeric && isOtherNumeric) return 1

                if (isNumeric && isOtherNumeric) {
                    // Identifiers consisting of only digits are compared numerically.
                    int num = Integer.parseInt(identifier)
                    int otherNum = Integer.parseInt(otherIdentifier)
                    if (num < otherNum) return -1
                    if (num > otherNum) return 1
                } else {
                    // Identifiers with letters or hyphens are compared lexically in ASCII sort order.
                    if (identifier < otherIdentifier) return -1
                    if (identifier > otherIdentifier) return 1
                }
            }
            return Integer.compare(identifiers.length, otherIdentifiers.length)
        }

        // When major, minor, and patch are equal, a pre-release version has lower precedence than a normal version
        if (snapshot == null) {
            return 1
        } else {
            return -1
        }
    }

    public boolean isBelow(ImplementationVersion other) {
        return this < other
    }

    public boolean isAbove(ImplementationVersion other) {
        return this > other
    }

    public static ImplementationVersion highest(Iterable<ImplementationVersion> versions) {
        return versions.max()
    }
}
