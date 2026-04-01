package edu.nd.pmcburne.hello

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

@Serializable
data class VisualCenter(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class Placemark(
    val id: Int,
    val name: String,
    @SerialName("tag_list") val tagList: List<String>,
    val description: String,
    @SerialName("visual_center") val visualCenter: VisualCenter
)

data class Location(
    val name: String,
    val description: String,
    val tags: List<String>,
    val visualCenter: LatLng
)

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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CampusMapsUiState())
    val uiState: StateFlow<CampusMapsUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application)
    private val locationDao = database.locationDao()

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.cs.virginia.edu/~wxt4gm/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(PlacemarkApi::class.java)

    init {
        // 1. Observe the database as the single source of truth
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
                
                // Get unique tags from locations, ensure "Core" is included, and sort alphabetically
                val uniqueTags = (locations.flatMap { it.tags } + "Core").distinct().sorted()
                
                _uiState.update { 
                    it.copy(
                        locations = locations,
                        tags = uniqueTags
                    ) 
                }
            }
        }

        // 2. Synchronize API data into the database on start-up
        syncWithApi()
    }

    private fun formatTag(tag: String): String {
        return tag.replace("_", " ")
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
    }

    private fun syncWithApi() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val placemarks = api.getPlacemarks()
                val entities = placemarks.map { placemark ->
                    LocationEntity(
                        id = placemark.id,
                        name = placemark.name,
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

    fun onTagSelected(tag: String) {
        _uiState.update { it.copy(selectedTag = tag, isDropdownExpanded = false) }
    }

    fun onDropdownExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isDropdownExpanded = expanded) }
    }
}
