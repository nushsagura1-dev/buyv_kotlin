import SwiftUI
import Shared

struct SearchReelsView: View {
    @StateObject private var viewModel = SearchReelsViewModel()
    @State private var isSearchFocused = false
    @State private var showSearchHistory = true
    @Environment(\.dismiss) private var dismiss
    
    @AppStorage("reelSearchHistory") private var searchHistoryData: String = ""
    
    private var searchHistory: [String] {
        searchHistoryData.isEmpty ? [] : searchHistoryData.components(separatedBy: "|||").filter { !$0.isEmpty }
    }
    
    private let columns = [
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2)
    ]
    
    // Trending hashtags
    private let trendingHashtags = [
        ("#fashion", "12.5K"),
        ("#unboxing", "8.3K"),
        ("#deals", "6.1K"),
        ("#review", "5.7K"),
        ("#haul", "4.2K"),
        ("#ootd", "3.8K"),
        ("#tech", "3.1K"),
        ("#beauty", "2.9K")
    ]
    
    var body: some View {
        VStack(spacing: 0) {
            // Search Header
            searchHeader
            
            // Category Chips
            categoryChips
            
            // Content
            ScrollView {
                if !viewModel.searchQuery.isEmpty && showSearchHistory && !searchHistory.isEmpty {
                    searchHistoryContent
                } else if viewModel.hasSearched {
                    searchResultsContent
                } else {
                    discoverContent
                }
            }
            .refreshable {
                viewModel.loadReels()
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .background(Color(.systemGroupedBackground))
    }
    
    // MARK: - Search Header
    private var searchHeader: some View {
        HStack(spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.gray)
                
                TextField("Search reels, hashtags, users...", text: $viewModel.searchQuery)
                    .textFieldStyle(.plain)
                    .onSubmit {
                        performSearch()
                    }
                    .onChange(of: viewModel.searchQuery) { newValue in
                        showSearchHistory = !newValue.isEmpty && !viewModel.hasSearched
                    }
                
                if !viewModel.searchQuery.isEmpty {
                    Button(action: {
                        viewModel.clearSearch()
                        showSearchHistory = false
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(12)
            
            if !viewModel.searchQuery.isEmpty {
                Button("Search") {
                    performSearch()
                }
                .foregroundColor(AppColors.primary)
                .fontWeight(.medium)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Category Chips
    private var categoryChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(viewModel.categories, id: \.self) { category in
                    Button(action: { viewModel.filterByCategory(category) }) {
                        Text(category)
                            .font(.subheadline)
                            .fontWeight(viewModel.selectedCategory == category ? .semibold : .regular)
                            .foregroundColor(viewModel.selectedCategory == category ? .white : .primary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(
                                viewModel.selectedCategory == category
                                    ? AppColors.primary
                                    : Color(.systemGray5)
                            )
                            .cornerRadius(20)
                    }
                }
            }
            .padding(.horizontal)
        }
        .padding(.bottom, 8)
    }
    
    // MARK: - Search History
    private var searchHistoryContent: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Recent Searches")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                Spacer()
                Button("Clear All") {
                    searchHistoryData = ""
                }
                .font(.caption)
                .foregroundColor(.red)
            }
            .padding(.horizontal)
            .padding(.top, 8)
            
            let filtered = searchHistory.filter { $0.localizedCaseInsensitiveContains(viewModel.searchQuery) }
            
            ForEach(filtered, id: \.self) { term in
                Button(action: {
                    viewModel.searchQuery = term
                    performSearch()
                }) {
                    HStack(spacing: 10) {
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text(term)
                            .font(.subheadline)
                            .foregroundColor(.primary)
                        Spacer()
                        Button(action: {
                            removeFromHistory(term)
                        }) {
                            Image(systemName: "xmark")
                                .font(.caption2)
                                .foregroundColor(.gray)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                }
            }
        }
    }
    
    // MARK: - Search Results
    private var searchResultsContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            if viewModel.searchResults.isEmpty {
                // No results
                VStack(spacing: 16) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    Text("No reels found")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Text("Try a different search term or category")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                    
                    // Suggestions
                    VStack(spacing: 8) {
                        Text("Try searching for:")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        HStack(spacing: 8) {
                            ForEach(["Fashion", "Tech", "Beauty"], id: \.self) { term in
                                Button(action: {
                                    viewModel.searchQuery = term
                                    performSearch()
                                }) {
                                    Text(term)
                                        .font(.caption)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(AppColors.primary.opacity(0.1))
                                        .foregroundColor(AppColors.primary)
                                        .cornerRadius(16)
                                }
                            }
                        }
                    }
                    .padding(.top, 8)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 60)
            } else {
                HStack {
                    Text("\(viewModel.searchResults.count) results")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                    
                    // View mode toggle
                    HStack(spacing: 4) {
                        Image(systemName: "square.grid.3x3")
                            .font(.caption)
                            .foregroundColor(AppColors.primary)
                    }
                }
                .padding(.horizontal)
                
                reelsGrid(reels: viewModel.searchResults)
            }
        }
    }
    
    // MARK: - Discover Content (was Trending)
    private var discoverContent: some View {
        VStack(alignment: .leading, spacing: 20) {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 60)
            } else {
                // Trending Hashtags
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: "number")
                            .foregroundColor(AppColors.primary)
                        Text("Trending Hashtags")
                            .font(.headline)
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)
                    
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(trendingHashtags, id: \.0) { tag, count in
                                Button(action: {
                                    viewModel.searchQuery = tag
                                    performSearch()
                                }) {
                                    VStack(spacing: 4) {
                                        Text(tag)
                                            .font(.subheadline)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.primary)
                                        Text("\(count) videos")
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 10)
                                    .background(Color(.systemGray6))
                                    .cornerRadius(12)
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                
                // Trending Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: "flame.fill")
                            .foregroundColor(.orange)
                        Text("Trending Reels")
                            .font(.headline)
                        Spacer()
                        if !viewModel.trendingReels.isEmpty {
                            Text("\(viewModel.trendingReels.count)")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color(.systemGray5))
                                .cornerRadius(8)
                        }
                    }
                    .padding(.horizontal)
                    
                    reelsGrid(reels: viewModel.trendingReels)
                }
                
                // Recent searches at bottom if not searching
                if !searchHistory.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: "clock")
                                .foregroundColor(.gray)
                            Text("Recent Searches")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                            Spacer()
                        }
                        .padding(.horizontal)
                        
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 6) {
                                ForEach(searchHistory.prefix(5), id: \.self) { term in
                                    Button(action: {
                                        viewModel.searchQuery = term
                                        performSearch()
                                    }) {
                                        Text(term)
                                            .font(.caption)
                                            .foregroundColor(.primary)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color(.systemGray5))
                                            .cornerRadius(16)
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                    .padding(.bottom, 16)
                }
            }
        }
    }
    
    // MARK: - Reels Grid
    private func reelsGrid(reels: [Product]) -> some View {
        LazyVGrid(columns: columns, spacing: 2) {
            ForEach(reels, id: \.id) { reel in
                NavigationLink(destination: ProductDetailView(productId: String(reel.id))) {
                    ReelThumbnailCard(reel: reel)
                }
            }
        }
        .padding(.horizontal, 2)
    }
    
    // MARK: - Helpers
    private func performSearch() {
        showSearchHistory = false
        addToHistory(viewModel.searchQuery)
        viewModel.search()
    }
    
    private func addToHistory(_ term: String) {
        guard !term.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        var history = searchHistory
        history.removeAll { $0.lowercased() == term.lowercased() }
        history.insert(term, at: 0)
        if history.count > 10 { history = Array(history.prefix(10)) }
        searchHistoryData = history.joined(separator: "|||")
    }
    
    private func removeFromHistory(_ term: String) {
        var history = searchHistory
        history.removeAll { $0 == term }
        searchHistoryData = history.joined(separator: "|||")
    }
}

