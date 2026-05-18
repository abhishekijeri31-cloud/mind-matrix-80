package com.example.myapplication.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.EcoGradientEnd
import com.example.myapplication.ui.theme.EcoGradientStart
import com.example.myapplication.ui.theme.SoftBlue
import com.example.myapplication.ui.viewmodel.AuthViewModel
import com.example.myapplication.ui.viewmodel.ReportState
import com.example.myapplication.ui.viewmodel.ReportViewModel
import com.example.myapplication.utils.ComposeFileProvider
import com.example.myapplication.utils.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportWasteScreen(viewModel: ReportViewModel, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var description by remember { mutableStateOf("") }
    var wasteType by remember { mutableStateOf("Select Waste Type") }
    var showDropdown by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var location by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var addressText by remember { mutableStateOf("Tap to fetch location") }

    var showImageSourceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val reportState by viewModel.reportState.collectAsState()
    val currentUser = authViewModel.currentUser

    // launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
        showImageSourceSheet = false
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempCameraUri
        }
        showImageSourceSheet = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    location = Pair(loc.latitude, loc.longitude)
                    addressText = "Lat: ${String.format("%.4f", loc.latitude)}, Lng: ${String.format("%.4f", loc.longitude)}"
                }
            }
        }
    }

    LaunchedEffect(reportState) {
        if (reportState is ReportState.Success) {
            description = ""
            wasteType = "Select Waste Type"
            imageUri = null
            viewModel.resetState()
            Toast.makeText(context, "Report Submitted!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Report Illegal Dumping", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

        // Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(2.dp, Brush.linearGradient(listOf(EcoGradientStart, EcoGradientEnd)), RoundedCornerShape(24.dp))
                .clickable { showImageSourceSheet = true },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp), tint = EcoGradientStart)
                    Text("Add Proof Photo", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Location Card
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = EcoGradientStart)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Location", style = MaterialTheme.typography.labelSmall)
                    Text(addressText, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Waste Dropdown
        OutlinedTextField(
            value = wasteType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Waste Type") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = { showDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
        )
        DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
            listOf("Plastic", "Organic", "Electronic", "Hazardous").forEach { type ->
                DropdownMenuItem(text = { Text(type) }, onClick = { wasteType = type; showDropdown = false })
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Button(
            onClick = {
                if (imageUri != null && location != null) {
                    viewModel.submitReport(context, currentUser?.uid ?: "", imageUri!!, wasteType, location!!.first, location!!.second, description)
                } else {
                    Toast.makeText(context, "Please add photo and location", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EcoGradientStart)
        ) {
            if (reportState is ReportState.Loading) CircularProgressIndicator(color = Color.White)
            else Text("Submit Report")
        }
    }

    if (showImageSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showImageSourceSheet = false }) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ListItem(
                    headlineContent = { Text("Take Photo") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                    modifier = Modifier.clickable {
                        val uri = ComposeFileProvider.getImageUri(context)
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                )
                ListItem(
                    headlineContent = { Text("Choose from Gallery") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                )
            }
        }
    }
}
