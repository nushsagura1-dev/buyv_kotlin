import SwiftUI
import PhotosUI
import Shared

struct EditProfileView: View {
    @StateObject private var viewModel = SocialViewModel()
    @Environment(\.presentationMode) var presentationMode
    
    // Initial values
    let currentUser: UserProfile
    
    @State private var displayName: String
    @State private var username: String
    @State private var bio: String
    @State private var phone: String
    @State private var photoUrl: String
    
    // Photo picker state
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var isUploading = false
    @State private var uploadError: String?
    @State private var showSaved = false
    
    private let maxBioLength = 500
    
    // Cloudinary Config
    private let cloudinaryCloudName = "dt7lzxomq"
    private let cloudinaryUploadPreset = "android-buyv"
    
    /// Convenience init — reads current user from SessionManager
    init() {
        let user = SessionManager.shared.currentUser ?? UserProfile(
            uid: "", email: "", displayName: "User", username: "",
            profileImageUrl: nil, bio: "", phone: "", role: "user",
            followersCount: 0, followingCount: 0, likesCount: 0,
            createdAt: "", lastUpdated: ""
        )
        self.init(user: user)
    }
    
    init(user: UserProfile) {
        self.currentUser = user
        _displayName = State(initialValue: user.displayName)
        _username = State(initialValue: user.username)
        _bio = State(initialValue: user.bio)
        _phone = State(initialValue: user.phone)
        _photoUrl = State(initialValue: user.profileImageUrl ?? "")
    }
    
    private var hasChanges: Bool {
        displayName != currentUser.displayName ||
        username != currentUser.username ||
        bio != currentUser.bio ||
        phone != currentUser.phone ||
        photoUrl != (currentUser.profileImageUrl ?? "")
    }
    
