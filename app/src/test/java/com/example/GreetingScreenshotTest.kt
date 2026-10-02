package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.YouTubeVideo
import com.example.ui.components.VideoCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun videoCard_screenshot() {
    val sampleVideo = YouTubeVideo(
      id = "jfKfPfyJRdk",
      title = "lofi hip hop radio 📚 - beats to relax/study to",
      channelTitle = "Lofi Girl",
      thumbnailUrl = "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg",
      duration = "LIVE",
      viewCountText = "42K watching",
      publishedTimeText = "Live stream",
      isLive = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        VideoCard(
          video = sampleVideo,
          isSaved = true,
          onVideoClick = {},
          onToggleSave = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

