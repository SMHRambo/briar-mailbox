package org.briarproject.mailbox.core.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import org.briarproject.mailbox.core.DaggerTestComponent
import org.briarproject.mailbox.core.TestComponent
import org.briarproject.mailbox.core.TestModule
import org.briarproject.mailbox.core.TestUtils.getNewRandomContact
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
import org.briarproject.mailbox.core.settings.Settings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.io.File

@TestInstance(Lifecycle.PER_CLASS)
abstract class IntegrationTest {

    protected lateinit var testComponent: TestComponent
    private val lifecycleManager by lazy { testComponent.getLifecycleManager() }
    protected val httpClient = HttpClient(CIO) {
        expectSuccess = false // prevents exceptions on non-success responses
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }
    protected val baseUrl = "http://127.0.0.1:$PORT"

    protected val ownerToken = getNewRandomId()
    protected val token = getNewRandomId()
    protected val id = getNewRandomId()
    protected val contact1 = getNewRandomContact()
    protected val contact2 = getNewRandomContact()

    @BeforeAll
    fun setUp(@TempDir tempDir: File) {
        testComponent = DaggerTestComponent.builder().testModule(TestModule(tempDir)).build()
        testComponent.injectCoreEagerSingletons()
        lifecycleManager.startServices()
        lifecycleManager.waitForStartup()
        initDb()
    }

    open fun initDb() {
        // sub-classes can initialize the DB here as needed
    }

    @AfterAll
    fun tearDown() {
        lifecycleManager.stopServices()
        lifecycleManager.waitForShutdown()
    }

    protected fun addOwnerToken() {
        val settingsManager = testComponent.getSettingsManager()
        val settings = Settings()
        settings[SETTINGS_OWNER_TOKEN] = ownerToken
        settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE_OWNER)
    }

    protected fun addContact(c: Contact) {
        val db = testComponent.getDatabase()
        db.transaction(false) { txn ->
            db.addContact(txn, c)
        }
    }

    protected fun HttpRequestBuilder.authenticateWithToken(t: String) {
        headers {
            @Suppress("EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
            append(HttpHeaders.Authorization, "Bearer $t")
        }
    }

}
