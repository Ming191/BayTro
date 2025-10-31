package com.example.baytro.view.navigationType

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationDrawerViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationDrawerView_AllMenuItems_Displayed() = runTest {
        // Given
        val mockOnPropertyClicked: () -> Unit = mockk(relaxed = true)
        val mockOnTenantClicked: () -> Unit = mockk(relaxed = true)
        val mockOnMaintenanceClicked: () -> Unit = mockk(relaxed = true)
        val mockOnDashboardClicked: () -> Unit = mockk(relaxed = true)
        val mockOnBillClicked: () -> Unit = mockk(relaxed = true)
        val mockOnContractClicked: () -> Unit = mockk(relaxed = true)
        val mockOnServiceClicked: () -> Unit = mockk(relaxed = true)
        val mockOnChatbotClicked: () -> Unit = mockk(relaxed = true)
        val mockOnDrawerClicked: () -> Unit = mockk(relaxed = true)

        // When
        composeTestRule.setContent {
            NavigationDrawerView(
                currentRoute = Screens.Dashboard.route,
                onDrawerClicked = mockOnDrawerClicked,
                onPropertyClicked = mockOnPropertyClicked,
                onTenantClicked = mockOnTenantClicked,
                onMaintenanceClicked = mockOnMaintenanceClicked,
                onDashboardClicked = mockOnDashboardClicked,
                onBillClicked = mockOnBillClicked,
                onContractClicked = mockOnContractClicked,
                onServiceClicked = mockOnServiceClicked,
                onChatbotClicked = mockOnChatbotClicked
            )
        }

        // Then - Verify all menu items are displayed
        val menuItems = listOf(
            "Properties",
            "Tenants",
            "Maintenance",
            "Dashboard",
            "Bills",
            "Contracts",
            "Services",
            "Chatbot"
        )

        menuItems.forEach { item ->
            composeTestRule.onNodeWithText(item, ignoreCase = true)
                .assertExists()
                .assertIsDisplayed()
        }
    }

    @Test
    fun testNavigationDrawerView_MenuItemClicks_TriggerCallbacks() = runTest {
        // Given
        var propertyClicked = false
        var tenantClicked = false
        var maintenanceClicked = false

        composeTestRule.setContent {
            NavigationDrawerView(
                currentRoute = Screens.Dashboard.route,
                onDrawerClicked = { },
                onPropertyClicked = { propertyClicked = true },
                onTenantClicked = { tenantClicked = true },
                onMaintenanceClicked = { maintenanceClicked = true },
                onDashboardClicked = { },
                onBillClicked = { },
                onContractClicked = { },
                onServiceClicked = { },
                onChatbotClicked = { }
            )
        }

        // When - Click on Properties
        composeTestRule.onNodeWithText("Properties", ignoreCase = true)
            .performClick()

        // Then
        assert(propertyClicked)

        // Reset and test another item
        propertyClicked = false

        // When - Click on Tenants
        composeTestRule.onNodeWithText("Tenants", ignoreCase = true)
            .performClick()

        // Then
        assert(tenantClicked)
    }
}