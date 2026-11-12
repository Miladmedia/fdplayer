package ir.miladmedia.fdplayer

import ir.miladmedia.fdplayer.domain.util.Hashing
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class HashingTest {
    private val abc = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test fun knownVectorBytes() {
        assertEquals(abc, Hashing.sha256("abc".toByteArray()))
    }

    @Test fun streamMatchesBytes() {
        assertEquals(abc, Hashing.sha256(ByteArrayInputStream("abc".toByteArray())))
    }

    @Test fun emptyInput() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Hashing.sha256(ByteArray(0))
        )
    }
}
