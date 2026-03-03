package com.project.e_commerce.data.remote.api

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * SoundApiService Ktor-mock tests — Section 3.2 of Technical Execution Plan.
 *
 * Tests 5 behavioural scenarios:
 *   1. getSound() full payload → all fields parsed
 *   2. getSound() partial payload → missing fields use defaults (resilience)
 *   3. getSound() 404 → throws exception
 *   4. getSounds() list → returns correct count
 *   5. incrementUsage() → returns usage count DTO
 */
class SoundApiServiceTest {

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun buildClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(lenientJson)
            }
        }
    }

    // ────────────────────────────────────────────
    // 1. getSound — full payload
    // ────────────────────────────────────────────

    @Test
    fun getSound_fullPayload_parsesAllFields() = runTest {
        val client = buildClient { request ->
            if (request.url.encodedPath.endsWith("/api/sounds/abc123")) {
                respond(
                    content = ByteReadChannel(
                        """{"id":1,"uid":"abc123","title":"Summer Vibes","artist":"DJ Test",
                           |"audioUrl":"https://cdn.test/summer.mp3","coverImageUrl":"https://cdn.test/cover.jpg",
                           |"duration":210.0,"usageCount":42,"createdAt":"2024-01-15T10:00:00Z"}"""
                            .trimMargin()
                    ),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
        val service = SoundApiService(client)
        val dto = service.getSound("abc123")

        assertEquals(1, dto.id)
        assertEquals("abc123", dto.uid)
        assertEquals("Summer Vibes", dto.title)
        assertEquals("DJ Test", dto.artist)
        assertEquals("https://cdn.test/summer.mp3", dto.audioUrl)
        assertEquals(210.0, dto.duration)
    }

    // ────────────────────────────────────────────
    // 2. getSound — partial payload uses defaults
    // ────────────────────────────────────────────

    @Test
    fun getSound_partialPayload_missingFieldsUseDefaults() = runTest {
        val client = buildClient { request ->
            if (request.url.encodedPath.contains("/api/sounds/")) {
                respond(
                    content = ByteReadChannel("""{"uid":"partial","title":"Partial Track"}"""),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
        val service = SoundApiService(client)
        val dto = service.getSound("partial")

        assertEquals("partial", dto.uid)
        assertEquals("Partial Track", dto.title)
        // Defaults
        assertEquals(0, dto.id)
        assertEquals("Unknown", dto.artist)
        assertEquals("", dto.audioUrl)
    }

    // ────────────────────────────────────────────
    // 3. getSound — empty JSON uses all defaults
    // ────────────────────────────────────────────

    @Test
    fun getSound_emptyJson_allDefaultsApplied() = runTest {
        val client = buildClient { _ ->
            respond(
                content = ByteReadChannel("{}"),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val service = SoundApiService(client)
        val dto = service.getSound("empty")

        assertEquals(0, dto.id)
        assertEquals("", dto.uid)
        assertEquals("Original Sound", dto.title)
        assertEquals("Unknown", dto.artist)
        assertEquals("", dto.audioUrl)
        assertFalse(dto.title.isEmpty(), "title default must not be blank")
    }

    // ────────────────────────────────────────────
    // 4. getSound — 404 throws exception
    // ────────────────────────────────────────────

    @Test
    fun getSound_notFound_throwsException() = runTest {
        val client = buildClient { _ ->
            respondError(HttpStatusCode.NotFound, content = """{"detail":"Sound not found"}""")
        }
        val service = SoundApiService(client)
        assertFailsWith<Exception> {
            service.getSound("nonexistent")
        }
    }

    // ────────────────────────────────────────────
    // 5. getSounds — list response returns all items
    // ────────────────────────────────────────────

    @Test
    fun getSounds_listResponse_returnsCorrectCount() = runTest {
        val client = buildClient { _ ->
            respond(
                content = ByteReadChannel(
                    """[
                        {"id":1,"uid":"s1","title":"Track 1","artist":"A1","audioUrl":"https://cdn.test/1.mp3"},
                        {"id":2,"uid":"s2","title":"Track 2","artist":"A2","audioUrl":"https://cdn.test/2.mp3"},
                        {"id":3,"uid":"s3","title":"Track 3","artist":"A3","audioUrl":"https://cdn.test/3.mp3"}
                    ]"""
                ),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val service = SoundApiService(client)
        val sounds = service.getSounds(limit = 10)

        assertEquals(3, sounds.size)
        assertEquals("Track 1", sounds[0].title)
        assertEquals("A2", sounds[1].artist)
        assertEquals("https://cdn.test/3.mp3", sounds[2].audioUrl)
    }

    // ────────────────────────────────────────────
    // 6. getSounds — empty list
    // ────────────────────────────────────────────

    @Test
    fun getSounds_emptyList_returnsEmpty() = runTest {
        val client = buildClient { _ ->
            respond(
                content = ByteReadChannel("[]"),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val service = SoundApiService(client)
        val result = service.getSounds()
        assertTrue(result.isEmpty(), "Empty list response should return empty list")
    }

    // ────────────────────────────────────────────
    // 7. incrementUsage — returns usage DTO
    // ────────────────────────────────────────────

    @Test
    fun incrementUsage_success_returnsUsageDto() = runTest {
        val client = buildClient { _ ->
            respond(
                content = ByteReadChannel(
                    """{"message":"Usage incremented","usage_count":43}"""
                ),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val service = SoundApiService(client)
        val response = service.incrementUsage("abc123")

        assertEquals(43, response.usageCount)
        assertEquals("Usage incremented", response.message)
    }

    // ────────────────────────────────────────────
    // 8. getSounds — url contains query parameters
    // ────────────────────────────────────────────

    @Test
    fun getSounds_withFilters_urlContainsQueryParams() = runTest {
        var capturedUrl = ""
        val client = buildClient { request ->
            capturedUrl = request.url.toString()
            respond(
                content = ByteReadChannel("[]"),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val service = SoundApiService(client)
        service.getSounds(search = "summer", genre = "pop", limit = 5)

        assertTrue(capturedUrl.contains("search=summer"), "URL should contain search param")
        assertTrue(capturedUrl.contains("genre=pop"), "URL should contain genre param")
        assertTrue(capturedUrl.contains("limit=5"), "URL should contain limit param")
    }
}
