package ai.aminrezaei.dataloggerapp

import ai.aminrezaei.dataloggerapp.ema.EmaWorker
import org.junit.Test
import org.junit.Assert.*

class EmaWorkerSuppressTest {

    @Test
    fun `suppress when active prompt exists`() {
        assertTrue(EmaWorker.shouldSuppressNewPrompt(hasActivePrompt = true))
    }

    @Test
    fun `do not suppress when no active prompt`() {
        assertFalse(EmaWorker.shouldSuppressNewPrompt(hasActivePrompt = false))
    }
}
