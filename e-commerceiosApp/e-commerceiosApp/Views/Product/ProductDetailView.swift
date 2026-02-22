import SwiftUI
import Shared

struct ProductDetailView: View {
    let productId: String
    @StateObject private var viewModel = ProductDetailViewModel()
    @State private var quantity = 1
    @State private var selectedImageIndex = 0
    @State private var selectedSize: String? = nil
    @State private var selectedColor: String? = nil
    @State private var showShareSheet = false
    @State private var showPromoteSheet = false
    @State private var addedToCartToast = false
    
    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if viewModel.isLoading && viewModel.product == nil {
                        ProgressView()
                            .frame(maxWidth: .infinity, minHeight: 300)
                    } else if let product = viewModel.product {
                        // MARK: - Image Carousel
                        imageCarousel(product: product)
                        
                        VStack(alignment: .leading, spacing: 16) {
                            // MARK: - Title + Price
                            titlePriceSection(product: product)
                            
                            // MARK: - Stats Row
                            statsRow(product: product)
                            
                            Divider()
                            
                            // MARK: - Commission Card (if commissionRate > 0)
                            if product.commissionRate > 0 {
                                commissionCard(product: product)
                            }
                            
                            // MARK: - Size & Color Selector
                            if let sizeColorData = product.sizeColorData as? String, !sizeColorData.isEmpty {
                                sizeColorSection(sizeColorData: sizeColorData)
                                Divider()
                            }
                            
                            // MARK: - Description
                            descriptionSection(product: product)
                            
                            Divider()
                            
                            // MARK: - Tags
                            if let tags = product.tags as? [String], !tags.isEmpty {
                                tagsSection(tags: tags)
                                Divider()
                            }
                            
                            // MARK: - Quantity Selector
                            quantitySelector(product: product)
                            
                            // Bottom spacer for action bar
                            Spacer().frame(height: 100)
                        }
                        .padding()
                    }
                }
            }
            .refreshable {
                viewModel.loadProduct(id: productId)
            }
            
            // MARK: - Bottom Action Bar
            if let product = viewModel.product {
                bottomActionBar(product: product)
            }
            
            // Toast overlay
            if addedToCartToast {
                VStack {
                    Spacer()
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.white)
                        Text("Added to Cart!")
                            .foregroundColor(.white)
                            .fontWeight(.semibold)
                        Spacer()
                        NavigationLink("View Cart") {
                            CartView()
                        }
                        .font(.subheadline.bold())
                        .foregroundColor(.yellow)
                    }
                    .padding()
                    .background(Color.black.opacity(0.85))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    .padding(.bottom, 90)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(10)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                HStack(spacing: 12) {
                    Button { showShareSheet = true } label: {
                        Image(systemName: "square.and.arrow.up")
                            .foregroundColor(AppColors.primary)
                    }
                    if viewModel.product?.commissionRate ?? 0 > 0 {
                        Button { showPromoteSheet = true } label: {
                            Image(systemName: "megaphone")
                                .foregroundColor(AppColors.primary)
                        }
                    }
                }
            }
        }
        .onAppear {
            viewModel.loadProduct(id: productId)
        }
        .alert(item: Binding<ErrorWrapper?>(
            get: { viewModel.errorMessage.map { ErrorWrapper(message: $0) } },
            set: { viewModel.errorMessage = $0?.message }
        )) { errorWrapper in
            Alert(title: Text("Error"), message: Text(errorWrapper.message), dismissButton: .default(Text("OK")))
        }
        .sheet(isPresented: $showShareSheet) {
            if let product = viewModel.product {
                ShareSheet(items: ["Check out \(product.name) on BuyV! $\(String(format: "%.2f", product.price))"])
            }
        }
        .sheet(isPresented: $showPromoteSheet) {
            if let product = viewModel.product {
                PromoteProductSheet(product: product, viewModel: viewModel)
            }
        }
    }
    
    // MARK: - Image Carousel
    @ViewBuilder
    private func imageCarousel(product: Product) -> some View {
        let images = productImages(product)
        
        ZStack(alignment: .bottomTrailing) {
            TabView(selection: $selectedImageIndex) {
                ForEach(images.indices, id: \.self) { index in
                    AsyncImage(url: URL(string: images[index])) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fit)
                        default:
                            Color.gray.opacity(0.1)
                                .overlay(Image(systemName: "photo").font(.largeTitle).foregroundColor(.gray.opacity(0.4)))
                        }
                    }
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .frame(height: 350)
            .background(Color.white)
            
            // Commission badge
            if product.commissionRate > 0 {
                Text("\(Int(product.commissionRate))% Commission")
                    .font(.caption2.bold())
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(AppColors.secondaryColor)
                    .cornerRadius(6)
                    .padding(12)
            }
        }
    }
    
    // MARK: - Title + Price
    @ViewBuilder
    private func titlePriceSection(product: Product) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(product.name)
                .font(.title2.bold())
                .foregroundColor(AppColors.titleTextColor)
            
            if !product.categoryName.isEmpty {
                Text(product.categoryName)
                    .font(.caption)
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(AppColors.chipsColor)
                    .cornerRadius(12)
            }
            
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text("$\(String(format: "%.2f", product.price))")
                    .font(.title.bold())
                    .foregroundColor(AppColors.primary)
                
                if product.originalPrice > 0 && product.originalPrice > product.price {
                    Text("$\(String(format: "%.2f", product.originalPrice))")
                        .font(.subheadline)
                        .strikethrough()
                        .foregroundColor(.gray)
                    
                    let discount = Int(((product.originalPrice - product.price) / product.originalPrice) * 100)
                    Text("-\(discount)%")
                        .font(.caption.bold())
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.red)
                        .cornerRadius(6)
                }
            }
        }
    }
    
    // MARK: - Stats Row
    @ViewBuilder
    private func statsRow(product: Product) -> some View {
        HStack(spacing: 0) {
            statItem(icon: "star.fill", value: String(format: "%.1f", product.rating), label: "Rating", color: .orange)
            Divider().frame(height: 40)
            statItem(icon: "cube.box.fill", value: "\(product.quantity)", label: "In Stock", color: product.quantity > 0 ? .green : .red)
            Divider().frame(height: 40)
            statItem(icon: "eye.fill", value: "--", label: "Views", color: .blue)
        }
        .padding(.vertical, 8)
        .background(Color.gray.opacity(0.06))
        .cornerRadius(12)
    }
    
    private func statItem(icon: String, value: String, label: String, color: Color) -> some View {
        VStack(spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon).foregroundColor(color).font(.caption)
                Text(value).fontWeight(.bold).font(.subheadline)
            }
            Text(label).font(.caption2).foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
    
    // MARK: - Commission Card
    @ViewBuilder
    private func commissionCard(product: Product) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "dollarsign.circle.fill")
                    .foregroundColor(.green)
                Text("Earning Potential")
                    .fontWeight(.semibold)
            }
            
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Commission Rate")
                        .font(.caption).foregroundColor(.secondary)
                    Text("\(Int(product.commissionRate))%")
                        .font(.title3.bold()).foregroundColor(.green)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Earn per sale")
                        .font(.caption).foregroundColor(.secondary)
                    Text("$\(String(format: "%.2f", product.price * product.commissionRate / 100))")
                        .font(.title3.bold()).foregroundColor(AppColors.primary)
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.green.opacity(0.06))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.green.opacity(0.2), lineWidth: 1))
        )
    }
    
    // MARK: - Size & Color
    @ViewBuilder
    private func sizeColorSection(sizeColorData: String) -> some View {
        // Parse sizeColorData — expected JSON string like {"sizes":["S","M","L"],"colors":["Red","Blue"]}
        let sizes = parseSizes(from: sizeColorData)
        let colors = parseColors(from: sizeColorData)
        
        VStack(alignment: .leading, spacing: 12) {
            if !sizes.isEmpty {
                Text("Size").font(.headline)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(sizes, id: \.self) { size in
                            Button {
                                selectedSize = selectedSize == size ? nil : size
                            } label: {
                                Text(size)
                                    .font(.subheadline)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(selectedSize == size ? AppColors.primary : Color.gray.opacity(0.1))
                                    .foregroundColor(selectedSize == size ? .white : .primary)
                                    .cornerRadius(8)
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(selectedSize == size ? AppColors.primary : Color.gray.opacity(0.3), lineWidth: 1))
                            }
                        }
                    }
                }
            }
            
            if !colors.isEmpty {
                Text("Color").font(.headline)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(colors, id: \.self) { color in
                            Button {
                                selectedColor = selectedColor == color ? nil : color
                            } label: {
                                HStack(spacing: 6) {
                                    Circle()
                                        .fill(colorFromName(color))
                                        .frame(width: 16, height: 16)
                                    Text(color)
                                        .font(.subheadline)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(selectedColor == color ? AppColors.primary.opacity(0.1) : Color.gray.opacity(0.1))
                                .cornerRadius(8)
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(selectedColor == color ? AppColors.primary : Color.gray.opacity(0.3), lineWidth: 1))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Description
    @ViewBuilder
    private func descriptionSection(product: Product) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Description").font(.headline)
            Text(product.description_)
                .font(.body)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
    
    // MARK: - Tags
    @ViewBuilder
    private func tagsSection(tags: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Tags").font(.headline)
            FlowLayout(spacing: 8) {
                ForEach(tags, id: \.self) { tag in
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
    
    // MARK: - Quantity Selector
    @ViewBuilder
    private func quantitySelector(product: Product) -> some View {
        HStack {
            Text("Quantity").font(.headline)
            Spacer()
            HStack(spacing: 16) {
                Button {
                    if quantity > 1 { quantity -= 1 }
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title2)
                        .foregroundColor(quantity > 1 ? AppColors.primary : .gray)
                }
                .disabled(quantity <= 1)
                
                Text("\(quantity)")
                    .font(.title3.bold())
                    .frame(minWidth: 32)
                
                Button {
                    if quantity < Int(product.quantity) { quantity += 1 }
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundColor(quantity < Int(product.quantity) ? AppColors.primary : .gray)
                }
                .disabled(quantity >= Int(product.quantity))
            }
        }
    }
    
    // MARK: - Bottom Action Bar
    @ViewBuilder
    private func bottomActionBar(product: Product) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Total")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("$\(String(format: "%.2f", product.price * Double(quantity)))")
                    .font(.title3.bold())
                    .foregroundColor(AppColors.primary)
            }
            
            Spacer()
            
            if product.commissionRate > 0 {
                Button { showPromoteSheet = true } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "megaphone.fill")
                        Text("Promote")
                    }
                    .font(.subheadline.bold())
                    .foregroundColor(AppColors.secondaryColor)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(AppColors.secondaryColor, lineWidth: 1.5))
                }
            }
            
            Button {
                viewModel.addToCart(quantity: quantity, size: selectedSize, color: selectedColor)
                withAnimation(.spring()) { addedToCartToast = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    withAnimation { addedToCartToast = false }
                }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "cart.fill.badge.plus")
                    Text("Add to Cart")
                }
                .font(.subheadline.bold())
                .foregroundColor(.white)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(
                    LinearGradient(colors: [AppColors.primary, AppColors.secondaryColor], startPoint: .leading, endPoint: .trailing)
                )
                .cornerRadius(10)
            }
            .disabled(viewModel.isLoading || product.quantity <= 0)
        }
        .padding()
        .background(
            Color.white
                .shadow(color: .black.opacity(0.08), radius: 8, y: -4)
        )
    }
    
    // MARK: - Helpers
    
    private func productImages(_ product: Product) -> [String] {
        var images: [String] = []
        if let productImages = product.productImages as? [String], !productImages.isEmpty {
            images = productImages
        }
        if images.isEmpty && !product.imageUrl.isEmpty {
            images = [product.imageUrl]
        }
        if images.isEmpty {
            images = [product.image]
        }
        return images.filter { !$0.isEmpty }
    }
    
    private func parseSizes(from json: String) -> [String] {
        guard let data = json.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let sizes = dict["sizes"] as? [String] else { return [] }
        return sizes
    }
    
    private func parseColors(from json: String) -> [String] {
        guard let data = json.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let colors = dict["colors"] as? [String] else { return [] }
        return colors
    }
    
    private func colorFromName(_ name: String) -> Color {
        switch name.lowercased() {
        case "red": return .red
        case "blue": return .blue
        case "green": return .green
        case "black": return .black
        case "white": return .gray.opacity(0.2)
        case "yellow": return .yellow
        case "pink": return .pink
        case "purple": return .purple
        case "orange": return .orange
        case "brown": return .brown
        case "gray", "grey": return .gray
        default: return .gray.opacity(0.3)
        }
    }
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Flow Layout for Tags
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: ProposedViewSize(width: bounds.width, height: nil), subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }
    
    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        
        return (CGSize(width: maxWidth, height: y + rowHeight), positions)
    }
}

