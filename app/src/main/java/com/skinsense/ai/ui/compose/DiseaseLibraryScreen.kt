package com.skinsense.ai.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.KeyboardArrowRight


import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.*

/**
 * Disease data model
 */
data class Disease(
    val id: String,
    val name: String,
    val category: String,
    val aliases: List<String> = emptyList(),
    val description: String = "",
    val symptoms: List<String> = emptyList(),
    val causes: List<String> = emptyList(),
    val riskFactors: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList()
)

// Helper for UI Theme
data class DiseaseTheme(
    val backgroundColor: Color,
    val icon: ImageVector,
    val iconColor: Color
)

fun getDiseaseTheme(diseaseId: String): DiseaseTheme {
    return when (diseaseId) {
        "acne" -> DiseaseTheme(Color(0xFFE0F2FE), Icons.Default.BubbleChart, Color(0xFF60A5FA)) // Light Blue
        "skin_cancer" -> DiseaseTheme(Color(0xFFFFEDD5), Icons.Default.Apps, Color(0xFFF87171)) // Light Orange/Red
        "psoriasis" -> DiseaseTheme(Color(0xFFFEF9C3), Icons.Default.Layers, Color(0xFFFBBF24)) // Light Yellow
        "eczema" -> DiseaseTheme(Color(0xFFE0F2FE), Icons.Default.Grain, Color(0xFF60A5FA)) // Light Blue
        "rosacea" -> DiseaseTheme(Color(0xFFFCE7F3), Icons.Default.FilterVintage, Color(0xFFF472B6)) // Light Pink
        "actinic_keratosis" -> DiseaseTheme(Color(0xFFFEF3C7), Icons.Default.Texture, Color(0xFFFBBF24)) // Light Orange
        "candidiasis" -> DiseaseTheme(Color(0xFFDCFCE7), Icons.Default.BugReport, Color(0xFF4ADE80)) // Light Green
        "vitiligo" -> DiseaseTheme(Color(0xFFF1F5F9), Icons.Default.InvertColors, Color(0xFF94A3B8)) // Light Grey
        "warts" -> DiseaseTheme(Color(0xFFCCFBF1), Icons.Default.ScatterPlot, Color(0xFF2DD4BF)) // Light Teal
        "benign_tumors" -> DiseaseTheme(Color(0xFFF3E8FF), Icons.Default.RadioButtonChecked, Color(0xFFA78BFA)) // Light Purple
        "seborrheic_keratoses" -> DiseaseTheme(Color(0xFFF1F5F9), Icons.Default.Settings, Color(0xFF94A3B8)) // Light Grey
        else -> DiseaseTheme(Color(0xFFF1F5F9), Icons.Default.Info, Color(0xFF94A3B8)) // Default Grey
    }
}

/**
 * Disease Library Screen
 * Searchable grid of diseases with risk badges
 */
@Composable
fun DiseaseLibraryScreen(
    diseases: List<Disease>,
    onDiseaseClick: (Disease) -> Unit,
    onBack: () -> Unit = {} // Added support for back navigation if needed in future
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    // Filter diseases based on search query and category
    val filteredDiseases = remember(searchQuery, selectedCategory, diseases) {
        val searchFiltered = if (searchQuery.isEmpty()) {
            diseases
        } else {
            diseases.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
        
        if (selectedCategory == "All") {
            searchFiltered
        } else {
            searchFiltered.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Clean white background
            .padding(16.dp)
    ) {
        // Header
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600))
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Optional Back Button (If implemented in navigation)
                         // Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                         // Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.nav_library),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.results_insight),
                        tint = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.library_count_label, diseases.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 200))
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.find_doctor_search_hint),
                modifier = Modifier.border(1.dp, MedicalSecondary.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Filters
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 300))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Common", "Rare").forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(stringResource(if (category == "All") R.string.all else if (category == "Common") R.string.common else R.string.rare)) },
                        shape = MaterialTheme.shapes.medium, // 18.dp
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MedicalSecondary,
                            selectedLabelColor = Color.White,
                            containerColor = MedicalBackground
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MedicalSecondary else MedicalSecondary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
        
        // Disease List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDiseases) { disease ->
                DiseaseListItem(
                    disease = disease,
                    onClick = { onDiseaseClick(disease) }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

/**
 * Disease List Item Component
 * Accordion-style list item with Risk Level and Chevron
 */
@Composable
fun DiseaseListItem(
    disease: Disease,
    onClick: () -> Unit
) {
    val theme = getDiseaseTheme(disease.id)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Disease Name
            Text(
                text = disease.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        // Chevron Icon
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = stringResource(R.string.common_ok),
            tint = TextTertiary
        )
    }
}

/**
 * Disease Detail Modal
 * Bottom sheet showing detailed disease information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDetailModal(
    disease: Disease,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BackgroundCard,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = disease.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Aliases
            if (disease.aliases.isNotEmpty()) {
                DetailSection(
                    title = stringResource(R.string.aliases),
                    content = disease.aliases.joinToString(", ")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Description
            if (disease.description.isNotEmpty()) {
                DetailSection(
                    title = stringResource(R.string.results_insight),
                    content = disease.description
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Symptoms
            if (disease.symptoms.isNotEmpty()) {
                DetailSection(
                    title = stringResource(R.string.results_symptoms),
                    items = disease.symptoms
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Causes
            if (disease.causes.isNotEmpty()) {
                DetailSection(
                    title = stringResource(R.string.causes),
                    items = disease.causes
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Risk Factors
            if (disease.riskFactors.isNotEmpty()) {
                DetailSection(
                    title = stringResource(R.string.risk_factors),
                    items = disease.riskFactors
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Recommended Actions
            if (disease.recommendedActions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MedicalBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recommended_actions),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        disease.recommendedActions.forEach { action ->
                            Text(
                                text = "• $action",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Close Button
            PrimaryButton(
                text = stringResource(R.string.common_ok),
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Detail Section Component
 * Helper for displaying disease detail sections
 */
@Composable
fun DetailSection(
    title: String,
    content: String? = null,
    items: List<String>? = null
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        content?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        
        items?.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
