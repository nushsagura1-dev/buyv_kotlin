import SwiftUI

struct AdminCJImportView: View {
    @StateObject private var viewModel = AdminCJImportViewModel()
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            headerSection
            
            // Search Bar
            searchBarSection
            
            // Category Chips
            categoryChips
            
            // Content
            contentSection
        }
        .navigationTitle("CJ Import")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $viewModel.showImportDialog) {
            importDialog
        }
        .alert("Product Imported!", isPresented: $viewModel.showImportSuccess) {
            Button("Continue Importing") {
                viewModel.dismissImportSuccess()
            }
        } message: {
            if let product = viewModel.lastImportedProduct {
                Text("\(product.name) has been imported successfully at $\(String(format: "%.2f", product.sellingPrice))")
            }
        }
    }
    
    // MARK: - Header
    private var headerSection: some View {
        HStack(spacing: 12) {
            Image(systemName: "shippingbox.fill")
                .font(.title2)
                .foregroundColor(.white)
            
            VStack(alignment: .leading) {
                Text("CJ Dropshipping")
                    .font(.headline)
                    .foregroundColor(.white)
                Text("Search and import products")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
            }
            
            Spacer()
        }
        .padding()
        .background(
            LinearGradient(colors: [Color.indigo, Color.purple], startPoint: .leading, endPoint: .trailing)
        )
    }
    
    // MARK: - Search Bar
    private var searchBarSection: some View {
        HStack(spacing: 8) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField("Search CJ products...", text: $viewModel.searchQuery)
                    .textFieldStyle(.plain)
                    .autocapitalization(.none)
                    .onSubmit {
                        Task { await viewModel.searchProducts() }
                    }
                if !viewModel.searchQuery.isEmpty {
                    Button(action: { viewModel.clearSearch() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            
            Button(action: {
                Task { await viewModel.searchProducts() }
            }) {
                Text("Search")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.indigo)
                    .cornerRadius(10)
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
                    Button(action: { viewModel.selectCategory(category) }) {
                        Text(category)
                            .font(.caption)
                            .fontWeight(.medium)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(viewModel.selectedCategory == category
                                        ? Color.indigo.opacity(0.2) : Color(.systemGray6))
                            .foregroundColor(viewModel.selectedCategory == category
                                             ? .indigo : .secondary)
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(viewModel.selectedCategory == category
                                            ? Color.indigo : .clear, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal)
        }
        .padding(.bottom, 4)
    }
    
    // MARK: - Content
    private var contentSection: some View {
        Group {
            if viewModel.isLoading {
                Spacer()
                VStack(spacing: 12) {
                    ProgressView()
                    Text("Searching CJ Dropshipping...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                Spacer()
            } else if let error = viewModel.errorMessage {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 40))
                        .foregroundColor(.red)
                    Text(error)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    Button("Retry") {
                        Task { await viewModel.searchProducts() }
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
                Spacer()
            } else if viewModel.searchResults.isEmpty && viewModel.searchQuery.isEmpty {
                welcomeView
            } else if viewModel.searchResults.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 40))
                        .foregroundColor(.secondary)
                    Text("No products found")
                        .font(.headline)
                    Text("Try different keywords or category")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                Spacer()
            } else {
                resultsList
            }
        }
    }
    
    // MARK: - Welcome View
    private var welcomeView: some View {
        VStack {
            Spacer()
            VStack(spacing: 16) {
                Image(systemName: "shippingbox.and.arrow.backward")
                    .font(.system(size: 60))
                    .foregroundColor(.indigo.opacity(0.5))
                
                Text("Import from CJ Dropshipping")
                    .font(.title3)
                    .fontWeight(.bold)
                
                Text("Search for products in the CJ catalog\nand import them into your marketplace")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                
                VStack(alignment: .leading, spacing: 8) {
                    WelcomeStep(number: 1, text: "Search for products by name or category")
                    WelcomeStep(number: 2, text: "Set your selling price and commission")
                    WelcomeStep(number: 3, text: "Import to your marketplace instantly")
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
            }
            .padding()
            Spacer()
        }
    }
    
    // MARK: - Results List
    private var resultsList: some View {
        VStack(spacing: 0) {
            // Results count
            HStack {
                Text("\(viewModel.totalResults) products found")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
            }
            .padding(.horizontal)
            .padding(.vertical, 4)
            
            List {
                ForEach(viewModel.searchResults) { product in
                    CJProductCard(
                        product: product,
                        isImported: viewModel.importedProductIds.contains(product.productId),
                        isImporting: viewModel.importingProductId == product.productId,
                        onImport: { viewModel.selectProduct(product) }
                    )
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    .onAppear {
                        if product.id == viewModel.searchResults.last?.id {
                            Task { await viewModel.loadNextPage() }
                        }
                    }
                }
                
                if viewModel.isLoadingMore {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                    .listRowInsets(EdgeInsets())
                    .padding()
                }
            }
            .listStyle(.plain)
        }
    }
    
    // MARK: - Import Dialog
    private var importDialog: some View {
        NavigationView {
            Form {
                if let product = viewModel.selectedProduct {
                    // Product Preview
                    Section("Product") {
                        HStack(spacing: 12) {
                            if let imageUrl = product.productImage, let url = URL(string: imageUrl) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.gray.opacity(0.3)
                                }
                                .frame(width: 70, height: 70)
                                .cornerRadius(8)
                            }
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.productName)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                    .lineLimit(2)
                                
                                Text("CJ Price: \(product.formattedPrice)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                if let category = product.categoryName {
                                    Text(category)
                                        .font(.caption2)
                                        .foregroundColor(.indigo)
                                }
                            }
                        }
                    }
                    
                    // Pricing
                    Section("Pricing") {
                        HStack {
                            Text("Selling Price ($)")
                            Spacer()
                            TextField("0.00", text: $viewModel.sellingPrice)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(width: 100)
                        }
                        
                        HStack {
                            Text("Commission Rate (%)")
                            Spacer()
                            TextField("10", text: $viewModel.commissionRate)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(width: 80)
                        }
                        
                        if let price = Double(viewModel.sellingPrice),
                           let rate = Double(viewModel.commissionRate) {
                            let margin = price - product.sellPrice
                            let commission = price * rate / 100
                            
                            HStack {
                                Text("Margin")
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(String(format: "$%.2f", margin))
                                    .foregroundColor(margin > 0 ? .green : .red)
                                    .fontWeight(.medium)
                            }
                            
                            HStack {
                                Text("Promoter Commission")
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(String(format: "$%.2f", commission))
                                    .foregroundColor(.blue)
                                    .fontWeight(.medium)
                            }
                        }
                    }
                    
                    // Description
                    Section("Custom Description (optional)") {
                        TextEditor(text: $viewModel.customDescription)
                            .frame(minHeight: 80)
                    }
                    
                    // Error
                    if let error = viewModel.importError {
                        Section {
                            Text(error)
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                    }
                    
                    // Import Button
                    Section {
                        Button(action: {
                            Task { await viewModel.importProduct() }
                        }) {
                            HStack {
                                Spacer()
                                if viewModel.isImporting {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Label("Import Product", systemImage: "square.and.arrow.down")
                                }
                                Spacer()
                            }
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .listRowBackground(Color.indigo)
                        }
                        .disabled(viewModel.isImporting)
                        .listRowBackground(Color.indigo)
                    }
                }
            }
            .navigationTitle("Import Product")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        viewModel.showImportDialog = false
                    }
                }
            }
        }
    }
}