    var body: some View {
        NavigationView {
            Form {
                // Profile Photo Section
                Section {
                    VStack(spacing: 16) {
                        // Preview
                        ZStack {
                            if let image = selectedImage {
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 110, height: 110)
                                    .clipShape(Circle())
                            } else if !photoUrl.isEmpty, let url = URL(string: photoUrl) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    ProgressView()
                                }
                                .frame(width: 110, height: 110)
                                .clipShape(Circle())
                            } else {
                                Circle()
                                    .fill(LinearGradient(colors: [.blue, .purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                                    .frame(width: 110, height: 110)
                                    .overlay(
                                        Text(currentUser.displayName.prefix(1).uppercased())
                                            .font(.system(size: 40, weight: .bold))
                                            .foregroundColor(.white)
                                    )
                            }
                            
                            // Camera overlay
                            Circle()
                                .fill(Color.black.opacity(0.3))
                                .frame(width: 110, height: 110)
                                .overlay(
                                    Image(systemName: "camera.fill")
                                        .foregroundColor(.white)
                                        .font(.title3)
                                )
                                .opacity(isUploading ? 0 : 0.001) // keep hit area
                            
                            if isUploading {
                                Circle()
                                    .fill(Color.black.opacity(0.5))
                                    .frame(width: 110, height: 110)
                                    .overlay(ProgressView().tint(.white))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        
                        // Photo picker button
                        PhotosPicker(
                            selection: $selectedPhotoItem,
                            matching: .images,
                            photoLibrary: .shared()
                        ) {
                            Label("Change Photo", systemImage: "photo.on.rectangle.angled")
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(AppColors.primary)
                                .cornerRadius(20)
                        }
                        .onChange(of: selectedPhotoItem) { newItem in
                            Task {
                                await loadAndUploadPhoto(item: newItem)
                            }
                        }
                        
                        if let error = uploadError {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                    .padding(.vertical, 8)
                }
                
                Section(header: Text("Personal Info")) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Display Name")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("Your display name", text: $displayName)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Username")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("Username", text: $username)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Phone")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextField("Phone number (optional)", text: $phone)
                            .keyboardType(.phonePad)
                    }
                }
                
                Section(header: Text("About")) {
                    VStack(alignment: .leading, spacing: 4) {
                        TextEditor(text: $bio)
                            .frame(minHeight: 100)
                            .onChange(of: bio) { newValue in
                                if newValue.count > maxBioLength {
                                    bio = String(newValue.prefix(maxBioLength))
                                }
                            }
                        
                        HStack {
                            Spacer()
                            Text("\(bio.count)/\(maxBioLength)")
                                .font(.caption2)
                                .foregroundColor(bio.count > maxBioLength - 50 ? .orange : .secondary)
                        }
                    }
                }
                
                Section(header: Text("Account")) {
                    HStack {
                        Text("Email")
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(currentUser.email)
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        Text("Role")
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(currentUser.role.capitalized)
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        Text("Member since")
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(formatDate(currentUser.createdAt))
                            .foregroundColor(.secondary)
                    }
                }
                
                if let error = viewModel.errorMessage {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
                
                Section {
                    Button(action: saveChanges) {
                        HStack {
                            Spacer()
                            if viewModel.isLoading {
                                ProgressView()
                            } else {
                                Text("Save Changes")
                                    .fontWeight(.bold)
                                    .foregroundColor(hasChanges ? .white : .gray)
                            }
                            Spacer()
                        }
                        .padding(.vertical, 4)
                        .background(hasChanges ? AppColors.primary : Color.gray.opacity(0.2))
                        .cornerRadius(8)
                    }
                    .disabled(viewModel.isLoading || isUploading || !hasChanges)
                }
            }
            .navigationTitle("Edit Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
            .overlay {
                if showSaved {
                    VStack {
                        Spacer()
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("Profile updated!")
                                .fontWeight(.medium)
                        }
                        .padding()
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                        .padding(.bottom, 40)
                    }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
        }
    }
    
    // MARK: - Photo Upload
    
    func loadAndUploadPhoto(item: PhotosPickerItem?) async {
        guard let item = item else { return }
        
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                uploadError = "Failed to load image data"
                return
            }
            
            guard let image = UIImage(data: data) else {
                uploadError = "Invalid image format"
                return
            }
            
            selectedImage = image
            isUploading = true
            uploadError = nil
            
            // Compress and upload to Cloudinary
            guard let imageData = image.jpegData(compressionQuality: 0.8) else {
                uploadError = "Failed to compress image"
                isUploading = false
                return
            }
            
            let uploadURL = "https://api.cloudinary.com/v1_1/\(cloudinaryCloudName)/image/upload"
            let timestamp = String(Int(Date().timeIntervalSince1970))
            let publicId = "profiles/\(timestamp)"
            
            var request = URLRequest(url: URL(string: uploadURL)!)
            request.httpMethod = "POST"
            
            let boundary = UUID().uuidString
            request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
            
            var body = Data()
            
            // Upload preset
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(cloudinaryUploadPreset)\r\n".data(using: .utf8)!)
            
            // Public ID
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"public_id\"\r\n\r\n".data(using: .utf8)!)
            body.append("\(publicId)\r\n".data(using: .utf8)!)
            
            // Folder
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"folder\"\r\n\r\n".data(using: .utf8)!)
            body.append("profiles\r\n".data(using: .utf8)!)
            
            // Image file
            body.append("--\(boundary)\r\n".data(using: .utf8)!)
            body.append("Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n".data(using: .utf8)!)
            body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
            body.append(imageData)
            body.append("\r\n".data(using: .utf8)!)
            body.append("--\(boundary)--\r\n".data(using: .utf8)!)
            
            request.httpBody = body
            
            let (responseData, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                uploadError = "Upload failed"
                isUploading = false
                return
            }
            
            struct CloudinaryResponse: Codable {
                let secure_url: String
            }
            
            let cloudinaryResponse = try JSONDecoder().decode(CloudinaryResponse.self, from: responseData)
            photoUrl = cloudinaryResponse.secure_url
            isUploading = false
            
        } catch {
            uploadError = error.localizedDescription
            isUploading = false
        }
    }
    
    func saveChanges() {
        viewModel.updateUserProfile(
            username: username,
            bio: bio,
            photoUrl: photoUrl.isEmpty ? nil : photoUrl
        ) { success in
            if success {
                SessionManager.shared.checkSession()
                withAnimation { showSaved = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    withAnimation { showSaved = false }
                    presentationMode.wrappedValue.dismiss()
                }
            }
        }
    }
    
    private func formatDate(_ dateStr: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatter.date(from: dateStr) {
            let df = DateFormatter()
            df.dateStyle = .medium
            return df.string(from: date)
        }
        // Try without fractional seconds
        isoFormatter.formatOptions = [.withInternetDateTime]
        if let date = isoFormatter.date(from: dateStr) {
            let df = DateFormatter()
            df.dateStyle = .medium
            return df.string(from: date)
        }
        return dateStr
    }
}
