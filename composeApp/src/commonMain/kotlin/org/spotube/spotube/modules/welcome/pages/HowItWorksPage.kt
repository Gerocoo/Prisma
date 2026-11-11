package org.spotube.spotube.modules.welcome.pages

import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.component.MillenniumListTile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import org.spotube.spotube.core.ui.theming.AppTheme
import com.composeunstyled.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDocumentDownload
import org.spotube.spotube.resources.iconsax.IconsaxMusic
import org.spotube.spotube.resources.iconsax.IconsaxSearch
import org.spotube.spotube.resources.iconsax.User
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_guide_lyrics_desc
import spotube.composeapp.generated.resources.welcome_guide_lyrics_title
import spotube.composeapp.generated.resources.welcome_guide_offline_desc
import spotube.composeapp.generated.resources.welcome_guide_offline_title
import spotube.composeapp.generated.resources.welcome_guide_search_desc
import spotube.composeapp.generated.resources.welcome_guide_search_title
import spotube.composeapp.generated.resources.welcome_guide_signin_desc
import spotube.composeapp.generated.resources.welcome_guide_signin_title
import spotube.composeapp.generated.resources.welcome_guide_subtitle
import spotube.composeapp.generated.resources.welcome_guide_title

/**
 * The onboarding guide: a short, concrete answer to "how do I actually use this app?".
 * No marketing prose — just the four things a new user needs to do.
 */
@Composable
fun HowItWorksPage(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.welcome_guide_title),
                style = AppTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
            Text(
                stringResource(Res.string.welcome_guide_subtitle),
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomeGuideStep(
                icon = Iconsax.User,
                title = Res.string.welcome_guide_signin_title,
                description = Res.string.welcome_guide_signin_desc)
            WelcomeGuideStep(
                icon = Iconsax.IconsaxSearch,
                title = Res.string.welcome_guide_search_title,
                description = Res.string.welcome_guide_search_desc)
            WelcomeGuideStep(
                icon = Iconsax.IconsaxDocumentDownload,
                title = Res.string.welcome_guide_offline_title,
                description = Res.string.welcome_guide_offline_desc)
            WelcomeGuideStep(
                icon = Iconsax.IconsaxMusic,
                title = Res.string.welcome_guide_lyrics_title,
                description = Res.string.welcome_guide_lyrics_desc)
        }
    }
}

/** One step as an Expressive tonal card: accent icon in a round badge, title and one line. */
@Composable
private fun WelcomeGuideStep(
    icon: ImageVector,
    title: StringResource,
    description: StringResource,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppTheme.colors.surfaceContainerHighest)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
            MillenniumIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = AppTheme.colors.onPrimary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(title),
                style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.onSurface)
            Text(
                stringResource(description),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant)
        }
    }
}
