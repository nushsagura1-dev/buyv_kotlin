import SwiftUI

struct OnboardingView: View {
    @State private var currentPage = 0
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding = false
    
    private let pages: [OnboardingPage] = [
        OnboardingPage(
            icon: "cart.fill",
            title: "Shop Anywhere",
            description: "Browse thousands of products from your favorite sellers, all in one place.",
            accentColor: AppColors.primary,
            gradientColors: [Color.orange.opacity(0.15), Color.orange.opacity(0.05)]
        ),
        OnboardingPage(
            icon: "play.rectangle.fill",
            title: "Watch & Discover",
            description: "Explore short videos showcasing products and trends from our community.",
            accentColor: AppColors.secondaryColor,
            gradientColors: [Color.blue.opacity(0.15), Color.blue.opacity(0.05)]
        ),
        OnboardingPage(
            icon: "person.2.fill",
            title: "Connect & Share",
            description: "Follow sellers, share your finds, and be part of the BuyV community.",
            accentColor: AppColors.chipsColor,
            gradientColors: [Color.green.opacity(0.15), Color.green.opacity(0.05)]
        )
    ]
    
    var body: some View {
        ZStack {
            // Background gradient per page
            pages[currentPage].gradientColors.first?
                .ignoresSafeArea()
                .animation(.easeInOut(duration: 0.5), value: currentPage)
            
            VStack(spacing: 0) {
                // Skip button
                HStack {
                    Spacer()
                    if currentPage < pages.count - 1 {
                        Button("Skip") {
                            hasSeenOnboarding = true
                        }
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                    }
                }
                
                // Page content
                TabView(selection: $currentPage) {
                    ForEach(0..<pages.count, id: \.self) { index in
                        onboardingPageView(page: pages[index])
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.3), value: currentPage)
                
                // Bottom card panel
                bottomPanel
            }
        }
    }
    
    // MARK: - Page Content
    private func onboardingPageView(page: OnboardingPage) -> some View {
        VStack(spacing: 32) {
            Spacer()
            
            // Icon with gradient background circle
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [page.accentColor.opacity(0.3), page.accentColor.opacity(0.1)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 160, height: 160)
                
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [page.accentColor.opacity(0.5), page.accentColor.opacity(0.2)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 120, height: 120)
                
                Image(systemName: page.icon)
                    .font(.system(size: 48))
                    .foregroundColor(page.accentColor)
            }
            
            VStack(spacing: 16) {
                Text(page.title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.primary)
                
                Text(page.description)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                    .lineSpacing(4)
            }
            
            Spacer()
            Spacer()
        }
    }
    
    // MARK: - Bottom Panel
    private var bottomPanel: some View {
        VStack(spacing: 24) {
            // Custom dot indicators
            HStack(spacing: 8) {
                ForEach(0..<pages.count, id: \.self) { index in
                    Capsule()
                        .fill(index == currentPage ? pages[currentPage].accentColor : Color.gray.opacity(0.3))
                        .frame(width: index == currentPage ? 24 : 8, height: 8)
                        .animation(.easeInOut(duration: 0.3), value: currentPage)
                }
            }
            
            // Navigation buttons
            HStack(spacing: 16) {
                // Back button
                if currentPage > 0 {
                    Button(action: {
                        withAnimation { currentPage -= 1 }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                                .font(.caption)
                            Text("Back")
                        }
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                    }
                }
                
                Spacer()
                
                // Next / Get Started
                if currentPage < pages.count - 1 {
                    Button(action: {
                        withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                            currentPage += 1
                        }
                    }) {
                        HStack(spacing: 8) {
                            Text("Next")
                            Image(systemName: "chevron.right")
                                .font(.caption)
                        }
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [pages[currentPage].accentColor, pages[currentPage].accentColor.opacity(0.8)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(12)
                        .shadow(color: pages[currentPage].accentColor.opacity(0.3), radius: 8, y: 4)
                    }
                } else {
                    Button(action: {
                        hasSeenOnboarding = true
                    }) {
                        HStack(spacing: 8) {
                            Text("Get Started")
                            Image(systemName: "arrow.right")
                                .font(.caption)
                        }
                        .font(.subheadline.weight(.bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            LinearGradient(
                                colors: [pages[currentPage].accentColor, pages[currentPage].accentColor.opacity(0.8)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .cornerRadius(14)
                        .shadow(color: pages[currentPage].accentColor.opacity(0.3), radius: 8, y: 4)
                    }
                }
            }
            .padding(.horizontal, 24)
            
            // Page counter
            Text("\(currentPage + 1) of \(pages.count)")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 24)
        .padding(.bottom, 16)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.08), radius: 12, y: -4)
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

// MARK: - Onboarding Page Model
private struct OnboardingPage {
    let icon: String
    let title: String
    let description: String
    let accentColor: Color
    let gradientColors: [Color]
}
