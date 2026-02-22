import SwiftUI

enum NotificationCategory: String, CaseIterable {
    case all = "All"
    case social = "Social"
    case orders = "Orders"
    case promotions = "Promotions"
    
    var icon: String {
        switch self {
        case .all: return "bell.fill"
        case .social: return "person.2.fill"
        case .orders: return "bag.fill"
        case .promotions: return "megaphone.fill"
        }
    }
    
    func matches(type: String?) -> Bool {
        switch self {
        case .all: return true
        case .social: return ["new_follower", "new_like", "new_comment"].contains(type ?? "")
        case .orders: return ["new_order", "order_status_update"].contains(type ?? "")
        case .promotions: return type == "promotion"
        }
    }
}

struct NotificationView: View {
    @StateObject private var viewModel = NotificationViewModel()
    @State private var selectedCategory: NotificationCategory = .all
    
    private var filteredNotifications: [NotificationResponse] {
        if selectedCategory == .all {
            return viewModel.notifications
        }
        return viewModel.notifications.filter { selectedCategory.matches(type: $0.type) }
    }
    
    private var todayNotifications: [NotificationResponse] {
        filteredNotifications.filter { isToday($0.createdAt) }
    }
    
    private var yesterdayNotifications: [NotificationResponse] {
        filteredNotifications.filter { isYesterday($0.createdAt) }
    }
    
    private var olderNotifications: [NotificationResponse] {
        filteredNotifications.filter { !isToday($0.createdAt) && !isYesterday($0.createdAt) }
    }
    
    var body: some View {
        ZStack {
            AppColors.background.ignoresSafeArea()
            
            if viewModel.isLoading && viewModel.notifications.isEmpty {
                ProgressView("Loading notifications...")
                    .progressViewStyle(CircularProgressViewStyle())
            } else if let errorMessage = viewModel.errorMessage, viewModel.notifications.isEmpty {
                errorView(errorMessage)
            } else if viewModel.notifications.isEmpty {
                emptyView()
            } else {
                VStack(spacing: 0) {
                    // Category filter chips
                    categoryFilter()
                    
                    // Notification list with temporal grouping
                    notificationList()
                }
            }
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: { viewModel.markAllAsRead() }) {
                        Label("Mark All Read", systemImage: "envelope.open.fill")
                    }
                    Button(role: .destructive, action: {
                        withAnimation { viewModel.clearAll() }
                    }) {
                        Label("Clear All", systemImage: "trash.fill")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(AppColors.primary)
                }
                .opacity(viewModel.notifications.isEmpty ? 0 : 1)
            }
            
