import SwiftUI
import AVKit

struct VideoPlayerView: View {
    let url: String
    @State private var player: AVPlayer?
    @State private var isPlaying = true
    
    var body: some View {
        ZStack {
            if let player = player {
                VideoPlayer(player: player)
                    .onAppear {
                        player.play()
                        
                        NotificationCenter.default.addObserver(
                            forName: .AVPlayerItemDidPlayToEndTime,
                            object: player.currentItem,
                            queue: .main) { _ in
                                player.seek(to: .zero)
                                player.play()
                        }
                    }
                    .onDisappear {
                        player.pause()
                    }
            } else {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .onAppear {
                        if let videoURL = URL(string: url) {
                            let newPlayer = AVPlayer(url: videoURL)
                            newPlayer.volume = 1.0 
                            self.player = newPlayer
                            self.player?.play()
                        }
                    }
            }
        }
        .edgesIgnoringSafeArea(.all)
        .onTapGesture {
            if let player = player {
                if isPlaying {
                    player.pause()
                } else {
                    player.play()
                }
                isPlaying.toggle()
            }
        }
    }
}
