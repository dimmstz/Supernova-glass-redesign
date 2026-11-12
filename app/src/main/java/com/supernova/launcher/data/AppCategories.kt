package com.supernova.launcher.data

import android.content.pm.ApplicationInfo

// Category buckets for Smart Folders. Localized display names live in
// EnumLabels (the data layer stays resource-free).
enum class SmartCategory {
    GAMES,
    // Music and video together.
    ENTERTAINMENT,
    IMAGE,
    SOCIAL,
    NEWS,
    MAPS,
    PRODUCTIVITY,
    FINANCE,
    SHOPPING,
    OTHER
}

// Resolves an app to a Smart Folder category: the curated package-prefix map
// wins (it covers apps that never set ApplicationInfo.category, and fixes a
// few that set it oddly), then the manifest category.
object AppCategories {

    // Prefix -> category. Matched by "package startsWith prefix"; keep
    // prefixes specific enough not to swallow unrelated vendors.
    private val prefixMap: List<Pair<String, SmartCategory>> = listOf(
        // Social / messaging
        "com.whatsapp" to SmartCategory.SOCIAL,
        "org.telegram" to SmartCategory.SOCIAL,
        "com.instagram" to SmartCategory.SOCIAL,
        "com.facebook.katana" to SmartCategory.SOCIAL,
        "com.facebook.lite" to SmartCategory.SOCIAL,
        "com.facebook.orca" to SmartCategory.SOCIAL,
        "com.snapchat" to SmartCategory.SOCIAL,
        "com.twitter" to SmartCategory.SOCIAL,
        "com.zhiliaoapp.musically" to SmartCategory.SOCIAL,
        "com.linkedin" to SmartCategory.SOCIAL,
        "com.reddit" to SmartCategory.SOCIAL,
        "com.discord" to SmartCategory.SOCIAL,
        "org.thoughtcrime.securesms" to SmartCategory.SOCIAL,
        "com.pinterest" to SmartCategory.SOCIAL,
        "jp.naver.line" to SmartCategory.SOCIAL,
        "com.google.android.apps.tachyon" to SmartCategory.SOCIAL,
        "com.google.android.apps.meetings" to SmartCategory.SOCIAL,
        "com.google.android.apps.messaging" to SmartCategory.SOCIAL,
        "com.samsung.android.messaging" to SmartCategory.SOCIAL,
        // Entertainment: music
        "com.spotify" to SmartCategory.ENTERTAINMENT,
        "com.google.android.apps.youtube.music" to SmartCategory.ENTERTAINMENT,
        "com.amazon.mp3" to SmartCategory.ENTERTAINMENT,
        "com.soundcloud" to SmartCategory.ENTERTAINMENT,
        "com.gaana" to SmartCategory.ENTERTAINMENT,
        "com.jio.media.jiobeats" to SmartCategory.ENTERTAINMENT,
        "com.shazam" to SmartCategory.ENTERTAINMENT,
        // Entertainment: video
        "com.google.android.youtube" to SmartCategory.ENTERTAINMENT,
        "com.netflix" to SmartCategory.ENTERTAINMENT,
        "com.amazon.avod" to SmartCategory.ENTERTAINMENT,
        "com.disney" to SmartCategory.ENTERTAINMENT,
        "com.hotstar" to SmartCategory.ENTERTAINMENT,
        "in.startv.hotstar" to SmartCategory.ENTERTAINMENT,
        "com.mxtech.videoplayer" to SmartCategory.ENTERTAINMENT,
        "org.videolan.vlc" to SmartCategory.ENTERTAINMENT,
        "tv.twitch" to SmartCategory.ENTERTAINMENT,
        // Image / camera
        "com.google.android.apps.photos" to SmartCategory.IMAGE,
        "com.adobe.lrmobile" to SmartCategory.IMAGE,
        "com.vsco" to SmartCategory.IMAGE,
        "com.canva.editor" to SmartCategory.IMAGE,
        // News
        "com.google.android.apps.magazines" to SmartCategory.NEWS,
        "flipboard.app" to SmartCategory.NEWS,
        "com.dailyhunt" to SmartCategory.NEWS,
        "bbc.mobile.news" to SmartCategory.NEWS,
        // Maps / travel
        "com.google.android.apps.maps" to SmartCategory.MAPS,
        "com.waze" to SmartCategory.MAPS,
        "com.ubercab" to SmartCategory.MAPS,
        "com.olacabs" to SmartCategory.MAPS,
        "in.redbus" to SmartCategory.MAPS,
        "com.rapido" to SmartCategory.MAPS,
        // Productivity
        "com.google.android.apps.docs" to SmartCategory.PRODUCTIVITY,
        "com.google.android.keep" to SmartCategory.PRODUCTIVITY,
        "com.google.android.calendar" to SmartCategory.PRODUCTIVITY,
        "com.google.android.gm" to SmartCategory.PRODUCTIVITY,
        "com.microsoft.office" to SmartCategory.PRODUCTIVITY,
        "com.microsoft.teams" to SmartCategory.PRODUCTIVITY,
        "com.todoist" to SmartCategory.PRODUCTIVITY,
        "com.notion.id" to SmartCategory.PRODUCTIVITY,
        "com.evernote" to SmartCategory.PRODUCTIVITY,
        "com.slack" to SmartCategory.PRODUCTIVITY,
        "us.zoom" to SmartCategory.PRODUCTIVITY,
        // Finance / banking / UPI
        "com.phonepe" to SmartCategory.FINANCE,
        "net.one97.paytm" to SmartCategory.FINANCE,
        "com.google.android.apps.nbu.paisa" to SmartCategory.FINANCE,
        "in.org.npci.upiapp" to SmartCategory.FINANCE,
        "com.sbi." to SmartCategory.FINANCE,
        "com.icicibank" to SmartCategory.FINANCE,
        "com.snapwork.hdfc" to SmartCategory.FINANCE,
        "com.axis.mobile" to SmartCategory.FINANCE,
        "com.paypal" to SmartCategory.FINANCE,
        "com.venmo" to SmartCategory.FINANCE,
        "com.revolut" to SmartCategory.FINANCE,
        "com.coinbase" to SmartCategory.FINANCE,
        "com.zerodha" to SmartCategory.FINANCE,
        "in.zerodha" to SmartCategory.FINANCE,
        "com.groww" to SmartCategory.FINANCE,
        "com.nu.production" to SmartCategory.FINANCE,
        "com.itau" to SmartCategory.FINANCE,
        "com.bradesco" to SmartCategory.FINANCE,
        "com.santander" to SmartCategory.FINANCE,
        "br.com.intermedium" to SmartCategory.FINANCE,
        "com.bancopan" to SmartCategory.FINANCE,
        "com.mercadopago" to SmartCategory.FINANCE,
        "com.picpay" to SmartCategory.FINANCE,
        // Shopping and delivery
        "com.mercadolibre" to SmartCategory.SHOPPING,
        "com.amazon" to SmartCategory.SHOPPING,
        "com.shopee" to SmartCategory.SHOPPING,
        "com.alibaba" to SmartCategory.SHOPPING,
        "com.grupoboticario" to SmartCategory.SHOPPING,
        "com.magazineluiza" to SmartCategory.SHOPPING,
        "com.ifood" to SmartCategory.SHOPPING,
        "com.rappi" to SmartCategory.SHOPPING
    )

