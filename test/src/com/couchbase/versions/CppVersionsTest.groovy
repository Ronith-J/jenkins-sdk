package com.couchbase.versions

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

class CppVersionsTest {
    @Test
    void latestSnapshotIsRelease() {
        def snapshot = CppVersions.getSnapshot("b7086c059659d3f1e03a8d9bff266fad0c6c9b89", false)
        assertEquals(1, snapshot.major)
        assertEquals(0, snapshot.minor)
        assertEquals(3, snapshot.patch)
        assertNull(snapshot.snapshot)
    }

    @Test
    void latestSnapshotIsReleaseCandidate() {
        def snapshot = CppVersions.getSnapshot("862fd4eb43327bc72ae494e49850bc04a39953e9", false)
        assertEquals(1, snapshot.major)
        assertEquals(0, snapshot.minor)
        assertEquals(3, snapshot.patch)
        assertEquals("-rc.1", snapshot.snapshot)
    }

    @Test
    void latestSnapshotHasNumberOfCommitsSinceRelease() {
        def snapshot = CppVersions.getSnapshot("aba976e4756cd5b9f4ba4c8ad9f99b215d1f2880", false)
        assertEquals(1, snapshot.major)
        assertEquals(0, snapshot.minor)
        assertEquals(4, snapshot.patch)
        assertEquals("-3+aba976e", snapshot.snapshot)
    }

    @Test
    void latestSnapshotHasNumberOfCommitsSinceReleaseDotMinorBump() {
        def snapshot = CppVersions.getSnapshot("aba976e4756cd5b9f4ba4c8ad9f99b215d1f2880", true)
        assertEquals(1, snapshot.major)
        assertEquals(1, snapshot.minor)
        assertEquals(0, snapshot.patch)
        assertEquals("-3+aba976e", snapshot.snapshot)
    }

    @Test
    void latestSnapshotHasNumberOfCommitsSinceReleaseCandidate() {
        def snapshot = CppVersions.getSnapshot("31fb90ead5d867b12a33154a1b3ff271d0c5ac79", false)
        assertEquals(1, snapshot.major)
        assertEquals(0, snapshot.minor)
        assertEquals(3, snapshot.patch)
        assertEquals("-rc.1.1+31fb90e", snapshot.snapshot)
    }

    @Test
    void versionOrder() {
        def versionList = Arrays.asList(
                CppVersions.getSnapshot("31fb90ead5d867b12a33154a1b3ff271d0c5ac79", false),
                CppVersions.getSnapshot("aba976e4756cd5b9f4ba4c8ad9f99b215d1f2880", false),
                CppVersions.getSnapshot("ae5370d8a1b4e0e047839f665ebbc0997629298b", false),
                ImplementationVersion.from("1.0.3-rc.2"),
                ImplementationVersion.from("1.0.2"),
                ImplementationVersion.from("1.0.3"),
                ImplementationVersion.from("1.0.3-rc.1"),
        )

        versionList.sort(true)

        assertEquals(
                Arrays.asList(
                        ImplementationVersion.from("1.0.2"),
                        ImplementationVersion.from("1.0.3-4+ae5370d"),
                        ImplementationVersion.from("1.0.3-rc.1"),
                        ImplementationVersion.from("1.0.3-rc.1.1+31fb90e"),
                        ImplementationVersion.from("1.0.3-rc.2"),
                        ImplementationVersion.from("1.0.3"),
                        ImplementationVersion.from("1.0.4-3+aba976e"),
                ),
                versionList
        )
    }
}
