import SwiftUI

/// A fullscreen overlay prompt that blocks interaction until the user signs in or dismisses.
/// Use as `.fullScreenCover(isPresented:)` or `.sheet(isPresented:)` from any view that
/// requires authentication (Cart, Profile, Reels interactions, etc.).
struct RequireLoginPromptView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var isVisible = false
    @State private var navigateToLogin = false
    @State private var navigateToRegister = false
    
    /// Optional context message explaining why login is needed
    var contextMessage: String = "Please sign in to like, bookmark, comment, and add items to your cart."
    
    var body: some View {
        NavigationStack {
            ZStack {
                // Gradient background overlay — blocks all underlying content
                LinearGradient(
                    colors: [
                        AppColors.primary.opacity(0.85),
                        AppColors.secondaryColor.opacity(0.9)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                // Content card
                VStack(spacing: 0) {
                    // Close button
                    HStack {
                        Spacer()
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white.opacity(0.8))
                                .padding(10)
                                .background(Circle().fill(.white.opacity(0.2)))
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    
                    Spacer()
                    
                    // Card content
                    VStack(spacing: 28) {
                        // Lock icon with animated appearance
                        ZStack {
                            Circle()
                                .fill(.white.opacity(0.15))
                                .frame(width: 140, height: 140)
                            
                            Circle()
                                .fill(.white.opacity(0.25))
                                .frame(width: 110, height: 110)
                            
                            Image(systemName: "lock.fill")
                                .font(.system(size: 44))
                                .foregroundColor(.white)
                        }
                        .scaleEffect(isVisible ? 1.0 : 0.5)
                        .opacity(isVisible ? 1.0 : 0)
                        
                        // Title and description
                        VStack(spacing: 12) {
                            Text("Sign In Required")
                                .font(.title.bold())
                                .foregroundColor(.white)
                            
                            Text(contextMessage)
                                .font(.body)
                                .foregroundColor(.white.opacity(0.85))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .opacity(isVisible ? 1.0 : 0)
                        .offset(y: isVisible ? 0 : 20)
                        
                        // Feature icons row
                        HStack(spacing: 24) {
                            FeatureIcon(icon: "heart.fill", label: "Like")
                            FeatureIcon(icon: "bookmark.fill", label: "Save")
                            FeatureIcon(icon: "cart.fill", label: "Cart")
                            FeatureIcon(icon: "bubble.left.fill", label: "Comment")
                        }
                        .opacity(isVisible ? 1.0 : 0)
                        .offset(y: isVisible ? 0 : 15)
                        
                        // Action buttons
                        VStack(spacing: 12) {
                            NavigationLink(destination: LoginView()) {
                                HStack {
                                    Image(systemName: "person.fill")
                                    Text("Sign In")
                                }
                                .font(Typography.button)
                                .foregroundColor(AppColors.primary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(.white)
                                .cornerRadius(14)
                                .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
                            }
                            
                            NavigationLink(destination: CreateAccountView()) {
                                HStack {
                                    Image(systemName: "person.badge.plus")
                                    Text("Create Account")
                                }
                                .font(Typography.button)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(.white, lineWidth: 2)
                                )
                            }
                            
                            Button {
                                dismiss()
                            } label: {
                                Text("Continue as Guest")
                                    .font(.subheadline.weight(.medium))
                                    .foregroundColor(.white.opacity(0.7))
                                    .padding(.vertical, 8)
                            }
                        }
                        .padding(.horizontal, 28)
                        .opacity(isVisible ? 1.0 : 0)
                        .offset(y: isVisible ? 0 : 30)
                    }
                    
                    Spacer()
                    Spacer()
                }
            }
            .onAppear {
                withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                    isVisible = true
                }
            }
        }
        .interactiveDismissDisabled(false)
    }
}

// MARK: - Feature Icon
private struct FeatureIcon: View {
    let icon: String
    let label: String
    
    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(Circle().fill(.white.opacity(0.2)))
            Text(label)
                .font(.caption2)
                .foregroundColor(.white.opacity(0.8))
        }
    }
}
}
