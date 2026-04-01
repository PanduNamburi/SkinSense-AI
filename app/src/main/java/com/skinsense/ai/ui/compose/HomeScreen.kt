package com.skinsense.ai.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinsense.ai.R
import com.skinsense.ai.ui.theme.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    onStartAnalysis: () -> Unit,
    onDiseasesClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Scaffold(
        containerColor = MedicalBackground,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hero Section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + expandVertically(animationSpec = tween(800))
            ) {
                PremiumHeroSection(onStartAnalysis = onStartAnalysis)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Awareness Carousel
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 200)) + slideInHorizontally()
            ) {
                AwarenessCarousel()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Grid
            StatsGrid(visible = visible)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Privacy Section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 400))
            ) {
                PremiumPrivacySection()
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB
            
            // Footer
            FooterSection()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PremiumHeroSection(onStartAnalysis: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MedicalPrimary, // Deep Navy Background
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.hero_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onStartAnalysis,
                colors = ButtonDefaults.buttonColors(containerColor = MedicalSecondary),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cta_start_analysis), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatsGrid(visible: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = stringResource(R.string.trust_reliability),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItem(
                icon = Icons.Default.Verified,
                value = stringResource(R.string.stat_accuracy_value),
                label = stringResource(R.string.stat_accuracy_label),
                visible = visible,
                delay = 300,
                modifier = Modifier.weight(1f)
            )
            StatItem(
                icon = Icons.AutoMirrored.Filled.List,
                value = stringResource(R.string.stat_conditions_value),
                label = stringResource(R.string.stat_conditions_label),
                visible = visible,
                delay = 450,
                modifier = Modifier.weight(1f)
            )
            StatItem(
                icon = Icons.Default.Timer,
                value = stringResource(R.string.stat_time_value),
                label = stringResource(R.string.stat_time_label),
                visible = visible,
                delay = 600,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    visible: Boolean,
    delay: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800, delayMillis = delay)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MedicalSurface,
            border = borderLight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MedicalSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PremiumPrivacySection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.large,
        color = MedicalSecondary.copy(alpha = 0.05f),
        border = borderLight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MedicalSecondary,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.secure_confidential),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.privacy_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextMuted,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_branding_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.footer_branding),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.footer_copyright),
            style = MaterialTheme.typography.labelSmall,
            color = TextLight
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AwarenessCarousel() {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { awarenessSlides.size })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.weight(1f)
        ) { page ->
            val slide = awarenessSlides[page]
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState
                                .currentPageOffsetFraction
                        ).absoluteValue
                        
                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = slide.imageResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(slide.titleResId),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(slide.descriptionResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(awarenessSlides.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MedicalSecondary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

data class AwarenessSlide(
    val titleResId: Int,
    val descriptionResId: Int,
    val imageResId: Int
)

val awarenessSlides = listOf(
    AwarenessSlide(
        titleResId = R.string.slide1_title,
        descriptionResId = R.string.slide1_desc,
        imageResId = R.drawable.awareness_detection
    ),
    AwarenessSlide(
        titleResId = R.string.slide2_title,
        descriptionResId = R.string.slide2_desc,
        imageResId = R.drawable.awareness_sun
    ),
    AwarenessSlide(
        titleResId = R.string.slide3_title,
        descriptionResId = R.string.slide3_desc,
        imageResId = R.drawable.awareness_skin
    )
)
