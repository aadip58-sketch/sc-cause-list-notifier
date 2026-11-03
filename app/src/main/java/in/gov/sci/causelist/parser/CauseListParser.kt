package in.gov.sci.causelist.parser

import in.gov.sci.causelist.model.CauseListItem
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CauseListParser {

    private const val SCI_CAUSE_LIST_URL = "https://www.sci.gov.in/cause-list/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val DATE_ISO_REGEX = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})")
    private val DATE_DMY_REGEX = Pattern.compile("(\\d{2}-\\d{2}-\\d{4})")

    /**
     * Fetches and parses the latest cause lists from the Supreme Court website.
     */
    fun fetchCauseLists(): List<CauseListItem> {
        val request = Request.Builder()
            .url(SCI_CAUSE_LIST_URL)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch cause lists: HTTP ${response.code}")
        }

        val html = response.body?.string() ?: return emptyList()
        return parseHtml(html)
    }

    /**
     * Parses the HTML string into a structured list of CauseListItems.
     */
    fun parseHtml(html: String): List<CauseListItem> {
        val document = Jsoup.parse(html)
        val links = document.select("a[href]")
        val items = mutableListOf<CauseListItem>()
        val seenUrls = mutableSetOf<String>()

        for (link in links) {
            val href = link.attr("abs:href").ifEmpty { link.attr("href") }
            if (!href.contains(".pdf", ignoreCase = true) || !href.contains("sci.gov.in", ignoreCase = true)) {
                continue
            }
            if (seenUrls.contains(href)) {
                continue
            }
            seenUrls.add(href)

            val linkText = link.text().trim()
            val filename = href.substringAfterLast("/")

            // Extract date from URL or text
            var date = ""
            val isoMatcher = DATE_ISO_REGEX.matcher(href)
            if (isoMatcher.find()) {
                date = isoMatcher.group(1) ?: ""
            } else {
                val dmyMatcher = DATE_DMY_REGEX.matcher(href)
                if (dmyMatcher.find()) {
                    date = dmyMatcher.group(1) ?: ""
                } else if (linkText.isNotEmpty()) {
                    val textMatcher = DATE_DMY_REGEX.matcher(linkText)
                    if (textMatcher.find()) {
                        date = textMatcher.group(1) ?: ""
                    }
                }
            }

            // Determine category and title
            val (category, title) = determineCategoryAndTitle(href, filename)

            // Court context
            val court = determineCourtContext(link, href)

            val id = hashString(href)

            items.add(
                CauseListItem(
                    id = id,
                    date = date.ifEmpty { "Latest" },
                    title = title,
                    court = court,
                    category = category,
                    pdfUrl = href
                )
            )
        }

        // Sort: newest dates and Advance/Main first
        return items.sortedWith(
            compareByDescending<CauseListItem> { it.date }
                .thenBy { it.category }
        )
    }

    private fun determineCategoryAndTitle(url: String, filename: String): Pair<String, String> {
        val lowerUrl = url.lowercase()
        val lowerFn = filename.lowercase()

        return when {
            lowerUrl.contains("/advance/") -> {
                Pair("ADVANCE", "Advance Cause List")
            }
            lowerFn.contains("m_j_2") -> {
                Pair("SUPPLEMENTARY", "Supplementary Cause List (Judicial)")
            }
            lowerFn.contains("m_j_1") || lowerFn.contains("m_j") -> {
                Pair("MAIN", "Main Cause List (Judicial)")
            }
            lowerFn.contains("m_r") || lowerFn.contains("r_") -> {
                Pair("REGISTRAR", "Registrar Court Cause List")
            }
            lowerFn.contains("m_c") || lowerFn.contains("c_") -> {
                Pair("CHAMBER", "Chamber Matters Cause List")
            }
            lowerFn.contains("m_s") || lowerFn.contains("s_") -> {
                Pair("SPECIAL", "Single Judge / Special List")
            }
            else -> {
                Pair("CAUSE LIST", "Supreme Court Cause List")
            }
        }
    }

    private fun determineCourtContext(element: org.jsoup.nodes.Element, url: String): String {
        // Try to check if it belongs to a table row with court number
        val row = element.closest("tr")
        if (row != null) {
            val firstCell = row.selectFirst("td, th")?.text()?.trim()
            if (!firstCell.isNullOrEmpty() && !firstCell.contains("-")) {
                return firstCell
            }
        }
        return "All Courts"
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
