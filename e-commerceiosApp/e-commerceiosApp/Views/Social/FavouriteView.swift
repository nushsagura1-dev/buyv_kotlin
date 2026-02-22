import SwiftUI
import Shared

struct FavouriteView: View {
    @StateObject private var viewModel = FavouriteViewModel()
    @State private var showRemoveAlert = false
    @State private var postToRemove: Post?
    @State private var isSelectionMode = false
    @State private var selectedPostIds: Set<String> = []
    
    let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            if viewModel.isLoading {
                VStack(spacing: 12) {
                    ProgressView()
                    Text("Loading favourites...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            } else if let error = viewModel.errorMessage, viewModel.bookmarkedPosts.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 48))
                        .foregroundColor(.orange)
                    Text("Something went wrong")
                        .font(.headline)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                    Button(action: { viewModel.refresh() }) {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.clockwise")
                            Text("Try Again")
                        }
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 10)
                        .background(AppColors.primary)
                        .cornerRadius(10)
                    }
                }
            } else if viewModel.bookmarkedPosts.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "heart.slash")
                        .font(.system(size: 56))
                        .foregroundColor(.gray.opacity(0.4))
                    Text("No Favourites Yet")
                        .font(.title3)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                    Text("Posts you bookmark will appear here")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                    
                    Button(action: { viewModel.refresh() }) {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.clockwise")
                            Text("Refresh")
                        }
                        .font(.subheadline)
                        .foregroundColor(AppColors.primary)
                    }
                    .padding(.top, 8)
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        // Error banner (non-blocking)
                        if let error = viewModel.errorMessage {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.orange)
                                Text(error)
                                    .font(.caption)
                                Spacer()
                                Button(action: { viewModel.clearError() }) {
                                    Image(systemName: "xmark")
                                        .font(.caption)
                                }
                            }
                            .padding(10)
                            .background(Color.orange.opacity(0.1))
                            .cornerRadius(8)
                            .padding(.horizontal)
                        }
                        
                        // Count header + selection mode
                        HStack {
                            Text("\(viewModel.bookmarkCount) saved")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            Spacer()
                            
                            if isSelectionMode && !selectedPostIds.isEmpty {
                                Button(action: removeSelectedPosts) {
                                    HStack(spacing: 4) {
                                        Image(systemName: "trash")
                                            .font(.caption)
                                        Text("Remove (\(selectedPostIds.count))")
                                            .font(.caption)
                                            .fontWeight(.medium)
                                    }
                                    .foregroundColor(.red)
                                }
                            }
                            
                            Button(action: {
                                isSelectionMode.toggle()
                                if !isSelectionMode { selectedPostIds.removeAll() }
                            }) {
                                Text(isSelectionMode ? "Done" : "Select")
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .foregroundColor(AppColors.primary)
                            }
                        }
                        .padding(.horizontal)
                        .padding(.top, 4)
                        
                        // Grid
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(viewModel.bookmarkedPosts, id: \.id) { post in
                                FavouriteCard(
                                    post: post,
                                    isSelectionMode: isSelectionMode,
                                    isSelected: selectedPostIds.contains(post.id),
                                    onRemove: {
                                        postToRemove = post
                                        showRemoveAlert = true
                                    },
                                    onSelect: {
                                        if selectedPostIds.contains(post.id) {
                                            selectedPostIds.remove(post.id)
                                        } else {
                                            selectedPostIds.insert(post.id)
                                        }
                                    }
                                )
                            }
                        }
                        .padding(.horizontal)
                    }
                    .padding(.bottom, 16)
                }
                .refreshable {
                    viewModel.refresh()
                }
            }
        }
        .navigationTitle("Favourites")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { viewModel.loadBookmarks() }
        .alert("Remove from Favourites?", isPresented: $showRemoveAlert) {
            Button("Cancel", role: .cancel) { postToRemove = nil }
            Button("Remove", role: .destructive) {
                if let post = postToRemove {
                    withAnimation { viewModel.removeBookmark(postId: post.id) }
                    postToRemove = nil
                }
            }
        } message: {
            Text("This post will be removed from your saved items.")
        }
    }
    
    private func removeSelectedPosts() {
        for postId in selectedPostIds {
            viewModel.removeBookmark(postId: postId)
        }
        selectedPostIds.removeAll()
        isSelectionMode = false
    }
}

