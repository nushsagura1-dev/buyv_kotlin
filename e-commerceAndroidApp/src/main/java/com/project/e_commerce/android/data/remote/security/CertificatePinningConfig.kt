package com.project.e_commerce.android.data.remote.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/**
 * Configuration du Certificate Pinning pour OkHttp.
 * 
 * Le pinning vérifie que le certificat du serveur correspond aux empreintes SHA-256
 * attendues, empêchant les attaques MITM (Man-In-The-Middle).
 * 
 * ⚠️ IMPORTANT : Les pins doivent être mis à jour AVANT le déploiement en production !
 * 
 * Documentation :
 * - https://square.github.io/okhttp/4.x/okhttp/okhttp3/-certificate-pinner/
 * - https://developer.android.com/training/articles/security-config
 */
object CertificatePinningConfig {
    
    /**
     * Domaine de l'API backend en production.
     * 
     * ⚠️ À REMPLACER par votre domaine réel avant déploiement !
     */
    private const val API_DOMAIN = "api.buyv.com"
    
    /**
     * Indique si le pinning est activé.
     * 
     * En développement (localhost), le pinning est désactivé.
     * En production, il doit être activé pour sécuriser les communications.
     */
    private const val ENABLE_PINNING = false // Mettre à true en production avec vrais pins
    
    /**
     * Crée un CertificatePinner configuré avec les pins de l'API backend.
     * 
     * ⚠️ IMPORTANT : Générer les vrais pins avant activation !
     * 
     * Comment obtenir le SHA-256 pin :
     * ```bash
     * # Option 1 : OpenSSL
     * echo | openssl s_client -servername api.buyv.com -connect api.buyv.com:443 2>/dev/null | \
     *   openssl x509 -pubkey -noout | \
     *   openssl pkey -pubin -outform der | \
     *   openssl dgst -sha256 -binary | \
     *   base64
     * 
     * # Option 2 : Utiliser l'outil en ligne
     * # https://www.ssllabs.com/ssltest/analyze.html?d=api.buyv.com
     * ```
     * 
     * @return CertificatePinner configuré, ou null si pinning désactivé
     */
    fun createCertificatePinner(): CertificatePinner? {
        if (!ENABLE_PINNING) {
            println("⚠️ Certificate Pinning is DISABLED - Enable in production!")
            return null
        }
        
        return CertificatePinner.Builder()
            // Pin primaire - Certificat actuel du serveur
            .add(
                API_DOMAIN,
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // ⚠️ REMPLACER
            )
            // Pin backup - Certificat de secours ou CA intermédiaire
            .add(
                API_DOMAIN,
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=" // ⚠️ REMPLACER
            )
            // Pin CA racine (optionnel mais recommandé)
            .add(
                API_DOMAIN,
                "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=" // ⚠️ REMPLACER
            )
            .build()
    }
    
    /**
     * Configure un OkHttpClient.Builder avec certificate pinning.
     * 
     * Usage :
     * ```kotlin
     * val okHttpClient = OkHttpClient.Builder()
     *     .apply {
     *         CertificatePinningConfig.configurePinning(this)
     *     }
     *     .build()
     * ```
     * 
     * @param builder Le OkHttpClient.Builder à configurer
     */
    fun configurePinning(builder: OkHttpClient.Builder) {
        createCertificatePinner()?.let { pinner ->
            builder.certificatePinner(pinner)
            println("✅ Certificate Pinning ENABLED for $API_DOMAIN")
        }
    }
    
    /**
     * Pins pré-générés pour Let's Encrypt (utilisé par Railway, Heroku, etc.)
     * 
     * Ces pins correspondent aux certificats CA racine de Let's Encrypt
     * et peuvent être utilisés comme fallback.
     * 
     * Source : https://letsencrypt.org/certificates/
     * Expiration : Septembre 2025 (ISRG Root X1)
     */
    object LetsEncryptPins {
        // ISRG Root X1 (Primary)
        const val ISRG_ROOT_X1 = "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M="
        
        // ISRG Root X2 (Backup)
        const val ISRG_ROOT_X2 = "sha256/6Yhhhh6+LmWK5HMYkRqgQcDtEmn2HKt8Z4o0F4JJckg="
        
        /**
         * Configure le pinning avec les certificats Let's Encrypt.
         * Utile pour Railway/Heroku avec certificats auto-générés.
         */
        fun createPinner(domain: String): CertificatePinner {
            return CertificatePinner.Builder()
                .add(domain, ISRG_ROOT_X1)
                .add(domain, ISRG_ROOT_X2)
                .build()
        }
    }
    
    /**
     * Pins pré-générés pour Cloudflare (si hébergé derrière Cloudflare)
     * 
     * Source : https://developers.cloudflare.com/ssl/
     */
    object CloudflarePins {
        // Cloudflare Root CA - ECC
        const val CLOUDFLARE_ROOT_CA_ECC = "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI="
        
        // Cloudflare Root CA - RSA
        const val CLOUDFLARE_ROOT_CA_RSA = "sha256/58qRu/uxh4gFezqAcERupSkRYBlBAvfcw7mEjGPLnNU="
        
        fun createPinner(domain: String): CertificatePinner {
            return CertificatePinner.Builder()
                .add(domain, CLOUDFLARE_ROOT_CA_ECC)
                .add(domain, CLOUDFLARE_ROOT_CA_RSA)
                .build()
        }
    }
}
