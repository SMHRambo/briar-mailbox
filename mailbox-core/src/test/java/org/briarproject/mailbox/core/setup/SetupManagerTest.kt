package org.briarproject.mailbox.core.setup

import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetupManagerTest : IntegrationTest() {

    @Test
    fun `setup request gets rejected when using non-setup token`() = runBlocking {
        // initially, there's no setup and no owner token
        db.read { txn ->
            assertNull(setupManager.getSetupToken(txn))
            assertNull(setupManager.getOwnerToken(txn))
        }
        // owner token gets rejected
        assertEquals(
            HttpStatusCode.Unauthorized,
            httpClient.put<HttpResponse>("$baseUrl/setup") {
                authenticateWithToken(ownerToken)
            }.status
        )

        // now we set the owner token which still gets rejected
        setupManager.setToken(null, ownerToken)
        assertEquals(
            HttpStatusCode.Unauthorized,
            httpClient.put<HttpResponse>("$baseUrl/setup") {
                authenticateWithToken(ownerToken)
            }.status
        )

        // now we set the setup token, but use a different one for the request, so it gets rejected
        setupManager.setToken(token, null)
        assertEquals(
            HttpStatusCode.Unauthorized,
            httpClient.put<HttpResponse>("$baseUrl/setup") {
                authenticateWithToken(ownerToken)
            }.status
        )
    }

    @Test
    fun `setup request clears setup token and sets new owner token`() = runBlocking {
        // set a setup-token
        setupManager.setToken(token, null)

        // we are not yet set up
        assertNull(db.read { txn -> setupManager.getOwnerToken(txn) })

        // use it for setup PUT request
        val response: SetupResponse = httpClient.put("$baseUrl/setup") {
            authenticateWithToken(token)
        }
        // setup token got wiped and new owner token from response got stored
        db.read { txn ->
            assertNull(setupManager.getSetupToken(txn))
            assertEquals(setupManager.getOwnerToken(txn), response.token)
        }
        // setup token can no longer be used
        assertEquals(
            HttpStatusCode.Unauthorized,
            httpClient.put<HttpResponse>("$baseUrl/setup") {
                authenticateWithToken(token)
            }.status
        )
    }

    @Test
    fun `authentication doesn't work with empty string`() = runBlocking {
        // use it for setup PUT request
        val response: HttpResponse = httpClient.put("$baseUrl/setup") {
            authenticateWithToken("")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

}
