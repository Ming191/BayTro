package com.example.baytro

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LandlordMainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLandlordMainScreen_DrawerNavigation_WorksCorrectly() = runTest {
        // Given
        val mockNavController = mockk<NavHostController>(relaxed = true)

        composeTestRule.setContent {
            // We need to create a wrapper since LandlordMainScreen uses rememberNavController
            val navController = rememberNavController()
            LandlordMainScreen()
        }

        // Wait for UI to load
        composeTestRule.waitForIdle()

        // Then - Check if drawer can be opened
        // Look for hamburger menu or drawer button
        composeTestRule.onNodeWithContentDescription("Navigation drawer")
            .or(composeTestRule.onNodeWithContentDescription("Menu"))
            .performClick()

        // Verify drawer content appears
        composeTestRule.onNodeWithText("Properties")
            .assertExists()

        composeTestRule.onNodeWithText("Tenants")
            .assertExists()

        composeTestRule.onNodeWithText("Maintenance")
            .assertExists()
    }

    @Test
    fun testLandlordMainScreen_NavigationItems_Displayed() = runTest {
        composeTestRule.setContent {
            LandlordMainScreen()
        }

        composeTestRule.waitForIdle()

        // Open drawer first
        composeTestRule.onNodeWithContentDescription("Navigation drawer")
            .or(composeTestRule.onNodeWithContentDescription("Menu"))
            .performClick()

        // Verify all navigation items are displayed
        val expectedMenuItems = listOf(
            "Properties",
            "Tenants",
            "Maintenance",
            "Dashboard",
            "Bills",
            "Contracts",
            "Services",
            "Chatbot"
        )

        expectedMenuItems.forEach { menuItem ->
            composeTestRule.onNodeWithText(menuItem)
                .assertExists()
                .assertIsDisplayed()
        }
    }
}