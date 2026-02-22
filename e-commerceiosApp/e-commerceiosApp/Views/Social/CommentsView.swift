import SwiftUI
import Shared

struct CommentsView: View {
    let postId: String
    @StateObject private var viewModel = CommentsViewModel()
    @State private var newComment = ""
    @State private var replyingTo: CommentsViewModel.CommentData? = nil
    @State private var showSortMenu = false
    @FocusState private var isInputFocused: Bool
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            headerView
            
            Divider()
            
            // Content
            if viewModel.isLoading && viewModel.comments.isEmpty {
                loadingView
            } else if viewModel.comments.isEmpty {
                emptyView
            } else {
                commentsList
            }
            
            Divider()
            
            // Reply indicator
            if let replyTo = replyingTo {
                replyIndicator(replyTo)
            }
            
            // Input area
            inputArea
        }
        .onAppear {
            viewModel.loadComments(postId: postId)
        }
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
            Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
        }
    }
    
    // MARK: - Header
    private var headerView: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("Comments")
                    .font(Typography.h3)
                Text("\(viewModel.commentCount) comment\(viewModel.commentCount == 1 ? "" : "s")")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Sort button
            Menu {
                Button(action: { viewModel.setSortOrder(.newest) }) {
                    Label("Newest First", systemImage: viewModel.sortOrder == .newest ? "checkmark" : "")
                }
                Button(action: { viewModel.setSortOrder(.oldest) }) {
                    Label("Oldest First", systemImage: viewModel.sortOrder == .oldest ? "checkmark" : "")
                }
                Button(action: { viewModel.setSortOrder(.mostLiked) }) {
                    Label("Most Liked", systemImage: viewModel.sortOrder == .mostLiked ? "checkmark" : "")
                }
            } label: {
                Image(systemName: "arrow.up.arrow.down.circle")
                    .foregroundColor(.secondary)
                    .font(.title3)
            }
            
            Button(action: { dismiss() }) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.secondary)
                    .font(.title2)
            }
        }
        .padding()
    }
    
    // MARK: - Comments List
    private var commentsList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    ForEach(viewModel.sortedComments, id: \.id) { comment in
                        CommentRow(
                            comment: comment,
                            onLike: { viewModel.likeComment(commentId: comment.id) },
                            onDelete: { viewModel.deleteComment(commentId: comment.id, postId: postId) },
                            onReply: { replyTo(comment) }
                        )
                        .id(comment.id)
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                        
                        if comment.id != viewModel.sortedComments.last?.id {
                            Divider().padding(.leading, 60)
                        }
                    }
                    
                    // Spacer for new comments to scroll into
                    Color.clear
                        .frame(height: 1)
                        .id("bottom")
                }
                .padding(.top, 8)
            }
            .refreshable {
                viewModel.loadComments(postId: postId)
            }
            .onChange(of: viewModel.comments.count) { _ in
                withAnimation {
                    proxy.scrollTo("bottom", anchor: .bottom)
                }
            }
        }
    }
    
    // MARK: - Reply Indicator
    private func replyIndicator(_ comment: CommentsViewModel.CommentData) -> some View {
        HStack(spacing: 8) {
            Rectangle()
                .fill(AppColors.primary)
                .frame(width: 3)
            
            Image(systemName: "arrowshape.turn.up.left.fill")
                .font(.caption)
                .foregroundColor(AppColors.primary)
            
            Text("Replying to ")
                .font(.caption)
                .foregroundColor(.secondary)
            + Text("@\(comment.username)")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.primary)
            
            Spacer()
            
            Button(action: { cancelReply() }) {
                Image(systemName: "xmark")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 6)
        .background(AppColors.primary.opacity(0.05))
    }
    
    // MARK: - Input Area
    private var inputArea: some View {
        HStack(spacing: 12) {
            // Avatar
            Circle()
                .fill(AppColors.primary.opacity(0.2))
                .frame(width: 32, height: 32)
                .overlay(
                    Text(String(SessionManager.shared.currentUser?.displayName.prefix(1) ?? "U").uppercased())
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                )
            
            TextField(replyingTo.map { "Reply to @\($0.username)..." } ?? "Add a comment...", text: $newComment)
                .padding(10)
                .background(Color.gray.opacity(0.1))
                .cornerRadius(20)
                .focused($isInputFocused)
            
            Button(action: sendComment) {
                if viewModel.isSending {
                    ProgressView()
                        .scaleEffect(0.8)
                } else {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(canSend ? AppColors.primary : .secondary)
                        .rotationEffect(.degrees(45))
                }
            }
            .disabled(!canSend || viewModel.isSending)
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
    }
    
    // MARK: - Loading View
    private var loadingView: some View {
        VStack {
            Spacer()
            VStack(spacing: 12) {
                ProgressView()
                Text("Loading comments...")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
    }
    
    // MARK: - Empty View
    private var emptyView: some View {
        VStack {
            Spacer()
            VStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(AppColors.primary.opacity(0.1))
                        .frame(width: 80, height: 80)
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 32))
                        .foregroundColor(AppColors.primary.opacity(0.5))
                }
                Text("No comments yet")
                    .font(Typography.body1)
                    .fontWeight(.medium)
                Text("Be the first to share your thoughts!")
                    .font(Typography.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
        }
    }
    
    // MARK: - Helpers
    private var canSend: Bool {
        !newComment.trimmingCharacters(in: .whitespaces).isEmpty
    }
    
    private func sendComment() {
        guard canSend else { return }
        let content: String
        if let replyTo = replyingTo {
            content = "@\(replyTo.username) \(newComment)"
        } else {
            content = newComment
        }
        viewModel.addComment(postId: postId, content: content)
        newComment = ""
        replyingTo = nil
    }
    
    private func replyTo(_ comment: CommentsViewModel.CommentData) {
        replyingTo = comment
        isInputFocused = true
    }
    
    private func cancelReply() {
        replyingTo = nil
    }
}

