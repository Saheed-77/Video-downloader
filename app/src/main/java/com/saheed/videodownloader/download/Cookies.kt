package com.saheed.videodownloader.download

import android.content.Context
import android.webkit.CookieManager
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

/** A site you can sign in to (Settings → Accounts), so login-only posts can be downloaded. */
enum class LoginSite(
    val label: String,
    val loginUrl: String,
    /** Where to read the cookies from, and their domain in cookies.txt. */
    val cookieUrl: String,
    val domain: String,
    /** The cookie that only exists once you're signed in. */
    val sessionCookie: String,
    val hosts: List<String>,
) {
    INSTAGRAM(
        "Instagram", "https://www.instagram.com/accounts/login/", "https://www.instagram.com",
        ".instagram.com", "sessionid", listOf("instagram.com"),
    ),
    FACEBOOK(
        "Facebook", "https://m.facebook.com/login/", "https://www.facebook.com",
        ".facebook.com", "c_user", listOf("facebook.com", "fb.watch"),
    );

    val fileName get() = "cookies-${name.lowercase()}.txt"
}

/**
 * Keeps the sign-in *cookies* (the small tokens a site uses to remember you're logged in) for
 * Instagram and Facebook, in the "Netscape cookies.txt" format that yt-dlp's `--cookies` reads.
 * They're stored in the app's private storage; the app never sees the password.
 */
object Cookies {

    /** The site a link belongs to, or null if it isn't one you can sign in to. */
    fun siteFor(url: String): LoginSite? {
        val host = try { URI(url).host?.lowercase() } catch (e: Exception) { null } ?: return null
        return LoginSite.entries.firstOrNull { site -> site.hosts.any { host == it || host.endsWith(".$it") } }
    }

    fun file(context: Context, site: LoginSite) = File(context.filesDir, site.fileName)

    fun isSignedIn(context: Context, site: LoginSite) = file(context, site).length() > 0

    /** The cookies file to pass to yt-dlp for [url], or null when not signed in to its site. */
    fun fileFor(context: Context, url: String): File? =
        siteFor(url)?.let { site -> file(context, site).takeIf { it.length() > 0 } }

    /**
     * Copies [site]'s cookies from the in-app browser into its cookies file.
     * Returns false (and saves nothing) if the sign-in hasn't finished yet.
     */
    fun save(context: Context, site: LoginSite): Boolean {
        val cookies = parseCookieHeader(CookieManager.getInstance().getCookie(site.cookieUrl))
        if (cookies.none { it.first == site.sessionCookie }) return false
        // cookies.txt needs an expiry date; the browser doesn't say, so use a year from now.
        val expires = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + TimeUnit.DAYS.toSeconds(365)
        file(context, site).writeText(toNetscape(site.domain, cookies, expires))
        return true
    }

    /** Forgets [site]: deletes the cookies file and signs the in-app browser out too. */
    fun signOut(context: Context, site: LoginSite) {
        file(context, site).delete()
        val manager = CookieManager.getInstance()
        parseCookieHeader(manager.getCookie(site.cookieUrl)).forEach { (name, _) ->
            manager.setCookie(site.cookieUrl, "$name=; Max-Age=0; Domain=${site.domain}; Path=/")
        }
        manager.flush()
    }

    /** "a=1; b=2" (the browser's cookie header) → [(a, 1), (b, 2)]. */
    fun parseCookieHeader(header: String?): List<Pair<String, String>> =
        header.orEmpty().split(';').mapNotNull { part ->
            val name = part.substringBefore('=').trim()
            if (name.isEmpty() || '=' !in part) null else name to part.substringAfter('=').trim()
        }

    /** The cookies.txt text: a header line, then one tab-separated line per cookie. */
    fun toNetscape(domain: String, cookies: List<Pair<String, String>>, expiresEpochSeconds: Long): String =
        buildString {
            append("# Netscape HTTP Cookie File\n")
            cookies.forEach { (name, value) ->
                // domain, include subdomains, path, https only, expiry, name, value
                append("$domain\tTRUE\t/\tTRUE\t$expiresEpochSeconds\t$name\t$value\n")
            }
        }
}
