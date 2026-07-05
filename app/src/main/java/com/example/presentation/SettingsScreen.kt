package com.example.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProviderType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SharedViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isKeyConfigured = com.example.BuildConfig.GEMINI_API_KEY.isNotBlank() && 
            com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model & Studio Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArtisticBackground
                )
            )
        },
        containerColor = ArtisticBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ArtisticBackground)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // API Key Status Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isKeyConfigured) Color(0xFF0D2418) else Color(0xFF2C1414)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = if (isKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = if (isKeyConfigured) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (isKeyConfigured) "Gemini API Key: Configured" else "Gemini API Key: Missing",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isKeyConfigured) 
                                "Secure connection established with Google Generative Language Services." 
                                else "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel. Running in Mock fallback mode.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Provider Configuration Section
            Text(
                text = "STUDIO GENERATIVE PROVIDER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ArtisticViolet
                )
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ArtisticSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mock Offline Studio", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Fast, 100% offline Canvas filtering", color = Color.Gray, fontSize = 12.sp)
                        }
                        RadioButton(
                            selected = viewModel.providerType == ProviderType.MOCK,
                            onClick = { viewModel.updateProvider(ProviderType.MOCK) },
                            colors = RadioButtonDefaults.colors(selectedColor = ArtisticViolet),
                            modifier = Modifier.testTag("provider_mock_radio")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Direct Gemini API Prototyping", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Connects online to Google Generative AI REST servers", color = Color.Gray, fontSize = 12.sp)
                        }
                        RadioButton(
                            selected = viewModel.providerType == ProviderType.GEMINI,
                            onClick = { viewModel.updateProvider(ProviderType.GEMINI) },
                            colors = RadioButtonDefaults.colors(selectedColor = ArtisticViolet),
                            modifier = Modifier.testTag("provider_gemini_radio")
                        )
                    }
                }
            }

            // Model choices (Visible if Gemini is selected)
            if (viewModel.providerType == ProviderType.GEMINI) {
                Text(
                    text = "SELECT REASONING MODEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ArtisticViolet
                    )
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtisticSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("gemini-2.5-flash-image", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Fastest balanced performance model", color = Color.Gray, fontSize = 12.sp)
                            }
                            RadioButton(
                                selected = viewModel.selectedModel == "gemini-2.5-flash-image",
                                onClick = { viewModel.updateModel("gemini-2.5-flash-image") },
                                colors = RadioButtonDefaults.colors(selectedColor = ArtisticViolet)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("gemini-3.1-flash-image-preview", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("High fidelity previews (supports up to 4K)", color = Color.Gray, fontSize = 12.sp)
                            }
                            RadioButton(
                                selected = viewModel.selectedModel == "gemini-3.1-flash-image-preview",
                                onClick = { viewModel.updateModel("gemini-3.1-flash-image-preview") },
                                colors = RadioButtonDefaults.colors(selectedColor = ArtisticViolet)
                            )
                        }
                    }
                }
            }

            // Remote Configurations (Admin view read-only)
            Text(
                text = "ADMINISTRATIVE CONFIGURATIONS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ArtisticSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminRow(label = "Maintenance Mode Status", value = "Disabled (Live)")
                    AdminRow(label = "Maximum Image Upload Limit", value = "10 MB")
                    AdminRow(label = "Supported Export Formats", value = "PNG, JPG, WEBP")
                    AdminRow(label = "Server Endpoint Host", value = "https://generativelanguage.googleapis.com/")
                    AdminRow(label = "Client Cache TTL", value = "24 Hours")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security warning
            Text(
                text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )
            )
        }
    }
}

@Composable
fun AdminRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
    }
}
