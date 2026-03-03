package com.project.e_commerce.data.remote.dto

import com.project.e_commerce.domain.model.Sound
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for SoundDto serialization resilience — SOUND-001 regression coverage.
 *
 * Verifies that missing / null fields from the backend never throw
 * MissingFieldException and always produce a usable Sound domain object.
 */
class SoundDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --------- SoundDto serialization ---------

    @Test
    fun soundDto_fullPayload_deserializesCorrectly() {
        val payload = """
            {
              "id": 42,
              "uid": "snd_abc123",
              "title": "My Track",
              "artist": "DJ Test",
              "audio_url": "https://cdn.test/track.mp3",
              "cover_image": "https://cdn.test/cover.jpg",
              "duration": 180,
              "created_at": "2024-01-15T10:00:00Z"
            }
        """.trimIndent()
        val dto = json.decodeFromString<SoundDto>(payload)
        assertEquals(42, dto.id)
        assertEquals("snd_abc123", dto.uid)
        assertEquals("My Track", dto.title)
        assertEquals("DJ Test", dto.artist)
    }

    @Test
    fun soundDto_missingAllFields_usesDefaults() {
        // Simulates backend returning {} — should NOT throw MissingFieldException
        val dto = json.decodeFromString<SoundDto>("{}")
        assertEquals(0, dto.id)
        assertEquals("", dto.uid)
        assertEquals("Original Sound", dto.title)
        assertEquals("Unknown", dto.artist)
        assertEquals("", dto.audioUrl)
    }

    @Test
    fun soundDto_missingAudioUrl_defaultsToEmpty() {
        val payload = """{"uid": "snd_xyz", "title": "No URL"}"""
        val dto = json.decodeFromString<SoundDto>(payload)
        assertEquals("", dto.audioUrl)
    }

    @Test
    fun soundDto_nullableFields_acceptNull() {
        val payload = """
            {
              "uid": "snd_001",
              "audio_url": "https://cdn.test/a.mp3",
              "cover_image": null,
              "waveform_url": null
            }
        """.trimIndent()
        // Should not throw
        val dto = json.decodeFromString<SoundDto>(payload)
        assertEquals("snd_001", dto.uid)
    }

    @Test
    fun soundDto_unknownFields_ignored() {
        val payload = """
            {
              "uid": "snd_new",
              "audio_url": "https://cdn.test/track.mp3",
              "future_field": "some new backend field"
            }
        """.trimIndent()
        val dto = json.decodeFromString<SoundDto>(payload)
        assertEquals("snd_new", dto.uid)
    }

    // --------- Sound domain model defaults & isValid ---------

    @Test
    fun sound_defaultConstructor_isValid_false() {
        val sound = Sound()
        assertFalse(sound.isValid, "Default Sound with empty uid/audioUrl should not be valid")
    }

    @Test
    fun sound_withUidAndAudioUrl_isValid_true() {
        val sound = Sound(uid = "snd_ok", audioUrl = "https://cdn.test/ok.mp3")
        assertTrue(sound.isValid)
    }

    @Test
    fun sound_blankUid_isValid_false() {
        val sound = Sound(uid = "   ", audioUrl = "https://cdn.test/ok.mp3")
        assertFalse(sound.isValid)
    }

    @Test
    fun sound_blankAudioUrl_isValid_false() {
        val sound = Sound(uid = "snd_ok", audioUrl = "")
        assertFalse(sound.isValid)
    }
}
