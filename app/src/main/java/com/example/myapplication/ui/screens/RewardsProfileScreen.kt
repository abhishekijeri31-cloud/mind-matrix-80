package com.example.myapplication.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import com.example.myapplication.ui.viewmodel.AuthViewModel // <--- ADD THIS LINE
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.EcoRank
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.components.SectionHeader
import com.example.myapplication.ui.theme.EcoGradientEnd
import com.example.myapplication.ui.theme.EcoGradientStart
import com.example.myapplication.ui.theme.SoftBlue
import com.example.myapplication.ui.viewmodel.UserViewModel

@Composable
fun RewardsProfileScreen(viewModel: UserViewModel , authViewModel: AuthViewModel)
{
    val user by viewModel.userData.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            ProfileHeader(user)
        }

        item {
            user?.let { RankProgressCard(it) }
        }

        item {
            SectionHeader(title = "Environmental Impact")
            ImpactStatsGrid(user)
        }

        item {
            SectionHeader(title = "Motivational Badges")
            AchievementGrid()
        }

        item {
            user?.let { MotivationalMessageCard(it.rank) }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileHeader(user: User?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user?.name ?: "Eco Hero",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = user?.rank?.displayName ?: "Eco Beginner",
            style = MaterialTheme.typography.titleMedium,
            color = EcoGradientStart,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RankProgressCard(user: User) {
    val rank = user.rank
    val nextRank = EcoRank.entries.getOrNull(rank.ordinal + 1)
    val progress = if (rank.nextRankPoints != null) {
        (user.ecoPoints - rank.minPoints).toFloat() / (rank.nextRankPoints - rank.minPoints).toFloat()
    } else 1f

    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${user.ecoPoints} pts", color = EcoGradientStart, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = EcoGradientStart,
                trackColor = MaterialTheme.colorScheme.surface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(rank.displayName, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                nextRank?.let {
                    Text(it.displayName, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MotivationalMessageCard(rank: EcoRank) {
    val message = when(rank) {
        EcoRank.BEGINNER -> "Every great journey starts with a single report. Welcome to the mission!"
        EcoRank.WATCHER -> "You're noticing what others ignore. Keep a sharp eye out!"
        EcoRank.GUARDIAN -> "Your community is becoming cleaner because of you. Thank you, Guardian!"
        EcoRank.PROTECTOR -> "You are a true shield for Mother Earth. Your impact is undeniable."
        EcoRank.WARRIOR -> "Heroic effort! You've cleared massive amounts of waste. Almost a savior!"
        EcoRank.SAVIOR -> "You are a legend! This city owes its cleanliness to your dedication."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EcoGradientStart.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EcoGradientStart)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun ImpactStatsGrid(user: User?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            label = "Reports",
            value = user?.reportsSubmitted?.toString() ?: "0",
            icon = Icons.Default.Description,
            modifier = Modifier.weight(1f),
            color = SoftBlue
        )
        StatCard(
            label = "Cleanups",
            value = user?.verifiedCleanups?.toString() ?: "0",
            icon = Icons.Default.Verified,
            modifier = Modifier.weight(1f),
            color = EcoGradientStart
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
    }
}

@Composable
fun AchievementGrid() {
    val achievements = listOf(
        Triple("First Report", Icons.Default.EmojiEvents, EcoGradientStart),
        Triple("Cleanup Hero", Icons.Default.VerifiedUser, SoftBlue),
        Triple("Top Reporter", Icons.Default.Stars, Color(0xFFFFD700)),
        Triple("Earth Friend", Icons.Default.Eco, Color(0xFF4CAF50))
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        achievements.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (name, icon, color) ->
                    AchievementCard(name, icon, color, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AchievementCard(name: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