// MARK: - CJ Product Card
struct CJProductCard: View {
    let product: CJProduct
    let isImported: Bool
    let isImporting: Bool
    let onImport: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Product image
            if let imageUrl = product.productImage, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.gray.opacity(0.2)
                        .overlay(Image(systemName: "photo").foregroundColor(.gray))
                }
                .frame(width: 90, height: 90)
                .cornerRadius(8)
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.gray.opacity(0.2))
                    .frame(width: 90, height: 90)
                    .overlay(Image(systemName: "shippingbox").foregroundColor(.gray))
            }
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(product.productName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)
                
                if let category = product.categoryName {
                    Text(category)
                        .font(.caption2)
                        .foregroundColor(.indigo)
                }
                
                HStack(spacing: 6) {
                    Text(product.formattedPrice)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.indigo)
                    
                    if let original = product.formattedOriginalPrice {
                        Text(original)
                            .font(.caption)
                            .strikethrough()
                            .foregroundColor(.secondary)
                    }
                }
                
                // Action button
                if isImported {
                    Label("Imported", systemImage: "checkmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.green)
                } else if isImporting {
                    HStack(spacing: 6) {
                        ProgressView()
                            .scaleEffect(0.7)
                        Text("Importing...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                } else {
                    Button(action: onImport) {
                        Label("Import", systemImage: "square.and.arrow.down")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 4)
                            .background(Color.indigo)
                            .cornerRadius(6)
                    }
                    .buttonStyle(.plain)
                }
            }
            
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Welcome Step
struct WelcomeStep: View {
    let number: Int
    let text: String
    
    var body: some View {
        HStack(spacing: 12) {
            Text("\(number)")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(width: 24, height: 24)
                .background(Color.indigo)
                .clipShape(Circle())
            
            Text(text)
                .font(.subheadline)
                .foregroundColor(.primary)
        }
    }
}
