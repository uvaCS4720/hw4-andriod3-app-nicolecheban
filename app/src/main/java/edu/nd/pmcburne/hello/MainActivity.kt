package edu.nd.pmcburne.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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

    // UVA Rotunda Coordinates
    val rotunda = LatLng(38.03567, -78.50365)
    val rotundaMarkerState = rememberMarkerState(position = rotunda)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(rotunda, 15f)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tag Selection Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.selectedTag,
                onValueChange = { },
                readOnly = true,
                label = { Text("Filter by Tag") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        "Dropdown",
                        Modifier.clickable { viewModel.onDropdownExpandedChange(true) }
                    )
                },
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onDropdownExpandedChange(true) }
            )
            DropdownMenu(
                expanded = uiState.isDropdownExpanded,
                onDismissRequest = { viewModel.onDropdownExpandedChange(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                uiState.tags.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(tag) },
                        onClick = { viewModel.onTagSelected(tag) }
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
                val title = "The Rotunda"
                val description = "The Rotunda is the symbol of the University of Virginia, designed by Thomas Jefferson and modeled after the Pantheon in Rome. Completed in 1826, it anchors the north end of the Academical Village and houses the University's library and ceremonial spaces."

                MarkerInfoWindowContent(
                    state = rotundaMarkerState,
                    title = title
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .widthIn(max = 200.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
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
        CampusMapsScreen(viewModel = MainViewModel())
    }
}