package com.example.baytro

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ---------- CASE 13 ----------
    // Đăng nhập thành công
    @Test
    fun signIn_Success() {
        composeTestRule.onNodeWithTag("emailField").performTextInput("baytro.dev@gmail.com")
        composeTestRule.onNodeWithTag("passwordField").performTextInput("Baytro123")
        composeTestRule.onNodeWithTag("signInButton").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ---------- CASE 14 ----------
    // Sai email hoặc mật khẩu
    @Test
    fun signIn_InvalidCredentials() {
        composeTestRule.onNodeWithTag("emailField").performTextInput("baytro.dev")
        composeTestRule.onNodeWithTag("passwordField").performTextInput("wrong password")
        composeTestRule.onNodeWithTag("signInButton").performClick()

        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    // ---------- CASE 15 ----------
    // Thiếu trường dữ liệu
    @Test
    fun signIn_MissingFields() {
        composeTestRule.onNodeWithTag("emailField").performTextInput("")
        composeTestRule.onNodeWithTag("passwordField").performTextInput("")
        composeTestRule.onNodeWithTag("signInButton").performClick()

        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }
}
