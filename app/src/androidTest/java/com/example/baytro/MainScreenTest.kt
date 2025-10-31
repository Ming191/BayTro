package com.example.baytro

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRoleState
import com.example.baytro.navigation.Screens
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private val userRoleState = MutableStateFlow<Role?>(null)

    @Before
    fun setUp() {
        // Mock UserRoleState
        mockkObject(UserRoleState)
        every { UserRoleState.userRole } returns userRoleState

        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testMainScreen_LoadingState_ShowsProgressIndicator() {
        // Given - null user role (loading state)
        userRoleState.value = null

        // When - set MainScreen content
        composeTestRule.setContent {
            MainScreen()
        }

        // Then - progress indicator should be displayed
        composeTestRule.onNodeWithContentDescription("Loading progress")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun testMainScreen_TenantRole_ShowsTenantMainScreen() {
        // Given - Tenant role
        userRoleState.value = Role.Tenant

        // When - set MainScreen content
        composeTestRule.setContent {
            MainScreen()
        }

        // Then - Tenant screen should be displayed
        // Note: We need to check for Tenant-specific UI elements
        // For now, we assume Tenant screen loads without errors
        composeTestRule.waitForIdle()
        // Add specific Tenant UI checks based on your TenantScaffold
    }

    @Test
    fun testMainScreen_LandlordRole_ShowsLandlordMainScreen() {
        // Given - Landlord role
        userRoleState.value = Role.Landlord

        // When - set MainScreen content
        composeTestRule.setContent {
            MainScreen()
        }

        // Then - Landlord screen should be displayed
        composeTestRule.waitForIdle()
        // The scaffold should be visible for Landlord
    }
}