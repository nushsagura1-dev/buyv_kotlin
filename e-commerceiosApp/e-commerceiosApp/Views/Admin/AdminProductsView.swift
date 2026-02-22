import SwiftUI
import Shared

struct AdminProductsView: View {
    @StateObject private var viewModel = AdminProductsViewModel()
    @State private var showingAddProduct = false
    @State private var selectedProduct: Product?
    @State private var showingDeleteAlert = false
    @State private var productToDelete: Product?
    
    var body: some View {
        VStack(spacing: 0) {
            // Search Bar
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.gray)
                
                TextField("Search products...", text: $viewModel.searchQuery)
                    .textFieldStyle(.plain)
                
                if !viewModel.searchQuery.isEmpty {
                    Button(action: {
                        viewModel.searchQuery = ""
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding(12)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            .padding()
            
            // Filter Bar
            HStack {
                Picker("Category", selection: $viewModel.selectedCategory) {
                    Text("All").tag(nil as String?)
                    ForEach(viewModel.categories, id: \.self) { category in
                        Text(category).tag(category as String?)
                    }
                }
                .pickerStyle(.menu)
                
                Spacer()
                
                Toggle("Featured Only", isOn: $viewModel.showFeaturedOnly)
                    .font(.caption)
            }
            .padding(.horizontal)
            
            // Products List
            if viewModel.isLoading {
                ProgressView("Loading products...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.errorMessage != nil {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text(viewModel.errorMessage ?? "")
                        .multilineTextAlignment(.center)
                    Button("Retry") {
                        viewModel.loadProducts()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.orange)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.filteredProducts.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "bag")
                        .font(.system(size: 50))
                        .foregroundColor(.gray)
                    Text("No products found")
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.filteredProducts, id: \.id) { product in
                            AdminProductCard(
                                product: product,
                                onEdit: {
                                    selectedProduct = product
                                },
                                onDelete: {
                                    productToDelete = product
                                    showingDeleteAlert = true
                                },
                                onToggleFeatured: {
                                    viewModel.toggleFeatured(product)
                                }
                            )
                        }
                    }
                    .padding()
                }
                .refreshable {
                    viewModel.loadProducts()
                }
            }
        }
        .navigationTitle("Products Management")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    showingAddProduct = true
                }) {
                    Image(systemName: "plus.circle.fill")
                        .foregroundColor(.orange)
                }
            }
        }
        .sheet(isPresented: $showingAddProduct) {
            AdminProductFormView(mode: .add) { product in
                viewModel.addProduct(product)
            }
        }
        .sheet(item: $selectedProduct) { product in
            AdminProductFormView(mode: .edit(product)) { updatedProduct in
                viewModel.updateProduct(updatedProduct)
            }
        }
        .alert("Delete Product", isPresented: $showingDeleteAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Delete", role: .destructive) {
                if let product = productToDelete {
                    viewModel.deleteProduct(product)
                }
            }
        } message: {
            Text("Are you sure you want to delete this product?")
        }
        .onAppear {
            viewModel.loadProducts()
        }
    }
}

// MARK: - Admin Product Card
struct AdminProductCard: View {
    let product: Product
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onToggleFeatured: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                // Product Image
                AsyncImage(url: URL(string: product.images.first ?? "")) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.gray.opacity(0.2)
                }
                .frame(width: 80, height: 80)
                .cornerRadius(8)
                
                // Product Info
                VStack(alignment: .leading, spacing: 4) {
                    Text(product.name)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .lineLimit(2)
                    
                    Text(product.category)
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    HStack {
                        Text("$\(product.price, specifier: "%.2f")")
                            .font(.headline)
                            .foregroundColor(.orange)
                        
                        Spacer()
                        
                        if product.featured {
                            HStack(spacing: 4) {
                                Image(systemName: "star.fill")
                                    .font(.caption2)
                                Text("Featured")
                                    .font(.caption2)
                            }
                            .foregroundColor(.yellow)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.yellow.opacity(0.2))
                            .cornerRadius(8)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            
            // Action Buttons
            HStack(spacing: 12) {
                Button(action: onToggleFeatured) {
                    HStack {
                        Image(systemName: product.featured ? "star.fill" : "star")
                        Text(product.featured ? "Unfeature" : "Feature")
                    }
                    .font(.caption)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(Color.yellow.opacity(0.2))
                    .foregroundColor(.yellow)
                    .cornerRadius(8)
                }
                
                Button(action: onEdit) {
                    HStack {
                        Image(systemName: "pencil")
                        Text("Edit")
                    }
                    .font(.caption)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(Color.blue.opacity(0.2))
                    .foregroundColor(.blue)
                    .cornerRadius(8)
                }
                
                Button(action: onDelete) {
                    HStack {
                        Image(systemName: "trash")
                        Text("Delete")
                    }
                    .font(.caption)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(Color.red.opacity(0.2))
                    .foregroundColor(.red)
                    .cornerRadius(8)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

// MARK: - Admin Product Form View
struct AdminProductFormView: View {
    enum Mode {
        case add
        case edit(Product)
    }
    
    let mode: Mode
    let onSave: (Product) -> Void
    
    @Environment(\.dismiss) var dismiss
    @State private var name: String = ""
    @State private var description: String = ""
    @State private var price: String = ""
    @State private var category: String = ""
    @State private var imageUrl: String = ""
    @State private var featured: Bool = false
    
    var body: some View {
        NavigationView {
            Form {
                Section("Product Information") {
                    TextField("Name", text: $name)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(3...6)
                    TextField("Price", text: $price)
                        .keyboardType(.decimalPad)
                    TextField("Category", text: $category)
                }
                
                Section("Image") {
                    TextField("Image URL", text: $imageUrl)
                }
                
                Section("Options") {
                    Toggle("Featured Product", isOn: $featured)
                }
            }
            .navigationTitle(mode.isAddMode ? "Add Product" : "Edit Product")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        saveProduct()
                    }
                    .disabled(!isValid)
                }
            }
            .onAppear {
                if case .edit(let product) = mode {
                    name = product.name
                    description = product.description_
                    price = String(product.price)
                    category = product.category
                    imageUrl = product.images.first ?? ""
                    featured = product.featured
                }
            }
        }
    }
    
    private var isValid: Bool {
        !name.isEmpty && !price.isEmpty && !category.isEmpty
    }
    
    private func saveProduct() {
        // Build product DTO for API submission
        let product = Product(
            id: mode.isAddMode ? Int32.random(in: 1000...9999) : mode.productId,
            name: name,
            description_: description,
            price: Double(price) ?? 0.0,
            category: category,
            images: imageUrl.isEmpty ? [] : [imageUrl],
            stock: 100,
            featured: featured,
            sellerId: 1
        )
        
        onSave(product)
        dismiss()
    }
}

extension AdminProductFormView.Mode {
    var isAddMode: Bool {
        if case .add = self { return true }
        return false
    }
    
    var productId: Int32 {
        if case .edit(let product) = self { return product.id }
        return 0
    }
}

#Preview {
    NavigationView {
        AdminProductsView()
    }
}