    // systemCategory is ApplicationInfo.category (CATEGORY_UNDEFINED when the
    // manifest doesn't declare one). Returns null when the app fits nowhere.
    fun categoryOf(packageName: String, systemCategory: Int, label: String = ""): SmartCategory? {
        prefixMap.firstOrNull { packageName.startsWith(it.first) }?.let { return it.second }
        val system = when (systemCategory) {
            ApplicationInfo.CATEGORY_GAME -> SmartCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> SmartCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_VIDEO -> SmartCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_IMAGE -> SmartCategory.IMAGE
            ApplicationInfo.CATEGORY_SOCIAL -> SmartCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> SmartCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> SmartCategory.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> SmartCategory.PRODUCTIVITY
            else -> null
        }
        if (system != null) return system
        val text = label.lowercase()
        return when {
            listOf("jogo", "game", "games", "rpg", "puzzle", "arcade", "chess", "soccer", "football").any(text::contains) -> SmartCategory.GAMES
            listOf("banco", "bank", "finan", "pagamento", "wallet", "carteira").any(text::contains) -> SmartCategory.FINANCE
            listOf("compras", "shopping", "loja", "store", "mercado", "delivery").any(text::contains) -> SmartCategory.SHOPPING
            listOf("chat", "mensag", "social", "comunidade", "forum", "community").any(text::contains) -> SmartCategory.SOCIAL
            listOf("foto", "photo", "camera", "câmera", "galeria", "gallery", "image", "eraser", "editor", "canva", "capcut").any(text::contains) -> SmartCategory.IMAGE
            listOf("mapa", "map", "gps", "viagem", "transporte", "taxi").any(text::contains) -> SmartCategory.MAPS
            listOf("notas", "notes", "calend", "email", "e-mail", "document", "office", "tarefa", "authenticator", "authentication", "senha", "password", "calculator", "calculadora", "base44", "browser", "navegador", "files", "arquivo", "drive").any(text::contains) -> SmartCategory.PRODUCTIVITY
            listOf("notícia", "noticia", "news", "jornal").any(text::contains) -> SmartCategory.NEWS
            listOf("music", "música", "video", "vídeo", "stream", "filme", "tv").any(text::contains) -> SmartCategory.ENTERTAINMENT
            // Do not silently leave an app outside smart organization. The
            // fallback is intentionally explicit so users can still find
            // unknown or regional apps in one useful category.
            else -> SmartCategory.OTHER
        }
    }
}
