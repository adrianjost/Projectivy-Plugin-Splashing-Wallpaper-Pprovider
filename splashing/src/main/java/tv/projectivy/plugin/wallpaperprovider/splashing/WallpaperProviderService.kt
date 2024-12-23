package tv.projectivy.plugin.wallpaperprovider.splashing

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.json.JSONArray
import tv.projectivy.plugin.wallpaperprovider.api.Event
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType
import java.net.HttpURLConnection
import java.net.URL

const val unsplashAccessKey = "YOUR_ACCESS_KEY"

class WallpaperProviderService : Service() {

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.init(this)
    }

    override fun onBind(intent: Intent): IBinder {
        // Return the interface.
        return binder
    }


    private val binder = object : IWallpaperProviderService.Stub() {
        override fun getWallpapers(event: Event?): List<Wallpaper> {
            return when (event) {
                is Event.TimeElapsed -> {
                    Log.d("WallpaperProviderService", "Event.TimeElapsed")
                    return fetchWallpapers()
                }

                // Below are "dynamic events" that might interest you in special cases
                // You will only receive dynamic events depending on the updateMode declared in your manifest
                // Don't subscribe if not interested :
                //  - this will consume device resources unnecessarily
                //  - some cache optimizations won't be enabled for dynamic wallpaper providers

                // When "now playing" changes (ex: a song starts or stops)
                is Event.NowPlayingChanged -> emptyList()
                // When the focused card changes
                is Event.CardFocused -> emptyList()
                // When the focused "program" card changes
                is Event.ProgramCardFocused -> emptyList()
                // When Projectivy enters or exits idle mode
                is Event.LauncherIdleModeChanged -> emptyList()
//                {
//                    return if (event.isIdle) { listOf(Wallpaper(getDrawableUri(R.drawable.ic_plugin).toString(), WallpaperType.DRAWABLE)) }
//                        else
//                }
                else -> emptyList()  // Returning an empty list won't change the currently displayed wallpaper
            }
        }

        override fun getPreferences(): String {
            return PreferencesManager.export()
        }

        override fun setPreferences(params: String) {
            PreferencesManager.import(params)
        }

        private fun fetchWallpapers(): List<Wallpaper> {
            return try {
                return when (PreferencesManager.mode) {
                    Mode.COLLECTION.name -> {
                        return fetchWallpapersFromUnsplash(
                            mapOf(
                                "collections" to PreferencesManager.collectionID,
                            )
                        )
                    }

                    Mode.RANDOM.name -> {
                        return fetchWallpapersFromUnsplash(mapOf())
                    }

                    Mode.SEARCH.name -> {
                        return fetchWallpapersFromUnsplash(
                            mapOf(
                                "query" to PreferencesManager.searchTerm,
                            )
                        )
                    }

                    else -> {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        private fun appendQueryParameters(url: String, queryParams: Map<String, String>): String {
            val builder = StringBuilder(url)
            if (!url.contains("?")) {
                builder.append("?")
            } else {
                builder.append("&")
            }
            for ((key, value) in queryParams) {
                builder.append("$key=$value&")
            }
            builder.deleteCharAt(builder.length - 1)
            return builder.toString()
        }

        private fun fetchWallpapersFromUnsplash(
            queryOptions: Map<String, String>,
        ): List<Wallpaper> {
            Log.d("WallpaperProviderService", "Fetching wallpapers with query: $queryOptions")
            val imagesQueryMap = queryOptions.toMutableMap()
            imagesQueryMap["count"] = R.integer.images_per_request.toString()
            imagesQueryMap["content_filter"] = "high"
            imagesQueryMap["orientation"] = "landscape"
            val url =
                URL(appendQueryParameters("https://api.unsplash.com/photos/random", imagesQueryMap))
            val connection = url.openConnection() as HttpURLConnection

            try {
                // Set up the connection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Client-ID $unsplashAccessKey")
                connection.setRequestProperty("Accept-Version", "v1")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Read the response
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    return parseWallpapers(response).shuffled()
                } else {
                    throw Exception("HTTP error code: ${connection.responseCode} - $url")
                }
            } finally {
                connection.disconnect()
            }
        }

        private fun parseWallpapers(jsonResponse: String): List<Wallpaper> {
            val jsonArray = JSONArray(jsonResponse)
            val wallpapers = mutableListOf<Wallpaper>()

            for (i in 0 until jsonArray.length()) {
                val photo = jsonArray.getJSONObject(i)

                val user = photo.getJSONObject("user")
                val author = user.getString("name")

                val urls = photo.getJSONObject("urls")
                val rawUrl = urls.getString("raw")

                val links = photo.getJSONObject("links")
                val source = links.getString("html")

                val description = photo.getString("description")

                val imageOptions = mapOf(
                    "q" to "90",
                    "fm" to "jpg",
                    "h" to "2160",
                    "w" to "3840",
                    "dpr" to "1",
                    "fit" to "clip"
                )
                val sourceTrackingParameters = mapOf(
                    "utm_source" to getString(R.string.setting_collection_id_title),
                    "utm_medium" to "referral"
                )
                wallpapers.add(
                    Wallpaper(
                        uri = appendQueryParameters(rawUrl, imageOptions),
                        type = WallpaperType.IMAGE,
                        author = author,
                        title = description,
                        source = appendQueryParameters(source, sourceTrackingParameters),
                    )
                )
            }
            return wallpapers
        }
    }
}
