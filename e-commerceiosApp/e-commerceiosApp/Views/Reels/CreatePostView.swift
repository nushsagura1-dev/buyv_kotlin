import SwiftUI
import PhotosUI
import AVKit

struct CreatePostView: View {
    @StateObject private var viewModel = CreatePostViewModel()
    @Environment(\.dismiss) var dismiss
    
    // Product linking (optional — when posting a product reel)
    @State private var linkProduct = false
    @State private var productName = ""
    @State private var productPrice = ""
    @State private var productCategory = "Fashion"
    @State private var selectedSize: String?
    @State private var selectedColor: String?
    
    private let categories = ["Fashion", "Electronics", "Beauty", "Home", "Sports", "Food", "Accessories", "Other"]
    private let sizes = ["XS", "S", "M", "L", "XL", "XXL"]
    private let colors = ["Black", "White", "Red", "Blue", "Green", "Yellow", "Pink", "Purple", "Orange", "Brown", "Gray"]
    
    var body: some View {
        NavigationView {
            ZStack {
                AppColors.background.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 20) {
                        // Media Picker Section
                        mediaPickerSection
                        
                        // Video/Image Preview
                        if let videoURL = viewModel.videoURL {
                            videoPreviewSection(videoURL: videoURL)
                        }
                        
                        // Caption Input
                        captionSection
                        
                        // Tags Input
                        tagsSection
                        
                        // Post Type Selector
                        postTypeSection
                        
                        // Product Linking (for product posts)
                        if viewModel.postType == "product" || linkProduct {
                            productLinkingSection
                        }
                        
                        // Size & Color (for product posts)
                        if viewModel.postType == "product" || linkProduct {
                            sizeColorSection
                        }
                        
                        // Visibility toggle
                        visibilitySection
                        
                        // Upload Button
                        uploadButton
                        
                        // Tips
                        tipsSection
                    }
                    .padding()
                }
                
                // Loading Overlay
                if viewModel.isLoading {
                    loadingOverlay
                }
            }
            .navigationTitle("Create Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    if viewModel.videoURL != nil {
                        Button("Reset") {
                            viewModel.resetForm()
                            linkProduct = false
                            productName = ""
                            productPrice = ""
                            selectedSize = nil
                            selectedColor = nil
                        }
                        .foregroundColor(.red)
                    }
                }
            }
            .alert("Error", isPresented: $viewModel.showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(viewModel.errorMessage ?? "Unknown error")
            }
            .onChange(of: viewModel.selectedVideoItem) { _, _ in
                Task {
                    await viewModel.loadVideo()
                }
            }
            .onChange(of: viewModel.postCreated) { _, created in
                if created {
                    dismiss()
                }
            }
        }
    }
    
    // MARK: - Media Picker Section
    private var mediaPickerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Select Media")
                .font(.headline)
            
            HStack(spacing: 12) {
                // Video picker
                PhotosPicker(selection: $viewModel.selectedVideoItem,
                            matching: .videos) {
                    VStack(spacing: 8) {
                        Image(systemName: "video.fill")
                            .font(.title2)
                            .foregroundColor(AppColors.primary)
                        Text("Video")
                            .font(.caption)
                            .foregroundColor(.primary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(viewModel.videoURL != nil ? AppColors.primary : Color.clear, lineWidth: 2)
                    )
                }
                
                // Camera (placeholder — needs real camera integration)
                Button(action: {
                    viewModel.errorMessage = "Camera capture coming soon"
                    viewModel.showError = true
                }) {
                    VStack(spacing: 8) {
                        Image(systemName: "camera.fill")
                            .font(.title2)
                            .foregroundColor(.secondary)
                        Text("Camera")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
            }
            
            if viewModel.videoURL != nil {
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text("Video selected")
                        .font(.caption)
                        .foregroundColor(.green)
                }
            }
        }
    }
    
    // MARK: - Video Preview Section
    private func videoPreviewSection(videoURL: URL) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Preview")
                    .font(.headline)
                Spacer()
                Button(action: {
                    viewModel.selectedVideoItem = nil
                    viewModel.videoURL = nil
                    viewModel.thumbnailImage = nil
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
            
            if let thumbnail = viewModel.thumbnailImage {
                ZStack {
                    Image(uiImage: thumbnail)
                        .resizable()
                        .aspectRatio(9/16, contentMode: .fit)
                        .frame(maxHeight: 300)
                        .clipped()
                        .cornerRadius(12)
                    
                    // Play icon overlay
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.white.opacity(0.9))
                        .shadow(radius: 5)
                }
                .frame(maxWidth: .infinity)
            } else {
                VideoPlayer(player: AVPlayer(url: videoURL))
                    .frame(height: 250)
                    .cornerRadius(12)
            }
        }
    }
    
    // MARK: - Caption Section
    private var captionSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Caption")
                    .font(.headline)
                Spacer()
                Text("\(viewModel.caption.count)/500")
                    .font(.caption)
                    .foregroundColor(viewModel.caption.count > 500 ? .red : .secondary)
            }
            
            TextField("Write a caption...", text: $viewModel.caption, axis: .vertical)
                .lineLimit(3...6)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
        }
    }
    
    // MARK: - Tags Section
    private var tagsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Tags")
                .font(.headline)
            
            TextField("Enter tags (comma separated)", text: $viewModel.tags)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            
            // Quick tag suggestions
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(["trending", "fashion", "beauty", "tech", "food", "lifestyle"], id: \.self) { tag in
                        Button(action: {
                            if viewModel.tags.isEmpty {
                                viewModel.tags = tag
                            } else if !viewModel.tags.contains(tag) {
                                viewModel.tags += ", \(tag)"
                            }
                        }) {
                            Text("#\(tag)")
                                .font(.caption)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(AppColors.primary.opacity(0.1))
                                .foregroundColor(AppColors.primary)
                                .cornerRadius(12)
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Post Type Section
    private var postTypeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Post Type")
                .font(.headline)
            
            Picker("Type", selection: $viewModel.postType) {
                Text("Reel").tag("reel")
                Text("Product").tag("product")
                Text("Photo").tag("photo")
            }
            .pickerStyle(.segmented)
            
            Text(postTypeDescription)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
    
    private var postTypeDescription: String {
        switch viewModel.postType {
        case "reel": return "Short video content to engage your audience"
        case "product": return "Showcase a product with purchase details"
        case "photo": return "Share a photo with your followers"
        default: return ""
        }
    }
    
    // MARK: - Product Linking Section
    private var productLinkingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "bag.fill")
                    .foregroundColor(AppColors.primary)
                Text("Product Details")
                    .font(.headline)
            }
            
            TextField("Product Name", text: $productName)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            
            HStack(spacing: 12) {
                HStack {
                    Text("$")
                        .foregroundColor(.secondary)
                    TextField("Price", text: $productPrice)
                        .keyboardType(.decimalPad)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                Picker("Category", selection: $productCategory) {
                    ForEach(categories, id: \.self) { cat in
                        Text(cat).tag(cat)
                    }
                }
                .pickerStyle(.menu)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
        }
    }
    
    // MARK: - Size & Color Section
    private var sizeColorSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Size
            VStack(alignment: .leading, spacing: 6) {
                Text("Size")
                    .font(.subheadline)
                    .fontWeight(.medium)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(sizes, id: \.self) { size in
                            Button(action: { selectedSize = selectedSize == size ? nil : size }) {
                                Text(size)
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(selectedSize == size ? AppColors.primary : Color(.systemGray6))
                                    .foregroundColor(selectedSize == size ? .white : .primary)
                                    .cornerRadius(8)
                            }
                        }
                    }
                }
            }
            
            // Color
            VStack(alignment: .leading, spacing: 6) {
                Text("Color")
                    .font(.subheadline)
                    .fontWeight(.medium)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(colors, id: \.self) { color in
                            Button(action: { selectedColor = selectedColor == color ? nil : color }) {
                                Text(color)
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(selectedColor == color ? AppColors.primary : Color(.systemGray6))
                                    .foregroundColor(selectedColor == color ? .white : .primary)
                                    .cornerRadius(8)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Visibility Section
    private var visibilitySection: some View {
        HStack {
            Image(systemName: "eye")
                .foregroundColor(.secondary)
            Text("Visible to everyone")
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            Image(systemName: "globe")
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
    
    // MARK: - Upload Button
    private var uploadButton: some View {
        Button(action: {
            Task {
                await viewModel.createPost()
            }
        }) {
            HStack(spacing: 10) {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.title3)
                    Text("Publish")
                        .fontWeight(.bold)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                LinearGradient(
                    colors: viewModel.canSubmit ? [AppColors.primary, AppColors.primary.opacity(0.8)] : [Color.gray, Color.gray],
                    startPoint: .leading, endPoint: .trailing
                )
            )
            .foregroundColor(.white)
            .cornerRadius(12)
            .shadow(color: viewModel.canSubmit ? AppColors.primary.opacity(0.3) : .clear, radius: 8, y: 4)
        }
        .disabled(!viewModel.canSubmit)
    }
    
    // MARK: - Tips Section
    private var tipsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Tips for great content")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
            
            VStack(alignment: .leading, spacing: 4) {
                tipRow(icon: "clock", text: "Keep reels under 60 seconds")
                tipRow(icon: "text.alignleft", text: "Write engaging captions")
                tipRow(icon: "number", text: "Use relevant hashtags")
                tipRow(icon: "camera.metering.spot", text: "Good lighting makes a difference")
            }
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.5))
        .cornerRadius(10)
    }
    
    private func tipRow(icon: String, text: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(AppColors.primary)
                .frame(width: 16)
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Loading Overlay
    private var loadingOverlay: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                ProgressView(value: viewModel.uploadProgress) {
                    Text(uploadStatusText)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                } currentValueLabel: {
                    Text("\(Int(viewModel.uploadProgress * 100))%")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
                .tint(AppColors.primary)
                .frame(width: 220)
                
                Text("Please wait while we upload your content")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
            }
            .padding(30)
            .background(.ultraThinMaterial)
            .cornerRadius(20)
        }
    }
    
    private var uploadStatusText: String {
        if viewModel.uploadProgress < 0.3 { return "Preparing..." }
        if viewModel.uploadProgress < 0.6 { return "Uploading media..." }
        if viewModel.uploadProgress < 0.8 { return "Processing..." }
        return "Almost done..."
    }
}

#Preview {
    CreatePostView()
}
