package com.example.pooltracker

import android.content.Context
import android.graphics.Bitmap
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Captures Play Store screenshots on an emulator with sample games loaded.
 * Run by .github/workflows/screenshots.yml, not part of the normal build.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val outDir = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }

    private fun argb(i: Int) = TeamColorChoices[i].let {
        (0xFF shl 24) or ((it.red * 255).toInt() shl 16) or ((it.green * 255).toInt() shl 8) or (it.blue * 255).toInt()
    }

    /** Builds a history where results follow [pattern] (1-based winners), spread over recent weeks. */
    private fun history(pattern: String): MutableList<GameResult> {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val n = pattern.length
        return pattern.mapIndexed { i, c ->
            // Sessions of ~5 games, a few days apart, the latest one tonight.
            val session = (n - 1 - i) / 5
            val inSession = (n - 1 - i) % 5
            GameResult(now - session * 72 * hour - inSession * hour / 3 - 20 * 60_000L, c.digitToInt())
        }.toMutableList()
    }

    private val rivalry = Matchup(
        teams = listOf(
            Team(players = listOf("Mike"), colorArgb = argb(1)),
            Team(players = listOf("Dave"), colorArgb = argb(2))
        ),
        history = history("1211221121212211112121112"),
        solidsTeam = 1
    )
    private val doubles = Matchup(
        teams = listOf(
            Team(name = "The Sharks", players = listOf("Jess", "Tony"), colorArgb = argb(3)),
            Team(name = "Corner Pocket", players = listOf("Sam", "Rico"), colorArgb = argb(6))
        ),
        history = history("21122121221"),
        solidsTeam = 2
    )
    private val threeWay = Matchup(
        teams = listOf(
            Team(players = listOf("Kev"), colorArgb = argb(4)),
            Team(players = listOf("Luis"), colorArgb = argb(5)),
            Team(players = listOf("Andre"), colorArgb = argb(8))
        ),
        history = history("1323121332213")
    )

    private fun shoot(name: String, seed: List<Matchup>, action: () -> Unit = {}) {
        Storage.save(context, seed)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            device.wait(Until.hasObject(By.textContains("WhoStarts")), 10_000)
            Thread.sleep(1_500)
            dismissSystemDialogs()
            action()
            device.waitForIdle()
            dismissSystemDialogs()
            // Crop off the nav bar / tablet taskbar so only the status bar and app show.
            var navBottom = 0
            scenario.onActivity { activity ->
                val insets = WindowInsetsCompat.toWindowInsetsCompat(activity.window.decorView.rootWindowInsets)
                navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            }
            val shot = instrumentation.uiAutomation.takeScreenshot()
            val cropped = Bitmap.createBitmap(shot, 0, 0, shot.width, shot.height - navBottom)
            File(outDir, "$name.png").outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** Slow emulators sometimes pop "X isn't responding"; tap Wait so it's gone. */
    private fun dismissSystemDialogs() {
        repeat(3) {
            val wait = device.findObject(By.text("Wait")) ?: return
            if (!device.hasObject(By.textContains("isn't responding"))) return
            wait.click()
            Thread.sleep(1_000)
        }
    }

    private fun tap(selector: androidx.test.uiautomator.BySelector) {
        device.wait(Until.findObject(selector), 10_000)!!.click()
    }

    @Test
    fun captureAll() {
        shoot("1-matchups", listOf(rivalry, doubles, threeWay))

        shoot("2-history", listOf(rivalry, doubles, threeWay)) {
            val history = device.wait(Until.findObject(By.textStartsWith("History (")), 10_000)!!
            val c = history.visibleCenter
            device.executeShellCommand("input tap ${c.x} ${c.y}")
            // Retry once if the dialog didn't open.
            if (!device.wait(Until.hasObject(By.text("Momentum")), 4_000)) {
                device.executeShellCommand("input tap ${c.x} ${c.y}")
            }
            Thread.sleep(2_500)
        }

        shoot("3-coin-flip", listOf(
            Matchup(teams = listOf(
                Team(players = listOf("Mike"), colorArgb = argb(1)),
                Team(players = listOf("Dave"), colorArgb = argb(2))
            )),
            doubles
        )) {
            tap(By.textContains("Flip a coin"))
            tap(By.text("Flip"))
            Thread.sleep(4_000)
        }

        shoot("4-wheel-spin", listOf(
            Matchup(teams = listOf(
                Team(players = listOf("Kev"), colorArgb = argb(4)),
                Team(players = listOf("Luis"), colorArgb = argb(5)),
                Team(players = listOf("Andre"), colorArgb = argb(8)),
                Team(players = listOf("Jess"), colorArgb = argb(3))
            )),
            rivalry
        )) {
            tap(By.textContains("Spin to decide"))
            tap(By.text("Spin"))
            Thread.sleep(4_000)
        }
    }
}
