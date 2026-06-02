package com.example.data.xml

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

data class RecommendationItem(
    val category: String,
    val title: String,
    val url: String,
    val description: String
)

object XmlRecommendationEngine {
    private const val RECOMMENDATIONS_XML = """<?xml version="1.0" encoding="utf-8"?>
<recommendations>
    <item category="privacy">
        <title>[Privacy] Zero-Knowledge Architectures 101</title>
        <url>https://war.dev/privacy/zero-knowledge</url>
        <description>Discover why local-only client-side encryption is the future of personal data sovereignty.</description>
    </item>
    <item category="privacy">
        <title>[Privacy] Adblocking Filter Lists Deep Dive</title>
        <url>https://war.dev/privacy/filter-lists</url>
        <description>Learn how adblock rules block telemetry trackers, scripts and banners locally.</description>
    </item>
    <item category="security">
        <title>[Security] AES-256 GCM Cryptography</title>
        <url>https://war.dev/security/aes-gcm</url>
        <description>An analysis of securing client data against third-party injection during device syncing.</description>
    </item>
    <item category="security">
        <title>[Security] Managing Local Keys Safely</title>
        <url>https://war.dev/security/local-keys</url>
        <description>Tips on securing private browser databases and keystores across macOS, Windows and Android.</description>
    </item>
    <item category="tech">
        <title>[Tech] The Future of Kotlin &amp; Jetpack Compose</title>
        <url>https://kotlinlang.org/compose</url>
        <description>Why fluid Material Design 3 and Native UI declarations deliver superior speeds.</description>
    </item>
    <item category="tech">
        <title>[Tech] Cross-Platform Sync Engines</title>
        <url>https://war.dev/tech/sync-engines</url>
        <description>Building lightning-fast zero-knowledge sync states for macOS and Windows apps.</description>
    </item>
    <item category="general">
        <title>[Release] Warrior X Browser Launch by war.dev</title>
        <url>https://war.dev/warrior-x/launch</url>
        <description>The ultimate privacy-first browser arrives with localized adblockers and encrypted local databases.</description>
    </item>
    <item category="general">
        <title>[General] Google Search indexing standard</title>
        <url>https://google.com</url>
        <description>Leveraging Google's supreme search database with our local zero-knowledge secure wrapper.</description>
    </item>
</recommendations>
"""

    fun parseRecommendations(): List<RecommendationItem> {
        val items = mutableListOf<RecommendationItem>()
        try {
            val factory = Xml.newPullParser()
            factory.setInput(StringReader(RECOMMENDATIONS_XML))

            var eventType = factory.eventType
            var currentCategory = ""
            var currentTitle = ""
            var currentUrl = ""
            var currentDescription = ""
            var currentTagName = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTagName = factory.name
                        if (currentTagName == "item") {
                            currentCategory = factory.getAttributeValue(null, "category") ?: "general"
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = factory.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTagName) {
                                "title" -> currentTitle = text
                                "url" -> currentUrl = text
                                "description" -> currentDescription = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (factory.name == "item") {
                            items.add(
                                RecommendationItem(
                                    category = currentCategory,
                                    title = currentTitle,
                                    url = currentUrl,
                                    description = currentDescription
                                )
                            )
                            currentCategory = ""
                            currentTitle = ""
                            currentUrl = ""
                            currentDescription = ""
                        }
                        currentTagName = ""
                    }
                }
                eventType = factory.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback hardcoded if parsing fails
            return listOf(
                RecommendationItem(
                    "privacy",
                    "[Privacy] Zero-Knowledge Architectures 101",
                    "https://war.dev/privacy/zero-knowledge",
                    "Discover why local-only client-side encryption is the future."
                ),
                RecommendationItem(
                    "security",
                    "[Security] AES-256 GCM Cryptography",
                    "https://war.dev/security/aes-gcm",
                    "An analysis of securing client data against third-party injection."
                )
            )
        }
        return items
    }

    /**
     * Recommends items based on local user categories activity scores.
     * We tally the local history categories, and sort items matching higher activity categories.
     */
    fun getRecommendationsForUser(historyCategories: Map<String, Int>): List<RecommendationItem> {
        val allItems = parseRecommendations()
        
        // Sort categories by visit counts descending
        val sortedCategories = historyCategories.entries
            .sortedByDescending { it.value }
            .map { it.key }

        // Sort items: items that fall into preferred categories appear first, sorted by preferred category rank
        return allItems.sortedWith(Comparator { a, b ->
            val indexA = sortedCategories.indexOf(a.category)
            val indexB = sortedCategories.indexOf(b.category)
            
            val valA = if (indexA >= 0) indexA else Int.MAX_VALUE
            val valB = if (indexB >= 0) indexB else Int.MAX_VALUE
            
            valA.compareTo(valB)
        })
    }
}
