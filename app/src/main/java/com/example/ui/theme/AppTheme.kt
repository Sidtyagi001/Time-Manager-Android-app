package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 10 Coordinated Color Themes for Time Manager:
 * 1. System Default — follows Android system light/dark mode and dynamic Material You on Android 12+
 * 2. Ocean Blue — calm and professional blue, cyan, and teal
 * 3. Aurora Purple — modern violet, indigo, and lavender
 * 4. Emerald Green — fresh emerald, mint, and deep green
 * 5. Sunset Orange — energetic orange, coral, and warm amber
 * 6. Rose Pink — colourful and elegant rose and magenta
 * 7. Midnight Neon — deep navy with electric cyan & purple neon
 * 8. Forest Dark — deep forest green with mint and emerald
 * 9. Warm Cream — warm cream surfaces, espresso text, and terracotta
 * 10. Lavender Light — soft lavender with purple and indigo
 */
enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryPreview: Color,
    val secondaryPreview: Color,
    val backgroundPreview: Color,
    val isDarkOnly: Boolean = false,
    val isLightOnly: Boolean = false
) {
    SYSTEM(
        id = "SYSTEM",
        displayName = "System Default",
        description = "Follows Android system settings and dynamic Material You colors",
        primaryPreview = Color(0xFF2563EB),
        secondaryPreview = Color(0xFF475569),
        backgroundPreview = Color(0xFFF8FAFC)
    ),
    OCEAN_BLUE(
        id = "OCEAN_BLUE",
        displayName = "Ocean Blue",
        description = "Calm and professional blue, cyan, and teal accents",
        primaryPreview = Color(0xFF0284C7),
        secondaryPreview = Color(0xFF0D9488),
        backgroundPreview = Color(0xFFF0F9FF)
    ),
    AURORA_PURPLE(
        id = "AURORA_PURPLE",
        displayName = "Aurora Purple",
        description = "Modern and premium violet, indigo, and lavender accents",
        primaryPreview = Color(0xFF7C3AED),
        secondaryPreview = Color(0xFF4F46E5),
        backgroundPreview = Color(0xFFFAF5FF)
    ),
    EMERALD_GREEN(
        id = "EMERALD_GREEN",
        displayName = "Emerald Green",
        description = "Fresh and balanced emerald, mint, and deep green accents",
        primaryPreview = Color(0xFF059669),
        secondaryPreview = Color(0xFF10B981),
        backgroundPreview = Color(0xFFF0FDF4)
    ),
    SUNSET_ORANGE(
        id = "SUNSET_ORANGE",
        displayName = "Sunset Orange",
        description = "Energetic and vibrant orange, coral, and warm amber accents",
        primaryPreview = Color(0xFFEA580C),
        secondaryPreview = Color(0xFFF97316),
        backgroundPreview = Color(0xFFFFF7ED)
    ),
    ROSE_PINK(
        id = "ROSE_PINK",
        displayName = "Rose Pink",
        description = "Colourful and elegant rose, magenta, and soft pink accents",
        primaryPreview = Color(0xFFE11D48),
        secondaryPreview = Color(0xFFD946EF),
        backgroundPreview = Color(0xFFFFF1F2)
    ),
    MIDNIGHT_NEON(
        id = "MIDNIGHT_NEON",
        displayName = "Midnight Neon",
        description = "Deep navy background with electric blue, purple, and cyan accents",
        primaryPreview = Color(0xFF38BDF8),
        secondaryPreview = Color(0xFFC084FC),
        backgroundPreview = Color(0xFF0B0F19),
        isDarkOnly = true
    ),
    FOREST_DARK(
        id = "FOREST_DARK",
        displayName = "Forest Dark",
        description = "Deep forest green background with mint and emerald accents",
        primaryPreview = Color(0xFF34D399),
        secondaryPreview = Color(0xFF10B981),
        backgroundPreview = Color(0xFF0A1F14),
        isDarkOnly = true
    ),
    WARM_CREAM(
        id = "WARM_CREAM",
        displayName = "Warm Cream",
        description = "Warm cream surfaces, espresso brown text, and terracotta accents",
        primaryPreview = Color(0xFFC2410C),
        secondaryPreview = Color(0xFFB45309),
        backgroundPreview = Color(0xFFFDFBF7),
        isLightOnly = true
    ),
    LAVENDER_LIGHT(
        id = "LAVENDER_LIGHT",
        displayName = "Lavender Light",
        description = "Soft lavender surfaces with purple and indigo accents",
        primaryPreview = Color(0xFF6D28D9),
        secondaryPreview = Color(0xFF4338CA),
        backgroundPreview = Color(0xFFF5F3FF),
        isLightOnly = true
    );

    fun getColorScheme(context: Context, isSystemDark: Boolean): ColorScheme {
        return when (this) {
            SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (isSystemDark) SystemDarkColorScheme else SystemLightColorScheme
                }
            }
            OCEAN_BLUE -> if (isSystemDark) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
            AURORA_PURPLE -> if (isSystemDark) AuroraPurpleDarkColorScheme else AuroraPurpleLightColorScheme
            EMERALD_GREEN -> if (isSystemDark) EmeraldGreenDarkColorScheme else EmeraldGreenLightColorScheme
            SUNSET_ORANGE -> if (isSystemDark) SunsetOrangeDarkColorScheme else SunsetOrangeLightColorScheme
            ROSE_PINK -> if (isSystemDark) RosePinkDarkColorScheme else RosePinkLightColorScheme
            MIDNIGHT_NEON -> MidnightNeonColorScheme
            FOREST_DARK -> ForestDarkColorScheme
            WARM_CREAM -> WarmCreamColorScheme
            LAVENDER_LIGHT -> LavenderLightColorScheme
        }
    }

    companion object {
        fun fromId(id: String?): AppTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

// 1. System Default Palettes
private val SystemLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

private val SystemDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF374151)
)

