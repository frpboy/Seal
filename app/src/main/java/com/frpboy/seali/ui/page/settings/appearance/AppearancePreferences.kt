package com.frpboy.seali.ui.page.settings.appearance

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.android.material.color.DynamicColors
import com.frpboy.seali.R
import com.frpboy.seali.download.DownloaderV2
import com.frpboy.seali.download.Task
import com.frpboy.seali.ui.common.LocalDarkTheme
import com.frpboy.seali.ui.common.LocalDynamicColorSwitch
import com.frpboy.seali.ui.common.LocalPaletteStyleIndex
import com.frpboy.seali.ui.common.LocalSeedColor
import com.frpboy.seali.ui.common.Route
import com.frpboy.seali.ui.component.BackButton
import com.frpboy.seali.ui.component.PreferenceItem
import com.frpboy.seali.ui.component.PreferenceSwitch
import com.frpboy.seali.ui.component.PreferenceSwitchWithDivider
import com.frpboy.seali.ui.page.downloadv2.ActionButton
import com.frpboy.seali.ui.page.downloadv2.CardStateIndicator
import com.frpboy.seali.ui.page.downloadv2.VideoCardV2
import com.frpboy.seali.util.DarkThemePreference.Companion.OFF
import com.frpboy.seali.util.DarkThemePreference.Companion.ON
import com.frpboy.seali.util.InterfaceSwitchGuard
import com.frpboy.seali.util.PreferenceUtil
import com.frpboy.seali.util.STYLE_MONOCHROME
import com.frpboy.seali.util.STYLE_TONAL_SPOT
import com.frpboy.seali.util.UI_STYLE_CLASSIC
import com.frpboy.seali.util.UI_STYLE_V2
import com.frpboy.seali.util.paletteStyles
import com.frpboy.seali.util.toDisplayName
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.a1
import com.kyant.monet.a2
import com.kyant.monet.a3
import io.material.hct.Hct
import java.util.Locale
import kotlinx.coroutines.Job
import org.koin.compose.koinInject

private val ColorList =
    ((4..10) + (1..3)).map { it * 35.0 }.map { Color(Hct.from(it, 40.0, 40.0).toInt()) }

