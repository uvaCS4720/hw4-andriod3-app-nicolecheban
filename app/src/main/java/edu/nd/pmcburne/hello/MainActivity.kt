package edu.nd.pmcburne.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.nd.pmcburne.hello.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    "Campus Maps", 
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    CampusMapsScreen(viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CampusMapsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    CampusMapsContent(
        uiState = uiState,
        onDropdownExpandedChange = { viewModel.onDropdownExpandedChange(it) },
        onTagSelected = { viewModel.onTagSelected(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapsContent(
    uiState: CampusMapsUiState,
    onDropdownExpandedChange: (Boolean) -> Unit,
    onTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // UVA Rotunda Coordinates for initial camera position
    val rotunda = LatLng(38.03567, -78.50365)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(rotunda, 15f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tag Selection Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.selectedTag,
                    onValueChange = { },
                    readOnly = true,
                    label = { 
                        Text(
                            "Filter by Tag", 
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            "Dropdown",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                // Transparent overlay to capture clicks anywhere on the OutlinedTextField
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onDropdownExpandedChange(true)
                        }
                )
            }
            DropdownMenu(
                expanded = uiState.isDropdownExpanded,
                onDismissRequest = { onDropdownExpandedChange(false) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                uiState.tags.forEach { tag ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = tag, 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        onClick = { onTagSelected(tag) }
                    )
                }
            }
        }

        // Google Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Filter locations based on the selected tag
                val filteredLocations = uiState.locations.filter { location ->
                    location.tags.any { it.equals(uiState.selectedTag, ignoreCase = true) }
                }

                filteredLocations.forEach { location ->
                    MarkerInfoWindowContent(
                        state = rememberMarkerState(position = location.visualCenter),
                        title = location.name
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .widthIn(max = 240.dp)
                            ) {
                                Text(
                                    text = location.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = location.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CampusMapsPreview() {
    MyApplicationTheme {
        CampusMapsContent(
            uiState = CampusMapsUiState(
                selectedTag = "Landmark",
                locations = listOf(
                    Location(
                        name = "Rotunda",
                        description = "The Rotunda is a building located on The Lawn on the Central Grounds of the University of Virginia.",
                        tags = listOf("Landmark"),
                        visualCenter = LatLng(38.03567, -78.50365)
                    )
                ),
                tags = listOf("Core", "Landmark")
            ),
            onDropdownExpandedChange = {},
            onTagSelected = {}
        )
    }
}