// MARK: - Comment Row
struct CommentRow: View {
    let comment: CommentsViewModel.CommentData
    let onLike: () -> Void
    let onDelete: () -> Void
    let onReply: () -> Void
    
    @State private var showDeleteConfirmation = false
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Avatar
            Circle()
                .fill(
                    LinearGradient(
                        colors: [AppColors.primary.opacity(0.3), AppColors.secondaryColor.opacity(0.3)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 36, height: 36)
                .overlay(
                    Text(String(comment.username.prefix(1)).uppercased())
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.primary)
                )
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(comment.username)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    
                    if comment.isOwn {
                        Text("You")
                            .font(.caption2)
                            .fontWeight(.medium)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(AppColors.primary.opacity(0.15))
                            .foregroundColor(AppColors.primary)
                            .cornerRadius(4)
                    }
                    
                    Spacer()
                    
                    Text(comment.timeAgo)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                // Content with @mention highlighting
                commentContentView(comment.content)
                
                // Actions
                HStack(spacing: 20) {
                    // Like
                    Button(action: onLike) {
                        HStack(spacing: 4) {
                            Image(systemName: comment.isLiked ? "heart.fill" : "heart")
                                .font(.caption)
                                .foregroundColor(comment.isLiked ? .red : .secondary)
                            if comment.likesCount > 0 {
                                Text("\(comment.likesCount)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    
                    // Reply
                    Button(action: onReply) {
                        HStack(spacing: 4) {
                            Image(systemName: "arrowshape.turn.up.left")
                                .font(.caption)
                            Text("Reply")
                                .font(.caption)
                        }
                        .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    // Delete (own comments only)
                    if comment.isOwn {
                        Button(action: { showDeleteConfirmation = true }) {
                            Image(systemName: "trash")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(.top, 4)
            }
        }
        .confirmationDialog("Delete Comment", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive, action: onDelete)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to delete this comment?")
        }
    }
    
    @ViewBuilder
    private func commentContentView(_ content: String) -> some View {
        let words = content.split(separator: " ", omittingEmptySubsequences: false)
        let attributedText = words.reduce(Text("")) { result, word in
            let wordStr = String(word)
            if wordStr.hasPrefix("@") {
                return result + Text(wordStr).fontWeight(.semibold).foregroundColor(AppColors.primary) + Text(" ")
            } else {
                return result + Text(wordStr) + Text(" ")
            }
        }
        attributedText
            .font(Typography.body2)
    }
}
    }
}