private val DrawableList =
    listOf(R.drawable.sample, R.drawable.sample1, R.drawable.sample2, R.drawable.sample3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferences(onNavigateBack: () -> Unit, onNavigateTo: (String) -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    val index by remember { mutableIntStateOf(DrawableList.indices.random()) }

    val image by remember(index) { mutableIntStateOf(DrawableList[index]) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.look_and_feel))
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(it)) {
                val downloadState = Task.DownloadState.Running(Job(), "", 0.8f)
                VideoCardV2(
                    modifier = Modifier.padding(18.dp).clearAndSetSemantics {},
                    title = stringResource(R.string.video_title_sample_text),
                    uploader = stringResource(R.string.video_creator_sample_text),
                    thumbnailModel = image,
                    stateIndicator = {
                        CardStateIndicator(modifier = Modifier, downloadState = downloadState)
                    },
                    actionButton = {
                        ActionButton(modifier = Modifier, downloadState = downloadState) {}
                    },
                ) {}
                val pageCount = ColorList.size + 1

                val pagerState =
                    rememberPagerState(
                        initialPage =
                            if (LocalPaletteStyleIndex.current == STYLE_MONOCHROME) pageCount - 1
                            else
                                ColorList.indexOf(Color(LocalSeedColor.current)).run {
                                    if (this == -1) 0 else this
                                }
                    ) {
                        pageCount
                    }

                HorizontalPager(
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { page ->
                    if (page < pageCount - 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ColorButtons(ColorList[page])
                        }
                    } else {
                        // ColorButton for Monochrome theme
                        val isSelected =
                            LocalPaletteStyleIndex.current == STYLE_MONOCHROME &&
                                !LocalDynamicColorSwitch.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ColorButtonImpl(
                                modifier = Modifier,
                                isSelected = { isSelected },
                                tonalPalettes =
                                    Color.Black.toTonalPalettes(PaletteStyle.Monochrome),
                                onClick = {
                                    PreferenceUtil.switchDynamicColor(enabled = false)
                                    PreferenceUtil.modifyThemeSeedColor(
                                        Color.Black.toArgb(),
                                        STYLE_MONOCHROME,
                                    )
                                },
                            )
                        }
                    }
                }

                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    pageCount = pageCount,
                    modifier =
                        Modifier.clearAndSetSemantics {}
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp),
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.outlineVariant,
                    indicatorHeight = 6.dp,
                    indicatorWidth = 6.dp,
                )
                Text(
                    text = stringResource(R.string.seali_accents),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    CuratedAccent(Color(0xFF007F86), stringResource(R.string.seali_accent_teal))
                    CuratedAccent(Color(0xFF3867D6), stringResource(R.string.seali_accent_blue))
                }
                if (DynamicColors.isDynamicColorAvailable()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_color),
                        description = stringResource(id = R.string.dynamic_color_desc),
                        icon = Icons.Outlined.Colorize,
                        isChecked = LocalDynamicColorSwitch.current,
                        onClick = { PreferenceUtil.switchDynamicColor() },
                    )
                }
                val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    isChecked = isDarkTheme,
                    description = LocalDarkTheme.current.getDarkThemeDesc(),
                    onChecked = {
                        PreferenceUtil.modifyDarkThemePreference(if (isDarkTheme) OFF else ON)
                    },
                    onClick = { onNavigateTo(Route.DARK_THEME) },
                )
                PreferenceItem(
                    title = stringResource(R.string.language),
                    icon = Icons.Outlined.Language,
                    description = Locale.getDefault().toDisplayName(),
                ) {
                    onNavigateTo(Route.LANGUAGES)
                }
                val context = LocalContext.current
                val downloader: DownloaderV2 = koinInject()
                var showInterfaceDialog by rememberSaveable { mutableStateOf(false) }
                val currentUiStyle = remember { PreferenceUtil.getUiStyle() }
                PreferenceItem(
                    title = stringResource(R.string.interface_style),
                    icon = Icons.Outlined.ViewQuilt,
                    description =
                        stringResource(
                            if (currentUiStyle == UI_STYLE_CLASSIC) R.string.interface_classic_summary
                            else R.string.interface_v2_summary
                        ),
                ) {
                    showInterfaceDialog = true
                }
                if (showInterfaceDialog) {
                    InterfaceStyleDialog(
                        current = currentUiStyle,
                        onDismiss = { showInterfaceDialog = false },
                        onSelect = { selected ->
                            val active =
                                InterfaceSwitchGuard.hasActiveDownloads(
                                    downloader.getTaskStateMap().values.map { it.downloadState }
                                )
                            if (active) {
                                Toast.makeText(
                                        context,
                                        R.string.interface_switch_active,
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            } else if (selected != currentUiStyle) {
                                PreferenceUtil.updateUiStyle(selected)
                                (context as? Activity)?.recreate()
                            }
                            showInterfaceDialog = false
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun RowScope.ColorButtons(color: Color) {
    paletteStyles.subList(STYLE_TONAL_SPOT, STYLE_MONOCHROME).forEachIndexed { index, style ->
        ColorButton(color = color, index = index, tonalStyle = style)
    }
}

@Composable
fun RowScope.ColorButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    index: Int = 0,
    tonalStyle: PaletteStyle = PaletteStyle.TonalSpot,
) {
    val tonalPalettes by remember { mutableStateOf(color.toTonalPalettes(tonalStyle)) }
    val isSelect =
        !LocalDynamicColorSwitch.current &&
            LocalSeedColor.current == color.toArgb() &&
            LocalPaletteStyleIndex.current == index
    ColorButtonImpl(modifier = modifier, tonalPalettes = tonalPalettes, isSelected = { isSelect }) {
        PreferenceUtil.switchDynamicColor(enabled = false)
        PreferenceUtil.modifyThemeSeedColor(color.toArgb(), index)
    }
}

@Composable
fun RowScope.ColorButtonImpl(
    modifier: Modifier = Modifier,
    isSelected: () -> Boolean = { false },
    tonalPalettes: TonalPalettes,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit = {},
) {

    val containerSize by animateDpAsState(targetValue = if (isSelected.invoke()) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (isSelected.invoke()) 16.dp else 0.dp)

    Surface(
        modifier =
            modifier
                .padding(4.dp)
                .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
                .weight(1f, false)
                .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        onClick = onClick,
    ) {
        CompositionLocalProvider(LocalTonalPalettes provides tonalPalettes) {
            val color1 = 80.a1
            val color2 = 90.a2
            val color3 = 60.a3
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(color1) }
                            .align(Alignment.Center)
                ) {
                    Surface(
                        color = color2,
                        modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                    ) {}
                    Surface(
                        color = color3,
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                    ) {}
                    Box(
                        modifier =
                            Modifier.align(Alignment.Center)
                                .clip(CircleShape)
                                .size(containerSize)
                                .drawBehind { drawCircle(containerColor) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterfaceStyleDialog(
    current: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    var selected by rememberSaveable(current) { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.interface_style)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                InterfaceChoice(
                    title = stringResource(R.string.interface_v2),
                    description = stringResource(R.string.interface_v2_summary),
                    selected = selected == UI_STYLE_V2,
                    onSelect = { selected = UI_STYLE_V2 },
                )
                InterfaceChoice(
                    title = stringResource(R.string.interface_classic),
                    description = stringResource(R.string.interface_classic_summary),
                    selected = selected == UI_STYLE_CLASSIC,
                    onSelect = { selected = UI_STYLE_CLASSIC },
                )
                Text(
                    text = stringResource(R.string.interface_switch_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text(stringResource(R.string.interface_switch))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun CuratedAccent(seed: Color, label: String) {
    FilterChip(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        selected = !LocalDynamicColorSwitch.current &&
            LocalSeedColor.current == seed.toArgb() &&
            LocalPaletteStyleIndex.current == STYLE_TONAL_SPOT,
        onClick = {
            PreferenceUtil.switchDynamicColor(enabled = false)
            PreferenceUtil.modifyThemeSeedColor(seed.toArgb(), STYLE_TONAL_SPOT)
        },
        label = { Text(label) },
        leadingIcon = { Box(Modifier.size(18.dp).clip(CircleShape).background(seed)) },
    )
}

@Composable
private fun InterfaceChoice(title: String, description: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
