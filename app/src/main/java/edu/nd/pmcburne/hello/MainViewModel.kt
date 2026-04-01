package edu.nd.pmcburne.hello

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import edu.nd.pmcburne.hello.data.AppDatabase
import edu.nd.pmcburne.hello.data.LocationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class VisualCenter(
    val latitude: Double,
    val longitude: Double
)

// Raw data structure returned by Placemark API

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Placemark(
    val id: Int,
    val name: String,
    @SerialName("tag_list") val tagList: List<String>,
    val description: String,
    @SerialName("visual_center") val visualCenter: VisualCenter
)

// Domain model used by the UI
data class Location(
    val name: String,
    val description: String,
    val tags: List<String>,
    val visualCenter: LatLng
)

// State of the Campus Maps screen.

data class CampusMapsUiState(
    val selectedTag: String = "Core",
    val tags: List<String> = listOf("Core"),
    val isDropdownExpanded: Boolean = false,
    val locations: List<Location> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

interface PlacemarkApi {
    @GET("placemarks.json")
    suspend fun getPlacemarks(): List<Placemark>
}

// Business logic handler for the Campus Maps application.
// Extends AndroidViewModel to access the Application context
class MainViewModel(application: Application) : AndroidViewModel(application) {
    // Single Source of Truth for the UI State
    private val _uiState = MutableStateFlow(CampusMapsUiState())
    val uiState: StateFlow<CampusMapsUiState> = _uiState.asStateFlow()

    // Database access
    private val database = AppDatabase.getDatabase(application)
    private val locationDao = database.locationDao()

    // JSON configuration for Retrofit
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    // Networking setup with Retrofit
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.cs.virginia.edu/~wxt4gm/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(PlacemarkApi::class.java)

    init {
        // Observe local DB as SST - any change updates UI automatically
        viewModelScope.launch {
            locationDao.getAllLocations().collectLatest { entities ->
                val locations = entities.map { entity ->
                    Location(
                        name = entity.name,
                        description = entity.description,
                        tags = entity.tags.map { formatTag(it) },
                        visualCenter = LatLng(entity.latitude, entity.longitude)
                    )
                }
                
                // Extract all unique tags, format them, and sort alphabetically
                val uniqueTags = (locations.flatMap { it.tags } + "Core").distinct().sorted()
                
                _uiState.update { 
                    it.copy(
                        locations = locations,
                        tags = uniqueTags
                    ) 
                }
            }
        }

        // Synchronize API data into the database on start-up
        syncWithApi()
    }

    // Convert raw API tags into user-friendly display tags
    private fun formatTag(tag: String): String {
        return tag.replace("_", " ") // replace _ with spaces
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } } // capitalize first letter
    }

    // Fetch new data from API and save it into local Room database
    private fun syncWithApi() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val placemarks = api.getPlacemarks()
                val entities = placemarks.map { placemark ->
                    LocationEntity(
                        id = placemark.id,
                        name = placemark.name,
                        // Basic HTML/Entity decoding for API descriptions
                        description = placemark.description
                            .replace("&code;", "")
                            .replace("&apos;", "'")
                            .replace("&quot;", "\"")
                            .replace("&amp;", "&"),
                        latitude = placemark.visualCenter.latitude,
                        longitude = placemark.visualCenter.longitude,
                        tags = placemark.tagList.map { it.lowercase() }
                    )
                }
                
                // insertAll uses REPLACE strategy, so it updates existing entries
                locationDao.insertAll(entities)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Sync failed: ${e.message}") }
            }
        }
    }

    // UI event handlers

    fun onTagSelected(tag: String) {
        _uiState.update { it.copy(selectedTag = tag, isDropdownExpanded = false) }
    }

    fun onDropdownExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isDropdownExpanded = expanded) }
    }
}