// MARK: - Promote Product Sheet
struct PromoteProductSheet: View {
    let product: Product
    @ObservedObject var viewModel: ProductDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedPostId: String? = nil
    
    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                // Product preview
                HStack(spacing: 12) {
                    AsyncImage(url: URL(string: product.imageUrl)) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Color.gray.opacity(0.1)
                    }
                    .frame(width: 60, height: 60)
                    .cornerRadius(8)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(product.name).font(.subheadline.bold()).lineLimit(1)
                        Text("$\(String(format: "%.2f", product.price))")
                            .foregroundColor(AppColors.primary)
                        Text("Earn $\(String(format: "%.2f", product.price * product.commissionRate / 100)) per sale")
                            .font(.caption).foregroundColor(.green)
                    }
                    Spacer()
                }
                .padding()
                .background(Color.gray.opacity(0.06))
                .cornerRadius(12)
                
                Text("Select a post to attach this promotion:")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                if viewModel.userPosts.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "square.stack.3d.up.slash")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text("No posts available")
                            .foregroundColor(.secondary)
                        Text("Create a video post first to promote this product.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, 30)
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 4) {
                            ForEach(viewModel.userPosts, id: \.id) { post in
                                Button {
                                    selectedPostId = String(post.id)
                                } label: {
                                    ZStack {
                                        AsyncImage(url: URL(string: post.imageUrl)) { img in
                                            img.resizable().aspectRatio(contentMode: .fill)
                                        } placeholder: {
                                            Color.gray.opacity(0.1)
                                        }
                                        .frame(height: 100)
                                        .clipped()
                                        
                                        if selectedPostId == String(post.id) {
                                            Color.blue.opacity(0.4)
                                            Image(systemName: "checkmark.circle.fill")
                                                .font(.title)
                                                .foregroundColor(.white)
                                        }
                                    }
                                    .cornerRadius(8)
                                }
                            }
                        }
                    }
                }
                
                Spacer()
                
                Button {
                    if let postId = selectedPostId {
                        viewModel.createPromotion(postId: postId)
                        dismiss()
                    }
                } label: {
                    Text("Create Promotion")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(selectedPostId != nil ? AppColors.primary : Color.gray)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                .disabled(selectedPostId == nil)
            }
            .padding()
            .navigationTitle("Promote Product")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
            .onAppear {
                viewModel.loadUserPosts()
            }
        }
    }
}
