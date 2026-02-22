import Foundation
import SwiftUI
import PhotosUI
import AVFoundation
import Shared

@MainActor
class CreatePostViewModel: ObservableObject {
    // MARK: - Properties
    @Published var selectedVideoItem: PhotosPickerItem?
    @Published var videoURL: URL?
    @Published var thumbnailImage: UIImage?
    
    @Published var caption: String = ""
    @Published var postType: String = "reel" // reel, product, photo
    @Published var tags: String = ""
    
    @Published var isLoading: Bool = false
    @Published var uploadProgress: Double = 0.0
    @Published var errorMessage: String?
    @Published var showError: Bool = false
    @Published var postCreated: Bool = false
    
    // Cloudinary Config
    private let cloudinaryCloudName = "dt7lzxomq"
    private let cloudinaryUploadPreset = "android-buyv"
    
    // Use Case
    private let createPostUseCase: CreatePostUseCase
    
    // MARK: - Initialization
    init() {
        self.createPostUseCase = DependencyWrapper.shared.createPostUseCase
    }
    
    // MARK: - Video Loading
    func loadVideo() async {
        guard let item = selectedVideoItem else { return }
        
        do {
            guard let movie = try await item.loadTransferable(type: VideoPickerTransferable.self) else {
                setError("Failed to load video")
                return
            }
            
            self.videoURL = movie.url
            await generateThumbnail(from: movie.url)
            
        } catch {
            setError("Error loading video: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Thumbnail Generation
    private func generateThumbnail(from videoURL: URL) async {
        let asset = AVAsset(url: videoURL)
        let imageGenerator = AVAssetImageGenerator(asset: asset)
        imageGenerator.appliesPreferredTrackTransform = true
        
        let time = CMTime(seconds: 1, preferredTimescale: 60)
        
        do {
            let cgImage = try imageGenerator.copyCGImage(at: time, actualTime: nil)
            self.thumbnailImage = UIImage(cgImage: cgImage)
        } catch {
            AppLogger.error("Error generating thumbnail: \(error)")
        }
    }
    
    // MARK: - Upload to Cloudinary
    func uploadVideoToCloudinary() async throws -> String {
        guard let videoURL = videoURL else {
            throw NSError(domain: "CreatePost", code: -1, userInfo: [NSLocalizedDescriptionKey: "No video selected"])
        }
        
        let uploadURL = "https://api.cloudinary.com/v1_1/\(cloudinaryCloudName)/video/upload"
        let timestamp = String(Int(Date().timeIntervalSince1970))
        let publicId = "reels/\(timestamp)"
        
        var request = URLRequest(url: URL(string: uploadURL)!)
        request.httpMethod = "POST"
        
        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        
        var body = Data()
        
        // Add form fields
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
        body.append("\(cloudinaryUploadPreset)\r\n")
        
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"public_id\"\r\n\r\n")
        body.append("\(publicId)\r\n")
        
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"folder\"\r\n\r\n")
        body.append("reels\r\n")
        
        // Add video file
        let videoData = try Data(contentsOf: videoURL)
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"video.mp4\"\r\n")
        body.append("Content-Type: video/mp4\r\n\r\n")
        body.append(videoData)
        body.append("\r\n")
        
        body.append("--\(boundary)--\r\n")
        
        request.httpBody = body
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw NSError(domain: "Cloudinary", code: -1, userInfo: [NSLocalizedDescriptionKey: "Upload failed"])
        }
        
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let secureUrl = json?["secure_url"] as? String else {
            throw NSError(domain: "Cloudinary", code: -1, userInfo: [NSLocalizedDescriptionKey: "No URL in response"])
        }
        
        return secureUrl
    }
    
    // MARK: - Create Post
    func createPost() async {
        guard !isLoading else { return }
        
        isLoading = true
        uploadProgress = 0.0
        errorMessage = nil
        
        do {
            // Step 1: Upload video to Cloudinary
            uploadProgress = 0.3
            let cloudinaryURL = try await uploadVideoToCloudinary()
            
            // Step 2: Create caption with tags
            uploadProgress = 0.6
            let finalCaption = buildCaption()
            
            // Step 3: Create post via backend
            uploadProgress = 0.8
            
            await withCheckedContinuation { continuation in
                createPostUseCase.invoke(
                    type: postType,
                    mediaUrl: cloudinaryURL,
                    caption: finalCaption.isEmpty ? nil : finalCaption
                ) { result, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            self.setError(error.localizedDescription)
                            continuation.resume()
                            return
                        }
                        
                        if result is ResultSuccess<Post> {
                            self.uploadProgress = 1.0
                            self.postCreated = true
                            self.resetForm()
                        } else if let errorResult = result as? ResultError {
                            self.setError(errorResult.error.message)
                        }
                        continuation.resume()
                    }
                }
            }
            
        } catch {
            setError("Upload failed: \(error.localizedDescription)")
        }
        
        isLoading = false
    }
    
    // MARK: - Helpers
    private func buildCaption() -> String {
        var result = caption
        
        if !tags.isEmpty {
            let hashtags = tags.split(separator: ",").map { "#\($0.trimmingCharacters(in: .whitespaces))" }.joined(separator: " ")
            if !result.isEmpty {
                result += " "
            }
            result += hashtags
        }
        
        return result
    }
    
    private func setError(_ message: String) {
        self.errorMessage = message
        self.showError = true
    }
    
    func resetForm() {
        selectedVideoItem = nil
        videoURL = nil
        thumbnailImage = nil
        caption = ""
        tags = ""
        uploadProgress = 0.0
    }
    
    // MARK: - Validation
    var canSubmit: Bool {
        videoURL != nil && !isLoading
    }
}

// MARK: - Video Picker Transferable
struct VideoPickerTransferable: Transferable {
    let url: URL
    
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .movie) { video in
            SentTransferredFile(video.url)
        } importing: { received in
            let originalFile = received.file
            let copiedFile = URL.documentsDirectory.appendingPathComponent(UUID().uuidString).appendingPathExtension("mp4")
            try FileManager.default.copyItem(at: originalFile, to: copiedFile)
            return Self(url: copiedFile)
        }
    }
}

// MARK: - Data Extension
extension Data {
    mutating func append(_ string: String) {
        if let data = string.data(using: .utf8) {
            append(data)
        }
    }
}
