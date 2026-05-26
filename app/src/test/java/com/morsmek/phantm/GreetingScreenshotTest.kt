package com.morsmek.phantm

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.morsmek.phantm.screens.WelcomeScreen
import com.morsmek.phantm.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Ignore("Fails in headless/sandbox environment due to Roborazzi/Robolectric native graphics loading limitations on Windows")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun welcome_screen_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { WelcomeScreen(onCreateIdentity = {}, onRecoverIdentity = {}) } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/welcome.png")
  }
}
