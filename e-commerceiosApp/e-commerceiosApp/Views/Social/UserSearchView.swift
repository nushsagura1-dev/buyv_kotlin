import SwiftUI
import Shared

struct UserSearchView: View {
    @StateObject private var viewModel = SocialViewModel()
    @State private var query = ""
    @State private var isSearching = false
    @AppStorage("recentUserSearches") private var recentSearchesData: String = ""
    
    private var recentSearches: [String] {
        recentSearchesData.isEmpty ? [] : recentSearchesData.components(separatedBy: "|||")
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search Bar
                searchBar
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                
                Divider()
                
                // Content
                if viewModel.isLoading {
                    loadingView
                } else if !viewModel.searchResults.isEmpty {
                    searchResultsList
                } else if isSearching && !query.isEmpty && viewModel.searchResults.isEmpty {
                    noResultsView
                } else if !recentSearches.isEmpty && query.isEmpty {
                    recentSearchesSection
                } else {
                    emptyStateView
                }
            }
            .navigationTitle("Discover")
            .navigationBarTitleDisplayMode(.large)
        }
    }
    
    // MARK: - Search Bar
    private var searchBar: some View {
        HStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.gray)
                    .font(.subheadline)
                
                TextField("Search users...", text: $query)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onChange(of: query) { newValue in
                        debounceSearch(newValue)
                    }
                    .onSubmit {
                        performSearch()
                    }
                
                if !query.isEmpty {
                    Button(action: {
                        query = ""
                        viewModel.searchResults = []
                        isSearching = false
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                            .font(.subheadline)
                    }
                }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(12)
            
            if !query.isEmpty {
                Button("Cancel") {
                    query = ""
                    viewModel.searchResults = []
                    isSearching = false
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }
                .foregroundColor(AppColors.primary)
                .font(.subheadline)
                .transition(.move(edge: .trailing).combined(with: .opacity))
                .animation(.easeInOut(duration: 0.2), value: query.isEmpty)
            }
        }
    }
    
    // MARK: - Search Results List
    private var searchResultsList: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(viewModel.searchResults, id: \.uid) { user in
                    NavigationLink(destination: UserProfileView(user: user)) {
                        UserRowView(user: user)
                            .padding(.horizontal)
                            .padding(.vertical, 6)
                    }
                    .buttonStyle(.plain)
                    .simultaneousGesture(TapGesture().onEnded {
                        saveRecentSearch(user.displayName)
                    })
                    
                    Divider().padding(.leading, 76)
                }
            }
            .padding(.top, 8)
        }
        .refreshable {
            if !query.trimmingCharacters(in: .whitespaces).isEmpty {
                viewModel.searchUsers(query: query)
            }
        }
    }
    
    // MARK: - Recent Searches
    private var recentSearchesSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Recent Searches")
                    .font(.headline)
                    .foregroundColor(.primary)
                Spacer()
                Button("Clear All") {
                    withAnimation {
                        recentSearchesData = ""
                    }
                }
                .font(.subheadline)
                .foregroundColor(AppColors.primary)
            }
            .padding(.horizontal)
            .padding(.top, 16)
            .padding(.bottom, 8)
            
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(recentSearches, id: \.self) { search in
                        Button(action: {
                            query = search
                            performSearch()
                        }) {
                            HStack(spacing: 12) {
                                Image(systemName: "clock.arrow.circlepath")
                                    .foregroundColor(.gray)
                                    .font(.subheadline)
                                    .frame(width: 24)
                                
                                Text(search)
                                    .foregroundColor(.primary)
                                    .font(.body)
                                
                                Spacer()
                                
                                Button(action: {
                                    removeRecentSearch(search)
                                }) {
                                    Image(systemName: "xmark")
                                        .foregroundColor(.gray)
                                        .font(.caption)
                                }
                            }
                            .padding(.horizontal)
                            .padding(.vertical, 12)
                        }
                        
                        Divider().padding(.leading, 52)
                    }
                }
            }
        }
    }
    
    // MARK: - Loading View
    private var loadingView: some View {
        VStack(spacing: 12) {
            Spacer()
            ProgressView()
            Text("Searching...")
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
        }
    }
    
    // MARK: - No Results View
    private var noResultsView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "person.slash")
                .font(.system(size: 48))
                .foregroundColor(.gray.opacity(0.4))
            Text("No users found")
                .font(.headline)
                .foregroundColor(.primary)
            Text("Try searching with a different name or username")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, 32)
    }
    
    // MARK: - Empty State
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Spacer()
            ZStack {
                Circle()
                    .fill(AppColors.primary.opacity(0.1))
                    .frame(width: 100, height: 100)
                Image(systemName: "person.2.fill")
                    .font(.system(size: 40))
                    .foregroundColor(AppColors.primary.opacity(0.5))
            }
            Text("Discover People")
                .font(.headline)
                .foregroundColor(.primary)
            Text("Search for friends, creators, and sellers to follow")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.horizontal, 32)
    }
    
    // MARK: - Search Logic
    private var searchTask: DispatchWorkItem? = nil
    
    private func debounceSearch(_ text: String) {
        NSObject.cancelPreviousPerformRequests(withTarget: self)
        
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            viewModel.searchResults = []
            isSearching = false
            return
        }
        
        isSearching = true
        
        // Debounce 400ms
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { [self] in
            if self.query == text && !text.isEmpty {
                viewModel.searchUsers(query: text)
            }
        }
    }
    
    private func performSearch() {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        isSearching = true
        saveRecentSearch(trimmed)
        viewModel.searchUsers(query: trimmed)
    }
    
    // MARK: - Recent Searches Persistence
    private func saveRecentSearch(_ term: String) {
        var searches = recentSearches.filter { $0 != term }
        searches.insert(term, at: 0)
        if searches.count > 10 { searches = Array(searches.prefix(10)) }
        recentSearchesData = searches.joined(separator: "|||")
    }
    
    private func removeRecentSearch(_ term: String) {
        let searches = recentSearches.filter { $0 != term }
        withAnimation {
            recentSearchesData = searches.joined(separator: "|||")
        }
    }
}

// MARK: - User Row View
struct UserRowView: View {
    let user: UserProfile
    
    var body: some View {
        HStack(spacing: 12) {
            if let urlString = user.profileImageUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: .fill)
                    case .failure:
                        initialsAvatar
                    default:
                        Circle().fill(Color.gray.opacity(0.2))
                            .overlay(ProgressView().scaleEffect(0.6))
                    }
                }
                .frame(width: 50, height: 50)
                .clipShape(Circle())
            } else {
                initialsAvatar
            }
            
            VStack(alignment: .leading, spacing: 2) {
                Text(user.displayName)
                    .font(.headline)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                Text("@\(user.username)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                if let bio = user.bio, !bio.isEmpty {
                    Text(bio)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundColor(.gray.opacity(0.4))
                .font(.caption)
        }
    }
    
    private var initialsAvatar: some View {
        Circle()
            .fill(
                LinearGradient(
                    colors: [AppColors.primary.opacity(0.7), AppColors.secondaryColor.opacity(0.7)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: 50, height: 50)
            .overlay(
                Text(user.displayName.prefix(1).uppercased())
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            )
    }
}