// MARK: - Favourite Card
struct FavouriteCard: View {
    let post: Post
    let isSelectionMode: Bool
    let isSelected: Bool
    let onRemove: () -> Void
    let onSelect: () -> Void
    
    init(post: Post, isSelectionMode: Bool = false, isSelected: Bool = false, onRemove: @escaping () -> Void, onSelect: @escaping () -> Void = {}) {
        self.post = post
        self.isSelectionMode = isSelectionMode
        self.isSelected = isSelected
        self.onRemove = onRemove
        self.onSelect = onSelect
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Thumbnail
            ZStack(alignment: .topTrailing) {
                if let thumbnailUrl = post.thumbnailUrl, !thumbnailUrl.isEmpty, let url = URL(string: thumbnailUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.gray.opacity(0.2)
                            .overlay(ProgressView())
                    }
                } else if let url = URL(string: post.mediaUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.gray.opacity(0.2)
                    }
                } else {
                    Color.gray.opacity(0.2)
                        .overlay(
                            Image(systemName: "photo")
                                .font(.title2)
                                .foregroundColor(.gray)
                        )
                }
                
                // Remove button
                Button(action: onRemove) {
                    Image(systemName: "bookmark.fill")
                        .font(.caption)
                        .foregroundColor(.white)
                        .padding(6)
                        .background(Color.black.opacity(0.5))
                        .clipShape(Circle())
                }
                .padding(6)
                
                // Media type badge
                VStack {
                    Spacer()
                    HStack {
                        if post.mediaType == "video" {
                            HStack(spacing: 3) {
                                Image(systemName: "play.fill")
                                    .font(.system(size: 8))
                                Text("Video")
                                    .font(.system(size: 9, weight: .medium))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(Color.black.opacity(0.6))
                            .cornerRadius(4)
                            .padding(6)
                        }
                        Spacer()
                    }
                }
            }
            .frame(height: 150)
            .clipped()
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                // Caption
                if !post.caption.isEmpty {
                    Text(post.caption)
                        .font(.caption)
                        .fontWeight(.medium)
                        .lineLimit(2)
                        .foregroundColor(.primary)
                }
                
                // Author
                HStack(spacing: 4) {
                    if let img = post.userProfileImage, !img.isEmpty, let url = URL(string: img) {
                        AsyncImage(url: url) { image in
                            image.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Color.gray.opacity(0.3)
                        }
                        .frame(width: 16, height: 16)
                        .clipShape(Circle())
                    }
                    Text("@\(post.username)")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                
                // Stats
                HStack(spacing: 8) {
                    HStack(spacing: 2) {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 8))
                            .foregroundColor(.red)
                        Text("\(post.likesCount)")
                            .font(.caption2)
                    }
                    HStack(spacing: 2) {
                        Image(systemName: "bubble.right")
                            .font(.system(size: 8))
                        Text("\(post.commentsCount)")
                            .font(.caption2)
                    }
                }
                .foregroundColor(.secondary)
            }
            .padding(8)
        }
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
        .overlay(
            Group {
                if isSelectionMode {
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(isSelected ? AppColors.primary : Color.clear, lineWidth: 2)
                    
                    VStack {
                        HStack {
                            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                                .font(.title3)
                                .foregroundColor(isSelected ? AppColors.primary : .white)
                                .shadow(radius: 2)
                                .padding(8)
                            Spacer()
                        }
                        Spacer()
                    }
                }
            }
        )
        .onTapGesture {
            if isSelectionMode {
                onSelect()
            }
        }
    }
}