// 2. Ocean Blue Palettes
private val OceanBlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF155E75),
    background = Color(0xFFF0F9FF),
    onBackground = Color(0xFF0C4A6E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C4A6E),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF0369A1),
    outline = Color(0xFFBAE6FD)
)

private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF08253B),
    onBackground = Color(0xFFF0F9FF),
    surface = Color(0xFF0C3450),
    onSurface = Color(0xFFF0F9FF),
    surfaceVariant = Color(0xFF164868),
    onSurfaceVariant = Color(0xFF7DD3FC),
    outline = Color(0xFF0284C7)
)

// 3. Aurora Purple Palettes
private val AuroraPurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = Color(0xFFA855F7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF581C87),
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF2E1065),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2E1065),
    surfaceVariant = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFF6D28D9),
    outline = Color(0xFFDDD6FE)
)

private val AuroraPurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF3730A3),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFC084FC),
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF6B21A8),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = Color(0xFF120B24),
    onBackground = Color(0xFFFAF5FF),
    surface = Color(0xFF1F123C),
    onSurface = Color(0xFFFAF5FF),
    surfaceVariant = Color(0xFF311B5A),
    onSurfaceVariant = Color(0xFFDDD6FE),
    outline = Color(0xFF7C3AED)
)

// 4. Emerald Green Palettes
private val EmeraldGreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA7F3D0),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF115E59),
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF022C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF022C22),
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF065F46),
    outline = Color(0xFF86EFAC)
)

private val EmeraldGreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF047857),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF07241B),
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF0D362A),
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF134C3C),
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF059669)
)

// 5. Sunset Orange Palettes
private val SunsetOrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFF97316),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFED7AA),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFFFF7ED),
    onBackground = Color(0xFF431407),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF431407),
    surfaceVariant = Color(0xFFFFEDD5),
    onSurfaceVariant = Color(0xFF9A3412),
    outline = Color(0xFFFDBA74)
)

private val SunsetOrangeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF9A3412),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFFFDBA74),
    onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFED7AA),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF200E05),
    onBackground = Color(0xFFFFF7ED),
    surface = Color(0xFF33170B),
    onSurface = Color(0xFFFFF7ED),
    surfaceVariant = Color(0xFF4E2312),
    onSurfaceVariant = Color(0xFFFDBA74),
    outline = Color(0xFFEA580C)
)

// 6. Rose Pink Palettes
private val RosePinkLightColorScheme = lightColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = Color(0xFFD946EF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFAE8FF),
    onSecondaryContainer = Color(0xFF701A75),
    tertiary = Color(0xFFF43F5E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF9F1239),
    background = Color(0xFFFFF1F2),
    onBackground = Color(0xFF4C0519),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFF9F1239),
    outline = Color(0xFFFECDD3)
)

private val RosePinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF9F1239),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFE879F9),
    onSecondary = Color(0xFF4A044E),
    secondaryContainer = Color(0xFF701A75),
    onSecondaryContainer = Color(0xFFFAE8FF),
    tertiary = Color(0xFFFDA4AF),
    onTertiary = Color(0xFF4C0519),
    tertiaryContainer = Color(0xFF881337),
    onTertiaryContainer = Color(0xFFFFE4E6),
    background = Color(0xFF240610),
    onBackground = Color(0xFFFFF1F2),
    surface = Color(0xFF370C1B),
    onSurface = Color(0xFFFFF1F2),
    surfaceVariant = Color(0xFF52152B),
    onSurfaceVariant = Color(0xFFFECDD3),
    outline = Color(0xFFE11D48)
)

// 7. Midnight Neon Palette (Dedicated dark theme)
private val MidnightNeonColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF031424),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFFC084FC),
    onSecondary = Color(0xFF1E0738),
    secondaryContainer = Color(0xFF581C87),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF042429),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF0284C7)
)

// 8. Forest Dark Palette (Dedicated dark theme)
private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022016),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF02261A),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFDCFCE7),
    background = Color(0xFF0A1F14),
    onBackground = Color(0xFFECFDF5),
    surface = Color(0xFF11291B),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF1A3B27),
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF059669)
)

// 9. Warm Cream Palette (Dedicated light theme)
private val WarmCreamColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF78350F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5EBE1),
    onTertiaryContainer = Color(0xFF431407),
    background = Color(0xFFFDFBF7),
    onBackground = Color(0xFF292524),
    surface = Color(0xFFF7F3EA),
    onSurface = Color(0xFF292524),
    surfaceVariant = Color(0xFFEDE6D8),
    onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFD6CEBE)
)

// 10. Lavender Light Palette (Dedicated light theme)
private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF4338CA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = Color(0xFF9333EA),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF581C87),
    background = Color(0xFFF5F3FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color(0xFFFAF5FF),
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFF6B21A8),
    outline = Color(0xFFDDD6FE)
)