            ToolbarItem(placement: .navigationBarLeading) {
                if viewModel.unreadCount > 0 {
                    Text("\(viewModel.unreadCount) unread")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(AppColors.primary)
                        .cornerRadius(10)
                }
            }
        }
        .onAppear {
            viewModel.loadNotifications()
        }
    }
    
    // MARK: - Category Filter
    @ViewBuilder
    private func categoryFilter() -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(NotificationCategory.allCases, id: \.self) { category in
                    let count = category == .all
                        ? viewModel.notifications.count
                        : viewModel.notifications.filter { category.matches(type: $0.type) }.count
                    
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedCategory = category
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: category.icon)
                                .font(.caption2)
                            Text(category.rawValue)
                                .font(.caption)
                                .fontWeight(.semibold)
                            if count > 0 {
                                Text("\(count)")
                                    .font(.caption2)
                                    .fontWeight(.bold)
                                    .padding(.horizontal, 4)
                                    .padding(.vertical, 1)
                                    .background(
                                        selectedCategory == category
                                            ? Color.white.opacity(0.3)
                                            : Color.gray.opacity(0.15)
                                    )
                                    .cornerRadius(6)
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(selectedCategory == category ? AppColors.primary : Color.gray.opacity(0.1))
                        .foregroundColor(selectedCategory == category ? .white : .primary)
                        .cornerRadius(20)
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
        .background(Color.white)
    }
    
    // MARK: - Notification List
    @ViewBuilder
    private func notificationList() -> some View {
        if filteredNotifications.isEmpty {
            VStack(spacing: 12) {
                Image(systemName: "tray")
                    .font(.system(size: 40))
                    .foregroundColor(.secondary)
                Text("No \(selectedCategory.rawValue.lowercased()) notifications")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .frame(maxHeight: .infinity)
        } else {
            List {
                if !todayNotifications.isEmpty {
                    Section(header: sectionHeader("Today")) {
                        ForEach(todayNotifications) { notification in
                            notificationRow(notification)
                        }
                        .onDelete { indexSet in
                            deleteFromSection(indexSet, notifications: todayNotifications)
                        }
                    }
                }
                
                if !yesterdayNotifications.isEmpty {
                    Section(header: sectionHeader("Yesterday")) {
                        ForEach(yesterdayNotifications) { notification in
                            notificationRow(notification)
                        }
                        .onDelete { indexSet in
                            deleteFromSection(indexSet, notifications: yesterdayNotifications)
                        }
                    }
                }
                
                if !olderNotifications.isEmpty {
                    Section(header: sectionHeader("Earlier")) {
                        ForEach(olderNotifications) { notification in
                            notificationRow(notification)
                        }
                        .onDelete { indexSet in
                            deleteFromSection(indexSet, notifications: olderNotifications)
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .refreshable {
                viewModel.loadNotifications()
            }
        }
    }
    
    // MARK: - Section Header
    @ViewBuilder
    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.subheadline)
            .fontWeight(.bold)
            .foregroundColor(.primary)
            .textCase(nil)
    }
    
    // MARK: - Notification Row
    @ViewBuilder
    private func notificationRow(_ notification: NotificationResponse) -> some View {
        HStack(spacing: 12) {
            // Icon
            ZStack {
                Circle()
                    .fill(notification.iconColor.opacity(0.15))
                    .frame(width: 44, height: 44)
                
                Image(systemName: notification.icon)
                    .font(.system(size: 18))
                    .foregroundColor(notification.iconColor)
            }
            
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(notification.title)
                        .font(.subheadline)
                        .fontWeight(notification.isRead ? .regular : .bold)
                    
                    if !notification.isRead {
                        Circle()
                            .fill(AppColors.primary)
                            .frame(width: 8, height: 8)
                    }
                    
                    Spacer()
                    
                    Text(notification.relativeTime)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                Text(notification.body)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
        .opacity(notification.isRead ? 0.7 : 1.0)
        .contentShape(Rectangle())
        .onTapGesture {
            if !notification.isRead {
                viewModel.markAsRead(notification)
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                if let idx = viewModel.notifications.firstIndex(where: { $0.id == notification.id }) {
                    viewModel.deleteNotification(at: IndexSet(integer: idx))
                }
            } label: {
                Label("Delete", systemImage: "trash.fill")
            }
            
            if !notification.isRead {
                Button {
                    viewModel.markAsRead(notification)
                } label: {
                    Label("Read", systemImage: "envelope.open.fill")
                }
                .tint(.blue)
            }
        }
    }
    
    // MARK: - Error View
    @ViewBuilder
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.secondary)
            Text("Error")
                .font(Typography.h3)
                .foregroundColor(.secondary)
            Text(message)
                .font(Typography.body2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.loadNotifications()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 10)
            .background(AppColors.primary)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        .padding()
    }
    
    // MARK: - Empty View
    @ViewBuilder
    private func emptyView() -> some View {
        VStack(spacing: 16) {
            Image(systemName: "bell.slash")
                .font(.system(size: 50))
                .foregroundColor(.secondary)
            Text("No Notifications")
                .font(Typography.h3)
                .foregroundColor(.secondary)
            Text("You're all caught up!")
                .font(Typography.body2)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Helpers
    private func deleteFromSection(_ indexSet: IndexSet, notifications: [NotificationResponse]) {
        let ids = indexSet.map { notifications[$0].id }
        let globalOffsets = ids.compactMap { id in
            viewModel.notifications.firstIndex(where: { $0.id == id })
        }
        viewModel.deleteNotification(at: IndexSet(globalOffsets))
    }
    
    private func parseDate(_ dateStr: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: dateStr) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: dateStr)
    }
    
    private func isToday(_ dateStr: String) -> Bool {
        guard let date = parseDate(dateStr) else { return false }
        return Calendar.current.isDateInToday(date)
    }
    
    private func isYesterday(_ dateStr: String) -> Bool {
        guard let date = parseDate(dateStr) else { return false }
        return Calendar.current.isDateInYesterday(date)
    }
}
