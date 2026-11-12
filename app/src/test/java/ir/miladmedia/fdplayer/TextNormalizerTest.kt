package ir.miladmedia.fdplayer

import ir.miladmedia.fdplayer.domain.util.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test fun arabicLettersAreUnified() {
        assertEquals("کتاب علی", TextNormalizer.normalize("كتاب  عليّ"))
    }

    @Test fun digitsBecomeAscii() {
        assertEquals("a123456", TextNormalizer.normalize("A١٢٣۴۵۶"))
    }

    @Test fun zwnjAndTatweelHandled() {
        assertEquals("می رود", TextNormalizer.normalize("می\u200Cرود"))
        assertEquals("سلام", TextNormalizer.normalize("سلـــام"))
    }

    @Test fun alefVariantsAndTaMarbuta() {
        assertEquals("ااا", TextNormalizer.normalize("أإآ"))
        assertEquals("مدرسه", TextNormalizer.normalize("مدرسة"))
    }

    @Test fun nameKeyCombinesTitleAndArtist() {
        assertEquals("song|artist", TextNormalizer.nameKey(" Song ", "ARTIST"))
    }
}
