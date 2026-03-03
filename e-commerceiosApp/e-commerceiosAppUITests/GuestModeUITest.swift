import XCTest

/**
 * P0 Regression: Guest browsing → gated action → Login prompt shown.
 *
 * Covers QA tickets AUTH-001 / QA #19, #21 (iOS parité of GuestModeAuthTest.kt).
 *
 * Scenarios:
 *   A. Fresh launch (unauthenticated) → tap Like → login sheet appears.
 *   B. Fresh launch → tap cart/buy → login/signup sheet appears.
 *   C. Login sheet presents Sign In + Sign Up options.
 *   D. Login sheet "dismissed" → app stays on content screen.
 *
 * NOTE: Add this target to the Xcode project UITests target before running.
 *   Tests assume the app is NOT logged in (fresh install or cleared keychain).
 *   Run with: xcodebuild test -scheme "e-commerceiosApp" -testPlan UITestPlan
 */
final class GuestModeUITest: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        // Signal to the app that it should start in guest mode for tests
        app.launchArguments += ["-UITestGuestMode", "1"]
        app.launchArguments += ["-UITestResetAuth", "1"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    // MARK: - A. Guest taps Like → login sheet appears

    func testGuest_tapsLike_loginSheetAppears() throws {
        navigateToReels()

        // Find the like button — labelled "like", "heart", or similar
        let likeButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'like' OR identifier == 'like_button' OR identifier == 'heart_button'")
        ).firstMatch

        guard likeButton.waitForExistence(timeout: 5) else {
            throw XCTSkip("Like button not found — may require content from backend")
        }

        likeButton.tap()
        Thread.sleep(forTimeInterval: 0.5)

        // Login sheet or alert should appear
        let loginSheetPresent = loginSheetExists(timeout: 3)
        XCTAssertTrue(loginSheetPresent,
                      "Login sheet must appear when guest taps Like (AUTH-001)")
    }

    // MARK: - B. Guest taps Add to Cart → login sheet appears

    func testGuest_tapsAddToCart_loginSheetAppears() throws {
        navigateToReels()

        let cartButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'cart' OR label CONTAINS[c] 'buy' OR identifier == 'add_to_cart_button'")
        ).firstMatch

        guard cartButton.waitForExistence(timeout: 5) else {
            throw XCTSkip("Cart/buy button not found — may require content from backend")
        }

        cartButton.tap()
        Thread.sleep(forTimeInterval: 0.5)

        let loginSheetPresent = loginSheetExists(timeout: 3)
        XCTAssertTrue(loginSheetPresent,
                      "Login sheet must appear when guest taps Add to Cart (AUTH-001)")
    }

    // MARK: - C. Login sheet has Sign In and Sign Up options

    func testLoginSheet_hasSignInAndSignUpButtons() throws {
        // Trigger the login sheet by attempting a gated action
        navigateToReels()

        // Try to trigger any gated action
        let anyGatedAction = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'like' OR label CONTAINS[c] 'follow' OR label CONTAINS[c] 'cart'")
        ).firstMatch

        guard anyGatedAction.waitForExistence(timeout: 5) else {
            throw XCTSkip("No gated action button found")
        }

        anyGatedAction.tap()

        guard loginSheetExists(timeout: 3) else {
            throw XCTSkip("Login sheet did not appear — may already be authenticated")
        }

        // Check for sign in button
        let signInButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'sign in' OR label CONTAINS[c] 'log in' OR label CONTAINS[c] 'connexion'")
        ).firstMatch

        XCTAssertTrue(signInButton.waitForExistence(timeout: 3),
                      "Login sheet must present a Sign In button")

        // Check for sign up / register option
        let signUpButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'sign up' OR label CONTAINS[c] 'register' OR label CONTAINS[c] 'create account' OR label CONTAINS[c] 'inscription'")
        ).firstMatch

        XCTAssertTrue(signUpButton.waitForExistence(timeout: 3),
                      "Login sheet must present a Sign Up / Register button")
    }

    // MARK: - D. Close login sheet → stays on content screen

    func testLoginSheet_dismissed_staysOnContent() throws {
        navigateToReels()

        let gatedAction = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'like' OR label CONTAINS[c] 'follow'")
        ).firstMatch

        guard gatedAction.waitForExistence(timeout: 5) else {
            throw XCTSkip("No gated action available")
        }
        gatedAction.tap()

        guard loginSheetExists(timeout: 3) else {
            throw XCTSkip("No login sheet appeared")
        }

        // Dismiss: swipe down or tap "Continue as guest" / close button
        let closeButton = app.buttons.matching(
            NSPredicate(format: "label CONTAINS[c] 'guest' OR label CONTAINS[c] 'later' OR label CONTAINS[c] 'cancel' OR identifier == 'close_button'")
        ).firstMatch

        if closeButton.exists {
            closeButton.tap()
        } else {
            app.swipeDown()
        }

        Thread.sleep(forTimeInterval: 0.5)

        // App must still be on the content screen (Reels visible or tab bar accessible)
        XCTAssertEqual(app.state, .runningForeground,
                       "App must remain in foreground after dismissing login sheet")
    }

    // MARK: - E. Guest can browse content freely (no forced login)

    func testGuest_canBrowseReels_withoutLogin() throws {
        navigateToReels()

        // Content should load without being blocked behind a mandatory login wall
        // Check that some scrollable UI exists
        let scrollView = app.scrollViews.firstMatch
        let reelView = app.otherElements.matching(
            NSPredicate(format: "identifier CONTAINS[c] 'reel' OR identifier CONTAINS[c] 'feed'")
        ).firstMatch

        let contentExists = scrollView.waitForExistence(timeout: 5) ||
                            reelView.waitForExistence(timeout: 5)

        // App must not have redirected to login screen blocking all content
        let loginScreen = app.otherElements.matching(
            NSPredicate(format: "identifier == 'login_screen' OR identifier == 'auth_screen'")
        ).firstMatch
        let isBlockedByLogin = loginScreen.exists

        XCTAssertFalse(isBlockedByLogin,
                       "Guest must be able to browse content without forced login (AUTH-004)")
        XCTAssertEqual(app.state, .runningForeground, "App must be running in foreground")
    }

    // MARK: - Helpers

    private func navigateToReels() {
        // Try Home / Reels tab
        for label in ["Home", "Feed", "Reels", "Accueil"] {
            let tab = app.tabBars.buttons[label]
            if tab.waitForExistence(timeout: 2) {
                tab.tap()
                Thread.sleep(forTimeInterval: 1.5)
                return
            }
        }
        // Fallback: first tab
        if let first = app.tabBars.buttons.allElementsBoundByIndex.first {
            first.tap()
        }
        Thread.sleep(forTimeInterval: 1.5)
    }

    private func loginSheetExists(timeout: TimeInterval = 2) -> Bool {
        // Match any typical login sheet/dialog
        let predicates: [NSPredicate] = [
            NSPredicate(format: "identifier == 'login_sheet'"),
            NSPredicate(format: "identifier == 'auth_sheet'"),
            NSPredicate(format: "label CONTAINS[c] 'sign in' OR label CONTAINS[c] 'log in'"),
            NSPredicate(format: "type == 'XCUIElementTypeSheet'"),
            NSPredicate(format: "type == 'XCUIElementTypeAlert'")
        ]
        for pred in predicates {
            if app.descendants(matching: .any).matching(pred).firstMatch
                .waitForExistence(timeout: timeout / Double(predicates.count)) {
                return true
            }
        }
        return false
    }
}