// MARK: - Reel Thumbnail Card
struct ReelThumbnailCard: View {
    let reel: Product
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Thumbnail (product image as placeholder)
            AsyncImage(url: URL(string: reel.image)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure(_):
                    ZStack {
                        Color.gray.opacity(0.2)
                        Image(systemName: "play.rectangle.fill")
                            .font(.title)
                            .foregroundColor(.gray)
                    }
                default:
                    Color.gray.opacity(0.1)
                        .overlay(ProgressView())
                }
            }
            .frame(height: 180)
            .clipped()
            
            // Overlay gradient
            LinearGradient(
                colors: [.clear, .black.opacity(0.6)],
                startPoint: .center,
                endPoint: .bottom
            )
            
            // Info overlay
            VStack(alignment: .leading, spacing: 2) {
                Text(reel.name)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .lineLimit(1)
                
                HStack(spacing: 4) {
                    Image(systemName: "play.fill")
                        .font(.system(size: 8))
                    Text("$\(String(format: "%.0f", Double(reel.price) ?? 0.0))")
                        .font(.caption2)
                        .fontWeight(.medium)
                }
                .foregroundColor(.white.opacity(0.8))
            }
            .padding(6)
            
            // Play icon
            Image(systemName: "play.circle.fill")
                .font(.system(size: 28))
                .foregroundColor(.white.opacity(0.8))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .cornerRadius(4)
    }
}

#Preview {
    NavigationView {
        SearchReelsView()
    }
}
