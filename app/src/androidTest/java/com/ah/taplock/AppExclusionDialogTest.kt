package com.ah.taplock

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ah.taplock.ui.theme.TapLockTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppExclusionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val fakeApps = listOf(
        TapLockAppInfo("com.aegis.launcher", "Aegis Launcher"),
        TapLockAppInfo("com.android.vending", "Play Store"),
        TapLockAppInfo("com.example.notes", "Notes")
    )

    @Test
    fun searchFiltersTheVisibleAppList() {
        composeTestRule.setContent {
            var searchQuery by remember { mutableStateOf("") }
            TapLockTheme {
                AppExclusionDialog(
                    apps = fakeApps,
                    excludedPackages = emptySet(),
                    searchQuery = searchQuery,
                    isLoading = false,
                    onSearchQueryChange = { searchQuery = it },
                    onTogglePackage = {},
                    onClearAll = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("field_app_exclusions_search")
            .performTextReplacement("aegis")

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                composeTestRule.onAllNodesWithText("Aegis Launcher").fetchSemanticsNodes().size
            )
            assertTrue(
                composeTestRule.onAllNodesWithText("Play Store").fetchSemanticsNodes().isEmpty()
            )
            assertTrue(
                composeTestRule.onAllNodesWithText("Notes").fetchSemanticsNodes().isEmpty()
            )
        }
    }

    @Test
    fun searchShowsNoResultsStateWhenNothingMatches() {
        composeTestRule.setContent {
            var searchQuery by remember { mutableStateOf("") }
            TapLockTheme {
                AppExclusionDialog(
                    apps = fakeApps,
                    excludedPackages = emptySet(),
                    searchQuery = searchQuery,
                    isLoading = false,
                    onSearchQueryChange = { searchQuery = it },
                    onTogglePackage = {},
                    onClearAll = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("field_app_exclusions_search")
            .performTextReplacement("does-not-exist")

        composeTestRule.runOnIdle {
            assertEquals(
                1,
                composeTestRule.onAllNodesWithText(
                    context.getString(R.string.app_exclusions_no_results)
                ).fetchSemanticsNodes().size
            )
        }
    }

    @Test
    fun tappingAppRowReportsTheSelectedPackage() {
        var toggledPackage: String? = null

        composeTestRule.setContent {
            TapLockTheme {
                AppExclusionDialog(
                    apps = fakeApps,
                    excludedPackages = emptySet(),
                    searchQuery = "",
                    isLoading = false,
                    onSearchQueryChange = {},
                    onTogglePackage = { toggledPackage = it },
                    onClearAll = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("app_exclusion_row_com.aegis.launcher")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals("com.aegis.launcher", toggledPackage)
        }
    }

    @Test
    fun clearAllButtonInvokesCallbackWhenSelectionsExist() {
        var clearCalls = 0

        composeTestRule.setContent {
            TapLockTheme {
                AppExclusionDialog(
                    apps = fakeApps,
                    excludedPackages = setOf("com.aegis.launcher"),
                    searchQuery = "",
                    isLoading = false,
                    onSearchQueryChange = {},
                    onTogglePackage = {},
                    onClearAll = { clearCalls++ },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("button_app_exclusions_clear")
            .assertIsEnabled()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clearCalls)
        }
    }
}
