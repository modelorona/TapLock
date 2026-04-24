package com.ah.taplock

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ah.taplock.ui.theme.TapLockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == "com.ah.taplock.LOCK_NOW") {
            TapLockAccessibilityService.instance?.lockScreen()
            finish()
            return
        }
        setContent {
            TapLockTheme {
                TapLockScreen()
            }
        }
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapLockScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val githubUrl = stringResource(R.string.github_url)
    val sharedPrefName = stringResource(R.string.shared_pref_name)
    val doubleTapTimeoutKey = stringResource(R.string.double_tap_timeout)
    val showWidgetIconKey = stringResource(R.string.show_widget_icon)
    val widgetStyleKey = stringResource(R.string.widget_style)
    val vibrateOnLockKey = stringResource(R.string.vibrate_on_lock)
    val statusBarDoubleTapKey = stringResource(R.string.status_bar_double_tap)
    val lockScreenDoubleTapKey = stringResource(R.string.lock_screen_double_tap)
    val leftEdgeLockZoneKey = stringResource(R.string.left_edge_lock_zone)
    val rightEdgeLockZoneKey = stringResource(R.string.right_edge_lock_zone)
    val topLeftCornerLockZoneKey = stringResource(R.string.top_left_corner_lock_zone)
    val topRightCornerLockZoneKey = stringResource(R.string.top_right_corner_lock_zone)
    val bottomLeftCornerLockZoneKey = stringResource(R.string.bottom_left_corner_lock_zone)
    val bottomRightCornerLockZoneKey = stringResource(R.string.bottom_right_corner_lock_zone)
    val edgeZoneWidthDpKey = stringResource(R.string.edge_zone_width_dp)
    val cornerZoneSizeDpKey = stringResource(R.string.corner_zone_size_dp)
    val edgeZoneCoveragePercentKey = stringResource(R.string.edge_zone_coverage_percent)
    val edgeZoneTopOffsetPercentKey = stringResource(R.string.edge_zone_top_offset_percent)
    val edgeZoneBottomOffsetPercentKey = stringResource(R.string.edge_zone_bottom_offset_percent)
    val customIconUpdatedMsg = stringResource(R.string.custom_icon_updated)
    val customIconResetMsg = stringResource(R.string.custom_icon_reset)
    val hasSeenInfoKey = stringResource(R.string.has_seen_info)
    val hasCompletedOnboardingKey = stringResource(R.string.has_completed_onboarding)
    val lockDelayMsKey = stringResource(R.string.lock_delay_ms)
    val lockCountKey = stringResource(R.string.lock_count)
    val lockZonePercentKey = stringResource(R.string.lock_zone_percent)
    val quickSettingsTileAddedKey = stringResource(R.string.quick_settings_tile_added)
    val tileLabel = stringResource(R.string.tile_label)
    val floatingButtonKey = stringResource(R.string.floating_button_enabled)
    val floatingButtonSizeDpKey = stringResource(R.string.floating_button_size_dp)
    val floatingButtonOpacityPercentKey = stringResource(R.string.floating_button_opacity_percent)

    var showDialog by remember { mutableStateOf(false) }

    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityEnabled(context))
    }

    var isBatteryOptimized by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }

    var timeoutValue by remember { mutableFloatStateOf(300f) }
    var showIcon by remember { mutableStateOf(false) }
    var widgetStyle by remember { mutableStateOf(TapLockWidgetStyle.default) }
    var vibrateOnLock by remember { mutableStateOf(true) }
    var vibrationPattern by remember { mutableStateOf(VibrationPattern.MEDIUM) }
    var statusBarDoubleTap by remember { mutableStateOf(false) }
    var lockScreenDoubleTap by remember { mutableStateOf(false) }
    var leftEdgeZoneEnabled by remember { mutableStateOf(false) }
    var rightEdgeZoneEnabled by remember { mutableStateOf(false) }
    var topLeftCornerZoneEnabled by remember { mutableStateOf(false) }
    var topRightCornerZoneEnabled by remember { mutableStateOf(false) }
    var bottomLeftCornerZoneEnabled by remember { mutableStateOf(false) }
    var bottomRightCornerZoneEnabled by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(0) }
    var lockDelayMs by remember { mutableIntStateOf(0) }
    var lockCount by remember { mutableIntStateOf(0) }
    var lockZonePercent by remember { mutableFloatStateOf(66f) }
    var edgeZoneWidthDp by remember {
        mutableFloatStateOf(TapLockEdgeZones.DEFAULT_WIDTH_DP.toFloat())
    }
    var edgeZoneTopOffsetPercent by remember {
        mutableFloatStateOf(TapLockEdgeZones.DEFAULT_TOP_OFFSET_PERCENT.toFloat())
    }
    var edgeZoneBottomOffsetPercent by remember {
        mutableFloatStateOf(TapLockEdgeZones.DEFAULT_BOTTOM_OFFSET_PERCENT.toFloat())
    }
    var cornerZoneSizeDp by remember {
        mutableFloatStateOf(TapLockEdgeZones.DEFAULT_CORNER_SIZE_DP.toFloat())
    }
    var floatingButtonEnabled by remember { mutableStateOf(false) }
    var floatingButtonSizeDp by remember {
        mutableFloatStateOf(TapLockFloatingButtonConfig.DEFAULT_SIZE_DP.toFloat())
    }
    var floatingButtonOpacityPercent by remember {
        mutableFloatStateOf(TapLockFloatingButtonConfig.DEFAULT_OPACITY_PERCENT.toFloat())
    }
    var isTileAdded by remember { mutableStateOf(false) }
    var widgetCount by remember { mutableIntStateOf(0) }
    var widgetIconBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var excludedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableApps by remember { mutableStateOf<List<TapLockAppInfo>>(emptyList()) }
    var showAppExclusionDialog by remember { mutableStateOf(false) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var appSearchQuery by remember { mutableStateOf("") }
    val edgeWidthSliderInteraction = remember { MutableInteractionSource() }
    val edgeTopOffsetSliderInteraction = remember { MutableInteractionSource() }
    val edgeBottomOffsetSliderInteraction = remember { MutableInteractionSource() }
    val cornerSizeSliderInteraction = remember { MutableInteractionSource() }
    val isEdgeWidthSliderDragged by edgeWidthSliderInteraction.collectIsDraggedAsState()
    val isEdgeTopOffsetSliderDragged by edgeTopOffsetSliderInteraction.collectIsDraggedAsState()
    val isEdgeBottomOffsetSliderDragged by edgeBottomOffsetSliderInteraction.collectIsDraggedAsState()
    val isCornerSizeSliderDragged by cornerSizeSliderInteraction.collectIsDraggedAsState()
    val editableLeftEdgeZoneEnabled = leftEdgeZoneEnabled
    val editableRightEdgeZoneEnabled = rightEdgeZoneEnabled
    val editableTopLeftCornerZoneEnabled = topLeftCornerZoneEnabled
    val editableTopRightCornerZoneEnabled = topRightCornerZoneEnabled
    val editableBottomLeftCornerZoneEnabled = bottomLeftCornerZoneEnabled
    val editableBottomRightCornerZoneEnabled = bottomRightCornerZoneEnabled
    val editableEdgeZoneWidthDp = edgeZoneWidthDp
    val editableEdgeZoneTopOffsetPercent = edgeZoneTopOffsetPercent
    val editableEdgeZoneBottomOffsetPercent = edgeZoneBottomOffsetPercent
    val editableCornerZoneSizeDp = cornerZoneSizeDp
    val anyEdgeZoneEnabled = editableLeftEdgeZoneEnabled || editableRightEdgeZoneEnabled
    val anyCornerZoneEnabled = editableTopLeftCornerZoneEnabled ||
        editableTopRightCornerZoneEnabled ||
        editableBottomLeftCornerZoneEnabled ||
        editableBottomRightCornerZoneEnabled
    val showLiveZoneOverlay = (
        anyEdgeZoneEnabled || anyCornerZoneEnabled
        ) && (
        isEdgeWidthSliderDragged ||
            isEdgeTopOffsetSliderDragged ||
            isEdgeBottomOffsetSliderDragged ||
            isCornerSizeSliderDragged
        )

    fun saveSelectedZoneBoolean(baseKey: String, value: Boolean) {
        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
            .edit { putBoolean(baseKey, value) }
    }

    fun saveSelectedZoneInt(baseKey: String, value: Int) {
        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
            .edit { putInt(baseKey, value) }
    }

    fun setEditableEdgeEnabled(side: EdgeZoneSide, isEnabled: Boolean) {
        when (side) {
            EdgeZoneSide.LEFT -> leftEdgeZoneEnabled = isEnabled
            EdgeZoneSide.RIGHT -> rightEdgeZoneEnabled = isEnabled
        }
    }

    fun setEditableCornerEnabled(position: CornerZonePosition, isEnabled: Boolean) {
        when (position) {
            CornerZonePosition.TOP_LEFT -> topLeftCornerZoneEnabled = isEnabled
            CornerZonePosition.TOP_RIGHT -> topRightCornerZoneEnabled = isEnabled
            CornerZonePosition.BOTTOM_LEFT -> bottomLeftCornerZoneEnabled = isEnabled
            CornerZonePosition.BOTTOM_RIGHT -> bottomRightCornerZoneEnabled = isEnabled
        }
    }

    fun setEditableEdgeWidth(value: Float) {
        edgeZoneWidthDp = value
    }

    fun setEditableTopOffset(value: Float) {
        edgeZoneTopOffsetPercent = value
    }

    fun setEditableBottomOffset(value: Float) {
        edgeZoneBottomOffsetPercent = value
    }

    fun setEditableCornerSize(value: Float) {
        cornerZoneSizeDp = value
    }

    fun refreshWidgetCount() {
        widgetCount = TapLockWidgetProvider.getWidgetCount(context)
    }

    fun refreshWidgets() {
        TapLockWidgetProvider.refreshAll(context)
        refreshWidgetCount()
    }

    fun restartFloatingButtonServiceIfRunning() {
        if (!floatingButtonEnabled || !Settings.canDrawOverlays(context)) return
        val serviceIntent = Intent(context, FloatingButtonService::class.java)
        context.stopService(serviceIntent)
        context.startForegroundService(serviceIntent)
    }

    fun requestQuickSettingsTilePrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            TapLockFeedback.showQuickSettingsAddUnsupported(context)
            return
        }

        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
            TapLockFeedback.showQuickSettingsAddResult(context, -1)
            return
        }

        runCatching {
            statusBarManager.requestAddTileService(
                ComponentName(context, TapLockTileService::class.java),
                tileLabel,
                Icon.createWithResource(context, R.drawable.ic_lock_tile),
                context.mainExecutor
            ) { result ->
                if (
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                ) {
                    isTileAdded = true
                    context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                        .edit { putBoolean(quickSettingsTileAddedKey, true) }
                }
                TapLockFeedback.showQuickSettingsAddResult(context, result)
            }
        }.onFailure {
            TapLockFeedback.showQuickSettingsAddResult(context, -1)
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        timeoutValue = prefs.getInt(doubleTapTimeoutKey, 300).toFloat()
        showIcon = prefs.getBoolean(showWidgetIconKey, false)
        widgetStyle = TapLockWidgetStyle.fromStored(prefs.getString(widgetStyleKey, null))
        vibrateOnLock = prefs.getBoolean(vibrateOnLockKey, true)
        vibrationPattern = VibrationHelper.fromPrefs(context)
        statusBarDoubleTap = prefs.getBoolean(statusBarDoubleTapKey, false)
        lockScreenDoubleTap = prefs.getBoolean(lockScreenDoubleTapKey, false)
        leftEdgeZoneEnabled = prefs.getBoolean(leftEdgeLockZoneKey, false)
        rightEdgeZoneEnabled = prefs.getBoolean(rightEdgeLockZoneKey, false)
        topLeftCornerZoneEnabled = prefs.getBoolean(topLeftCornerLockZoneKey, false)
        topRightCornerZoneEnabled = prefs.getBoolean(topRightCornerLockZoneKey, false)
        bottomLeftCornerZoneEnabled = prefs.getBoolean(bottomLeftCornerLockZoneKey, false)
        bottomRightCornerZoneEnabled = prefs.getBoolean(bottomRightCornerLockZoneKey, false)
        infoExpanded = !prefs.getBoolean(hasSeenInfoKey, false)
        showOnboarding = !prefs.getBoolean(hasCompletedOnboardingKey, false)
        lockDelayMs = prefs.getInt(lockDelayMsKey, 0)
        lockCount = prefs.getInt(lockCountKey, 0)
        lockZonePercent = prefs.getInt(lockZonePercentKey, 66).toFloat()
        edgeZoneWidthDp = prefs.getInt(
            edgeZoneWidthDpKey,
            TapLockEdgeZones.DEFAULT_WIDTH_DP
        ).toFloat()
        val legacyEdgeCoveragePercent = prefs.getInt(
            edgeZoneCoveragePercentKey,
            TapLockEdgeZones.DEFAULT_COVERAGE_PERCENT
        )
        val (fallbackTopOffsetPercent, fallbackBottomOffsetPercent) =
            TapLockEdgeZones.deriveOffsetsFromCoverage(legacyEdgeCoveragePercent)
        edgeZoneTopOffsetPercent = prefs.getInt(
            edgeZoneTopOffsetPercentKey,
            fallbackTopOffsetPercent
        ).toFloat()
        edgeZoneBottomOffsetPercent = prefs.getInt(
            edgeZoneBottomOffsetPercentKey,
            fallbackBottomOffsetPercent
        ).toFloat()
        cornerZoneSizeDp = prefs.getInt(
            cornerZoneSizeDpKey,
            TapLockEdgeZones.DEFAULT_CORNER_SIZE_DP
        ).toFloat()
        floatingButtonEnabled = prefs.getBoolean(floatingButtonKey, false)
        floatingButtonSizeDp = TapLockFloatingButtonConfig.clampSizeDp(
            prefs.getInt(
                floatingButtonSizeDpKey,
                TapLockFloatingButtonConfig.DEFAULT_SIZE_DP
            )
        ).toFloat()
        floatingButtonOpacityPercent = TapLockFloatingButtonConfig.clampOpacityPercent(
            prefs.getInt(
                floatingButtonOpacityPercentKey,
                TapLockFloatingButtonConfig.DEFAULT_OPACITY_PERCENT
            )
        ).toFloat()
        isTileAdded = prefs.getBoolean(quickSettingsTileAddedKey, false)
        excludedPackages = TapLockAppRules.getExcludedPackages(context)
        refreshWidgetCount()
        // Load custom icon for preview
        val iconFile = File(context.filesDir, "custom_widget_icon.png")
        widgetIconBitmap = if (iconFile.exists()) {
            BitmapFactory.decodeFile(iconFile.absolutePath)?.asImageBitmap()
        } else null

        isLoadingApps = true
        availableApps = withContext(Dispatchers.IO) {
            TapLockAppRules.loadLaunchableApps(context)
        }
        isLoadingApps = false
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val scaledBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                                bitmap.scale(512, 512)
                            } else {
                                bitmap
                            }
                            val file = File(context.filesDir, "custom_widget_icon.png")
                            FileOutputStream(file).use { out ->
                                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, customIconUpdatedMsg, Toast.LENGTH_SHORT).show()
                            val newIconFile = File(context.filesDir, "custom_widget_icon.png")
                            widgetIconBitmap = BitmapFactory.decodeFile(newIconFile.absolutePath)?.asImageBitmap()
                            refreshWidgets()
                            restartFloatingButtonServiceIfRunning()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val defaultAppIconBitmap = remember {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
        val bmp = createBitmap(width, height)
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }

    val disclaimerString = buildAnnotatedString {
        append(stringResource(R.string.home_screen_disclaimer))
        append(" ")
        withLink(LinkAnnotation.Url(
            url = githubUrl,
            styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
        )) {
            append(githubUrl)
        }
    }

    val excludedAppLabels = excludedPackages
        .map { packageName ->
            availableApps.firstOrNull { it.packageName == packageName }?.label
                ?: TapLockAppRules.resolveAppLabel(context, packageName)
        }
        .sortedBy { it.lowercase(Locale.getDefault()) }

    val excludedAppsSummary = when {
        excludedAppLabels.isEmpty() -> stringResource(R.string.app_exclusions_none)
        excludedAppLabels.size > 3 -> stringResource(
            R.string.app_exclusions_summary_more,
            excludedAppLabels.take(3).joinToString(", "),
            excludedAppLabels.size - 3
        )
        else -> excludedAppLabels.joinToString(", ")
    }

    DisposableEffect(Unit) {
        val lifecycleOwner = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityEnabled(context)
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
                val prefs = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                lockCount = prefs.getInt(lockCountKey, 0)
                showIcon = prefs.getBoolean(showWidgetIconKey, false)
                widgetStyle = TapLockWidgetStyle.fromStored(prefs.getString(widgetStyleKey, null))
                leftEdgeZoneEnabled = prefs.getBoolean(leftEdgeLockZoneKey, false)
                rightEdgeZoneEnabled = prefs.getBoolean(rightEdgeLockZoneKey, false)
                topLeftCornerZoneEnabled = prefs.getBoolean(topLeftCornerLockZoneKey, false)
                topRightCornerZoneEnabled = prefs.getBoolean(topRightCornerLockZoneKey, false)
                bottomLeftCornerZoneEnabled = prefs.getBoolean(bottomLeftCornerLockZoneKey, false)
                bottomRightCornerZoneEnabled = prefs.getBoolean(bottomRightCornerLockZoneKey, false)
                edgeZoneWidthDp = prefs.getInt(
                    edgeZoneWidthDpKey,
                    TapLockEdgeZones.DEFAULT_WIDTH_DP
                ).toFloat()
                val legacyEdgeCoveragePercent = prefs.getInt(
                    edgeZoneCoveragePercentKey,
                    TapLockEdgeZones.DEFAULT_COVERAGE_PERCENT
                )
                val (fallbackTopOffsetPercent, fallbackBottomOffsetPercent) =
                    TapLockEdgeZones.deriveOffsetsFromCoverage(legacyEdgeCoveragePercent)
                edgeZoneTopOffsetPercent = prefs.getInt(
                    edgeZoneTopOffsetPercentKey,
                    fallbackTopOffsetPercent
                ).toFloat()
                edgeZoneBottomOffsetPercent = prefs.getInt(
                    edgeZoneBottomOffsetPercentKey,
                    fallbackBottomOffsetPercent
                ).toFloat()
                cornerZoneSizeDp = prefs.getInt(
                    cornerZoneSizeDpKey,
                    TapLockEdgeZones.DEFAULT_CORNER_SIZE_DP
                ).toFloat()
                floatingButtonEnabled = prefs.getBoolean(floatingButtonKey, false)
                floatingButtonSizeDp = TapLockFloatingButtonConfig.clampSizeDp(
                    prefs.getInt(
                        floatingButtonSizeDpKey,
                        TapLockFloatingButtonConfig.DEFAULT_SIZE_DP
                    )
                ).toFloat()
                floatingButtonOpacityPercent = TapLockFloatingButtonConfig.clampOpacityPercent(
                    prefs.getInt(
                        floatingButtonOpacityPercentKey,
                        TapLockFloatingButtonConfig.DEFAULT_OPACITY_PERCENT
                    )
                ).toFloat()
                isTileAdded = prefs.getBoolean(quickSettingsTileAddedKey, false)
                excludedPackages = TapLockAppRules.getExcludedPackages(context)
                refreshWidgetCount()
                coroutineScope.launch {
                    isLoadingApps = true
                    availableApps = withContext(Dispatchers.IO) {
                        TapLockAppRules.loadLaunchableApps(context)
                    }
                    isLoadingApps = false
                }
                // If user just granted overlay permission, start the service
                if (floatingButtonEnabled && !Settings.canDrawOverlays(context)) {
                    floatingButtonEnabled = false
                    prefs.edit { putBoolean(floatingButtonKey, false) }
                    context.stopService(Intent(context, FloatingButtonService::class.java))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.top_bar_title)) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Collapsible info section
            InfoSection(
                expanded = infoExpanded,
                onToggle = {
                    infoExpanded = !infoExpanded
                    if (!infoExpanded) {
                        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                            .edit { putBoolean(hasSeenInfoKey, true) }
                    }
                },
                disclaimerString = disclaimerString
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.required_permissions),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAccessibilityEnabled) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(stringResource(R.string.accessibility_service), modifier = Modifier.weight(1f))
                        Button(
                            onClick = { showDialog = true },
                            enabled = !isAccessibilityEnabled,
                        ) {
                            Text(text = if (isAccessibilityEnabled) stringResource(R.string.enabled) else stringResource(R.string.enable))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.battery_optimization_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBatteryOptimized) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            if (isBatteryOptimized) stringResource(R.string.battery_status_restricting)
                            else stringResource(R.string.battery_status_unrestricted),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        ) {
                            Text(
                                if (isBatteryOptimized) stringResource(R.string.battery_action_fix)
                                else stringResource(R.string.battery_action_restrict)
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_label),
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (!isAccessibilityEnabled) {
                        Text(
                            stringResource(R.string.accessibility_required_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFA000)
                        )
                    }

                    Text(
                        stringResource(R.string.slider_timeout_label, timeoutValue.toInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = timeoutValue,
                        onValueChange = { timeoutValue = it },
                        onValueChangeFinished = {
                            context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                .edit {
                                    putInt(doubleTapTimeoutKey, timeoutValue.toInt())
                                }
                        },
                        valueRange = 100f..800f,
                        steps = 13,
                        modifier = Modifier.testTag("slider_timeout")
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.edge_zones_label))
                        Text(
                            stringResource(R.string.edge_zones_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.edge_zones_portrait_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.edge_zones_subsection_label),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.left_edge_lock_zone_label))
                            Switch(
                                checked = editableLeftEdgeZoneEnabled,
                                onCheckedChange = { isChecked ->
                                    setEditableEdgeEnabled(EdgeZoneSide.LEFT, isChecked)
                                    saveSelectedZoneBoolean(leftEdgeLockZoneKey, isChecked)
                                },
                                enabled = isAccessibilityEnabled,
                                modifier = Modifier.testTag("switch_left_edge_zone")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.right_edge_lock_zone_label))
                            Switch(
                                checked = editableRightEdgeZoneEnabled,
                                onCheckedChange = { isChecked ->
                                    setEditableEdgeEnabled(EdgeZoneSide.RIGHT, isChecked)
                                    saveSelectedZoneBoolean(rightEdgeLockZoneKey, isChecked)
                                },
                                enabled = isAccessibilityEnabled,
                                modifier = Modifier.testTag("switch_right_edge_zone")
                            )
                        }

                        if (anyEdgeZoneEnabled) {
                            Text(
                                stringResource(
                                    R.string.edge_zone_width_label,
                                    editableEdgeZoneWidthDp.toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = editableEdgeZoneWidthDp,
                                onValueChange = { setEditableEdgeWidth(it) },
                                onValueChangeFinished = {
                                    saveSelectedZoneInt(
                                        edgeZoneWidthDpKey,
                                        editableEdgeZoneWidthDp.toInt()
                                    )
                                },
                                valueRange = TapLockEdgeZones.MIN_WIDTH_DP.toFloat()..
                                    TapLockEdgeZones.MAX_WIDTH_DP.toFloat(),
                                interactionSource = edgeWidthSliderInteraction,
                                modifier = Modifier.testTag("slider_edge_zone_width")
                            )

                            Text(
                                stringResource(
                                    R.string.edge_zone_top_offset_label,
                                    editableEdgeZoneTopOffsetPercent.toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = editableEdgeZoneTopOffsetPercent,
                                onValueChange = { setEditableTopOffset(it) },
                                onValueChangeFinished = {
                                    saveSelectedZoneInt(
                                        edgeZoneTopOffsetPercentKey,
                                        editableEdgeZoneTopOffsetPercent.toInt()
                                    )
                                },
                                valueRange = TapLockEdgeZones.MIN_OFFSET_PERCENT.toFloat()..
                                    TapLockEdgeZones.MAX_OFFSET_PERCENT.toFloat(),
                                interactionSource = edgeTopOffsetSliderInteraction,
                                modifier = Modifier.testTag("slider_edge_zone_top_offset")
                            )

                            Text(
                                stringResource(
                                    R.string.edge_zone_bottom_offset_label,
                                    editableEdgeZoneBottomOffsetPercent.toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = editableEdgeZoneBottomOffsetPercent,
                                onValueChange = { setEditableBottomOffset(it) },
                                onValueChangeFinished = {
                                    saveSelectedZoneInt(
                                        edgeZoneBottomOffsetPercentKey,
                                        editableEdgeZoneBottomOffsetPercent.toInt()
                                    )
                                },
                                valueRange = TapLockEdgeZones.MIN_OFFSET_PERCENT.toFloat()..
                                    TapLockEdgeZones.MAX_OFFSET_PERCENT.toFloat(),
                                interactionSource = edgeBottomOffsetSliderInteraction,
                                modifier = Modifier.testTag("slider_edge_zone_bottom_offset")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            stringResource(R.string.corner_zones_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.corner_zones_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.top_left_corner_lock_zone_label))
                            Switch(
                                checked = editableTopLeftCornerZoneEnabled,
                                onCheckedChange = { isChecked ->
                                    setEditableCornerEnabled(
                                        CornerZonePosition.TOP_LEFT,
                                        isChecked
                                    )
                                    saveSelectedZoneBoolean(topLeftCornerLockZoneKey, isChecked)
                                },
                                enabled = isAccessibilityEnabled,
                                modifier = Modifier.testTag("switch_top_left_corner_zone")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.top_right_corner_lock_zone_label))
                            Switch(
                                checked = editableTopRightCornerZoneEnabled,
                                onCheckedChange = { isChecked ->
                                    setEditableCornerEnabled(
                                        CornerZonePosition.TOP_RIGHT,
                                        isChecked
                                    )
                                    saveSelectedZoneBoolean(topRightCornerLockZoneKey, isChecked)
                                },
                                enabled = isAccessibilityEnabled,
                                modifier = Modifier.testTag("switch_top_right_corner_zone")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.bottom_left_corner_lock_zone_label))
                            Switch(
                                checked = editableBottomLeftCornerZoneEnabled,
                                onCheckedChange = { isChecked ->
                                    setEditableCornerEnabled(
                                        CornerZonePosition.BOTTOM_LEFT,
                                        isChecked
                                    )
                                    saveSelectedZoneBoolean(
                                        bottomLeftCornerLockZoneKey,
                                        isChecked
                                    )
                                },
                                enabled = isAccessibilityEnabled,
                                modifier = Modifier.testTag("switch_bottom_left_corner_zone")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.bottom_right_corner_lock_zone_label))
                            Switch(
                                checked = editableBottomRightCornerZoneEnabled,
                                onCheckedChange = { isChecked ->
                                    setEditableCornerEnabled(
                                        CornerZonePosition.BOTTOM_RIGHT,
                                        isChecked
                                    )
                                    saveSelectedZoneBoolean(
                                        bottomRightCornerLockZoneKey,
                                        isChecked
                                    )
                                },
                                enabled = isAccessibilityEnabled,
                                modifier = Modifier.testTag("switch_bottom_right_corner_zone")
                            )
                        }

                        if (anyCornerZoneEnabled) {
                            Text(
                                stringResource(
                                    R.string.corner_zone_size_label,
                                    editableCornerZoneSizeDp.toInt()
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = editableCornerZoneSizeDp,
                                onValueChange = { setEditableCornerSize(it) },
                                onValueChangeFinished = {
                                    saveSelectedZoneInt(
                                        cornerZoneSizeDpKey,
                                        editableCornerZoneSizeDp.toInt()
                                    )
                                },
                                valueRange = TapLockEdgeZones.MIN_CORNER_SIZE_DP.toFloat()..
                                    TapLockEdgeZones.MAX_CORNER_SIZE_DP.toFloat(),
                                interactionSource = cornerSizeSliderInteraction,
                                modifier = Modifier.testTag("slider_corner_zone_size")
                            )
                        }

                        if (anyEdgeZoneEnabled || anyCornerZoneEnabled) {
                            Text(
                                stringResource(R.string.edge_zone_preview_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            EdgeZonePreview(
                                leftEnabled = editableLeftEdgeZoneEnabled,
                                rightEnabled = editableRightEdgeZoneEnabled,
                                edgeWidthDp = editableEdgeZoneWidthDp.toInt(),
                                topOffsetPercent = editableEdgeZoneTopOffsetPercent.toInt(),
                                bottomOffsetPercent = editableEdgeZoneBottomOffsetPercent.toInt(),
                                topLeftCornerEnabled = editableTopLeftCornerZoneEnabled,
                                topRightCornerEnabled = editableTopRightCornerZoneEnabled,
                                bottomLeftCornerEnabled = editableBottomLeftCornerZoneEnabled,
                                bottomRightCornerEnabled = editableBottomRightCornerZoneEnabled,
                                cornerSizeDp = editableCornerZoneSizeDp.toInt()
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.vibrate_on_lock_label))
                        Switch(
                            checked = vibrateOnLock,
                            onCheckedChange = { isChecked ->
                                vibrateOnLock = isChecked
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit {
                                        putBoolean(vibrateOnLockKey, isChecked)
                                    }
                            },
                            modifier = Modifier.testTag("switch_vibrate")
                        )
                    }

                    if (vibrateOnLock) {
                        val vibrationPatternKey = stringResource(R.string.vibration_pattern)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = vibrationPattern == VibrationPattern.LIGHT,
                                onClick = {
                                    vibrationPattern = VibrationPattern.LIGHT
                                    context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                        .edit { putString(vibrationPatternKey, VibrationPattern.LIGHT.name) }
                                },
                                label = { Text(stringResource(R.string.vibration_light)) },
                                modifier = Modifier.testTag("chip_light")
                            )
                            FilterChip(
                                selected = vibrationPattern == VibrationPattern.MEDIUM,
                                onClick = {
                                    vibrationPattern = VibrationPattern.MEDIUM
                                    context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                        .edit { putString(vibrationPatternKey, VibrationPattern.MEDIUM.name) }
                                },
                                label = { Text(stringResource(R.string.vibration_medium)) },
                                modifier = Modifier.testTag("chip_medium")
                            )
                            FilterChip(
                                selected = vibrationPattern == VibrationPattern.STRONG,
                                onClick = {
                                    vibrationPattern = VibrationPattern.STRONG
                                    context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                        .edit { putString(vibrationPatternKey, VibrationPattern.STRONG.name) }
                                },
                                label = { Text(stringResource(R.string.vibration_strong)) },
                                modifier = Modifier.testTag("chip_strong")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
                    val widgetPinSupported = remember { appWidgetManager.isRequestPinAppWidgetSupported }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (widgetCount == 0) {
                                stringResource(R.string.widget_status_none)
                            } else {
                                pluralStringResource(
                                    R.plurals.widget_count_label,
                                    widgetCount,
                                    widgetCount
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                if (!widgetPinSupported) {
                                    TapLockFeedback.showWidgetPinUnsupported(context)
                                } else {
                                    val requested = appWidgetManager.requestPinAppWidget(
                                        ComponentName(context, TapLockWidgetProvider::class.java),
                                        null,
                                        null
                                    )
                                    if (!requested) {
                                        TapLockFeedback.showWidgetPinUnsupported(context)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (widgetCount == 0) {
                                    stringResource(R.string.add_widget_button)
                                } else {
                                    stringResource(R.string.add_another_widget_button)
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.quick_settings_tile_label))
                        Text(
                            stringResource(R.string.quick_settings_tile_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isTileAdded) {
                                    stringResource(R.string.quick_settings_tile_added_status)
                                } else {
                                    stringResource(R.string.quick_settings_tile_missing_status)
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { requestQuickSettingsTilePrompt() },
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isTileAdded
                            ) {
                                Text(
                                    if (isTileAdded) {
                                        stringResource(R.string.enabled)
                                    } else {
                                        stringResource(R.string.quick_settings_tile_button)
                                    }
                                )
                            }
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Text(
                                stringResource(R.string.quick_settings_tile_manual_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.show_widget_icon_label))
                        Switch(
                            checked = showIcon,
                            onCheckedChange = { isChecked ->
                                showIcon = isChecked
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit { putBoolean(showWidgetIconKey, isChecked) }
                                refreshWidgets()
                            },
                            enabled = isAccessibilityEnabled,
                            modifier = Modifier.testTag("switch_show_icon")
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.widget_style_label))
                        Text(
                            stringResource(R.string.widget_style_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TapLockWidgetStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = widgetStyle == style,
                                    onClick = {
                                        widgetStyle = style
                                        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                            .edit { putString(widgetStyleKey, style.name) }
                                        refreshWidgets()
                                    },
                                    label = { Text(stringResource(style.labelResId)) },
                                    modifier = Modifier.testTag("chip_widget_style_${style.name.lowercase(Locale.US)}")
                                )
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.widget_preview_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    WidgetStylePreview(
                        style = widgetStyle,
                        showIcon = showIcon,
                        iconBitmap = widgetIconBitmap ?: defaultAppIconBitmap,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showIcon) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.select_icon))
                            }
                            Button(
                                onClick = {
                                    val file = File(context.filesDir, "custom_widget_icon.png")
                                    if (file.exists()) {
                                        file.delete()
                                        Toast.makeText(context, customIconResetMsg, Toast.LENGTH_SHORT).show()
                                        widgetIconBitmap = null
                                        refreshWidgets()
                                        restartFloatingButtonServiceIfRunning()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.reset_icon))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.status_bar_double_tap_label))
                            Text(
                                stringResource(R.string.status_bar_double_tap_description),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = statusBarDoubleTap,
                            onCheckedChange = { isChecked ->
                                statusBarDoubleTap = isChecked
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit {
                                        putBoolean(statusBarDoubleTapKey, isChecked)
                                    }
                            },
                            enabled = isAccessibilityEnabled,
                            modifier = Modifier.testTag("switch_status_bar")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.lock_screen_double_tap_label))
                            Text(
                                stringResource(R.string.lock_screen_double_tap_description),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = lockScreenDoubleTap,
                            onCheckedChange = { isChecked ->
                                lockScreenDoubleTap = isChecked
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit {
                                        putBoolean(lockScreenDoubleTapKey, isChecked)
                                    }
                            },
                            enabled = isAccessibilityEnabled,
                            modifier = Modifier.testTag("switch_lock_screen")
                        )
                    }

                    if (lockScreenDoubleTap) {
                        val lockZoneKey = stringResource(R.string.lock_zone_percent)
                        Text(
                            stringResource(R.string.lock_zone_label, lockZonePercent.toInt()),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = lockZonePercent,
                            onValueChange = { lockZonePercent = it },
                            onValueChangeFinished = {
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit { putInt(lockZoneKey, lockZonePercent.toInt()) }
                            },
                            valueRange = 20f..100f,
                            steps = 15,
                            modifier = Modifier.testTag("slider_lock_zone")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.floating_button_label))
                            Text(
                                stringResource(R.string.floating_button_description),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = floatingButtonEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !Settings.canDrawOverlays(context)) {
                                    TapLockFeedback.showOverlayPermissionRequired(context)
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            "package:${context.packageName}".toUri()
                                        )
                                    )
                                    return@Switch
                                }
                                floatingButtonEnabled = isChecked
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit { putBoolean(floatingButtonKey, isChecked) }
                                val serviceIntent = Intent(context, FloatingButtonService::class.java)
                                if (isChecked) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.stopService(serviceIntent)
                                }
                            },
                            enabled = isAccessibilityEnabled,
                            modifier = Modifier.testTag("switch_floating_button")
                        )
                    }

                    if (floatingButtonEnabled) {
                        Text(
                            stringResource(R.string.floating_button_preview_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FloatingButtonPreview(
                            iconBitmap = widgetIconBitmap ?: defaultAppIconBitmap,
                            sizeDp = floatingButtonSizeDp,
                            opacityPercent = floatingButtonOpacityPercent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(R.string.floating_button_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            stringResource(
                                R.string.floating_button_size_label,
                                floatingButtonSizeDp.toInt()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = floatingButtonSizeDp,
                            onValueChange = { floatingButtonSizeDp = it },
                            onValueChangeFinished = {
                                val clamped = TapLockFloatingButtonConfig.clampSizeDp(
                                    floatingButtonSizeDp.toInt()
                                )
                                floatingButtonSizeDp = clamped.toFloat()
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit { putInt(floatingButtonSizeDpKey, clamped) }
                                restartFloatingButtonServiceIfRunning()
                            },
                            valueRange = TapLockFloatingButtonConfig.MIN_SIZE_DP.toFloat()..
                                TapLockFloatingButtonConfig.MAX_SIZE_DP.toFloat(),
                            steps = TapLockFloatingButtonConfig.MAX_SIZE_DP -
                                TapLockFloatingButtonConfig.MIN_SIZE_DP - 1,
                            modifier = Modifier.testTag("slider_floating_button_size")
                        )

                        Text(
                            stringResource(
                                R.string.floating_button_opacity_label,
                                floatingButtonOpacityPercent.toInt()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = floatingButtonOpacityPercent,
                            onValueChange = { floatingButtonOpacityPercent = it },
                            onValueChangeFinished = {
                                val clamped = TapLockFloatingButtonConfig.clampOpacityPercent(
                                    floatingButtonOpacityPercent.toInt()
                                )
                                floatingButtonOpacityPercent = clamped.toFloat()
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit { putInt(floatingButtonOpacityPercentKey, clamped) }
                                restartFloatingButtonServiceIfRunning()
                            },
                            valueRange = TapLockFloatingButtonConfig.MIN_OPACITY_PERCENT.toFloat()..
                                TapLockFloatingButtonConfig.MAX_OPACITY_PERCENT.toFloat(),
                            steps = TapLockFloatingButtonConfig.MAX_OPACITY_PERCENT -
                                TapLockFloatingButtonConfig.MIN_OPACITY_PERCENT - 1,
                            modifier = Modifier.testTag("slider_floating_button_opacity")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.app_exclusions_label))
                        Text(
                            stringResource(R.string.app_exclusions_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                appSearchQuery = ""
                                showAppExclusionDialog = true
                            },
                            enabled = isAccessibilityEnabled,
                            modifier = Modifier.testTag("button_app_exclusions")
                        ) {
                            Text(stringResource(R.string.app_exclusions_button))
                        }
                        Text(
                            if (excludedPackages.isEmpty()) {
                                excludedAppsSummary
                            } else {
                                buildString {
                                    append(
                                        context.resources.getQuantityString(
                                            R.plurals.app_exclusions_count,
                                            excludedPackages.size,
                                            excludedPackages.size
                                        )
                                    )
                                    append('\n')
                                    append(excludedAppsSummary)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Lock delay dropdown
                    val lockDelayKey = stringResource(R.string.lock_delay_ms)
                    val delayOptions = listOf(
                        0 to stringResource(R.string.lock_delay_none),
                        500 to stringResource(R.string.lock_delay_half),
                        1000 to stringResource(R.string.lock_delay_one),
                        2000 to stringResource(R.string.lock_delay_two)
                    )
                    var delayDropdownExpanded by remember { mutableStateOf(false) }
                    val selectedDelayLabel = delayOptions.first { it.first == lockDelayMs }.second

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.lock_delay_label),
                            modifier = Modifier.weight(1f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = delayDropdownExpanded,
                            onExpandedChange = { delayDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedDelayLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = delayDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .size(width = 120.dp, height = 52.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            DropdownMenu(
                                expanded = delayDropdownExpanded,
                                onDismissRequest = { delayDropdownExpanded = false }
                            ) {
                                delayOptions.forEach { (ms, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            lockDelayMs = ms
                                            delayDropdownExpanded = false
                                            context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                                .edit { putInt(lockDelayKey, ms) }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Tap-to-test area
                    DoubleTapTestArea(timeoutMs = timeoutValue.toInt())

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Lock counter
                    Text(
                        pluralStringResource(R.plurals.lock_count_label, lockCount, lockCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            }

            if (showLiveZoneOverlay) {
                EdgeZoneLiveOverlay(
                    leftEnabled = editableLeftEdgeZoneEnabled,
                    rightEnabled = editableRightEdgeZoneEnabled,
                    edgeWidthDp = editableEdgeZoneWidthDp.toInt(),
                    topOffsetPercent = editableEdgeZoneTopOffsetPercent.toInt(),
                    bottomOffsetPercent = editableEdgeZoneBottomOffsetPercent.toInt(),
                    topLeftCornerEnabled = editableTopLeftCornerZoneEnabled,
                    topRightCornerEnabled = editableTopRightCornerZoneEnabled,
                    bottomLeftCornerEnabled = editableBottomLeftCornerZoneEnabled,
                    bottomRightCornerEnabled = editableBottomRightCornerZoneEnabled,
                    cornerSizeDp = editableCornerZoneSizeDp.toInt()
                )
            }
        }

        if (showAppExclusionDialog) {
            AppExclusionDialog(
                apps = availableApps,
                excludedPackages = excludedPackages,
                searchQuery = appSearchQuery,
                isLoading = isLoadingApps,
                onSearchQueryChange = { appSearchQuery = it },
                onTogglePackage = { packageName ->
                    val updatedPackages = excludedPackages.toMutableSet().apply {
                        if (!add(packageName)) {
                            remove(packageName)
                        }
                    }.toSet()
                    excludedPackages = updatedPackages
                    TapLockAppRules.setExcludedPackages(context, updatedPackages)
                },
                onClearAll = {
                    excludedPackages = emptySet()
                    TapLockAppRules.setExcludedPackages(context, emptySet())
                },
                onDismiss = { showAppExclusionDialog = false }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.accessibility_permission_title)) },
                text = { Text(stringResource(R.string.accessibility_permission_description)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    ) {
                        Text(stringResource(R.string.agree))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text(stringResource(R.string.not_now))
                    }
                }
            )
        }

        if (showOnboarding) {
            val onboardingTitle = when (onboardingStep) {
                0 -> stringResource(R.string.onboarding_welcome_title)
                1 -> stringResource(R.string.onboarding_accessibility_title)
                2 -> stringResource(R.string.onboarding_widget_title)
                else -> stringResource(R.string.onboarding_done_title)
            }
            val onboardingBody = when (onboardingStep) {
                0 -> stringResource(R.string.onboarding_welcome_body)
                1 -> stringResource(R.string.onboarding_accessibility_body)
                2 -> stringResource(R.string.onboarding_widget_body)
                else -> stringResource(R.string.onboarding_done_body)
            }

            AlertDialog(
                onDismissRequest = {},
                title = { Text(onboardingTitle) },
                text = { Text(onboardingBody) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            when (onboardingStep) {
                                1 -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                3 -> {
                                    showOnboarding = false
                                    context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                        .edit { putBoolean(hasCompletedOnboardingKey, true) }
                                }
                            }
                            if (onboardingStep < 3) onboardingStep++
                        }
                    ) {
                        Text(
                            when (onboardingStep) {
                                1 -> stringResource(R.string.onboarding_open_settings)
                                3 -> stringResource(R.string.onboarding_done)
                                else -> stringResource(R.string.onboarding_next)
                            }
                        )
                    }
                },
                dismissButton = {
                    if (onboardingStep < 3) {
                        TextButton(
                            onClick = {
                                showOnboarding = false
                                context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                    .edit { putBoolean(hasCompletedOnboardingKey, true) }
                            }
                        ) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AppExclusionDialog(
    apps: List<TapLockAppInfo>,
    excludedPackages: Set<String>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTogglePackage: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val filteredApps = if (searchQuery.isBlank()) {
        apps
    } else {
        apps.filter { app ->
            app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_exclusions_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(stringResource(R.string.app_exclusions_search_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_app_exclusions_search")
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.app_exclusions_loading),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.app_exclusions_no_results),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredApps.forEach { app ->
                                AppExclusionRow(
                                    app = app,
                                    checked = excludedPackages.contains(app.packageName),
                                    onToggle = { onTogglePackage(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_exclusions_done))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClearAll,
                enabled = excludedPackages.isNotEmpty(),
                modifier = Modifier.testTag("button_app_exclusions_clear")
            ) {
                Text(stringResource(R.string.app_exclusions_clear))
            }
        }
    )
}

@Composable
private fun EdgeZonePreview(
    leftEnabled: Boolean,
    rightEnabled: Boolean,
    edgeWidthDp: Int,
    topOffsetPercent: Int,
    bottomOffsetPercent: Int,
    topLeftCornerEnabled: Boolean,
    topRightCornerEnabled: Boolean,
    bottomLeftCornerEnabled: Boolean,
    bottomRightCornerEnabled: Boolean,
    cornerSizeDp: Int
) {
    val previewZoneWidth = (
        6f + (edgeWidthDp.toFloat() / TapLockEdgeZones.MAX_WIDTH_DP.toFloat()) * 18f
        ).dp
    val previewCornerSize = (
        8f + (cornerSizeDp.toFloat() / TapLockEdgeZones.MAX_CORNER_SIZE_DP.toFloat()) * 24f
        ).dp
    val topOffsetFraction = (topOffsetPercent / 100f)
        .coerceIn(
            TapLockEdgeZones.MIN_OFFSET_PERCENT / 100f,
            TapLockEdgeZones.MAX_OFFSET_PERCENT / 100f
        )
    val bottomOffsetFraction = (bottomOffsetPercent / 100f)
        .coerceIn(
            TapLockEdgeZones.MIN_OFFSET_PERCENT / 100f,
            TapLockEdgeZones.MAX_OFFSET_PERCENT / 100f
        )
    val zoneColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val frameColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val shape = RoundedCornerShape(24.dp)
    val previewWidth = 120.dp
    val previewHeight = 220.dp
    val previewVerticalPadding = 16.dp
    val previewInnerHeight = 188.dp
    val topInset = previewInnerHeight * topOffsetFraction
    val bottomInset = previewInnerHeight * bottomOffsetFraction

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .border(1.dp, frameColor, shape)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    shape
                )
                .padding(vertical = previewVerticalPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topInset, bottom = bottomInset)
                ) {
                    if (leftEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(previewZoneWidth)
                                .background(
                                    zoneColor,
                                    RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                )
                        )
                    }

                    if (rightEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(previewZoneWidth)
                                .background(
                                    zoneColor,
                                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                )
                        )
                    }
                }

                if (topLeftCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(bottomEnd = 12.dp)
                            )
                    )
                }

                if (topRightCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(bottomStart = 12.dp)
                            )
                    )
                }

                if (bottomLeftCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topEnd = 12.dp)
                            )
                    )
                }

                if (bottomRightCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topStart = 12.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStylePreview(
    style: TapLockWidgetStyle,
    showIcon: Boolean,
    iconBitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val fillColor = when (style) {
        TapLockWidgetStyle.TRANSPARENT -> Color.Transparent
        TapLockWidgetStyle.GLASS -> Color(0x660F172A)
        TapLockWidgetStyle.SOLID -> Color(0xDD111827)
        TapLockWidgetStyle.OUTLINE -> Color.Transparent
    }
    val borderColor = when (style) {
        TapLockWidgetStyle.GLASS -> Color.White.copy(alpha = 0.18f)
        TapLockWidgetStyle.OUTLINE -> Color.White.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(172.dp)
                .height(84.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fillColor, shape)
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(1.dp, borderColor, shape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showIcon) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingButtonPreview(
    iconBitmap: ImageBitmap,
    sizeDp: Float,
    opacityPercent: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .alpha(opacityPercent / 100f)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size((sizeDp * 0.68f).dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun EdgeZoneLiveOverlay(
    leftEnabled: Boolean,
    rightEnabled: Boolean,
    edgeWidthDp: Int,
    topOffsetPercent: Int,
    bottomOffsetPercent: Int,
    topLeftCornerEnabled: Boolean,
    topRightCornerEnabled: Boolean,
    bottomLeftCornerEnabled: Boolean,
    bottomRightCornerEnabled: Boolean,
    cornerSizeDp: Int
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edge_zone_live_overlay")
    ) {
        val topInset = maxHeight * (topOffsetPercent / 100f)
        val bottomInset = maxHeight * (bottomOffsetPercent / 100f)
        val zoneColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        val zoneBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.06f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset, bottom = bottomInset)
            ) {
                if (leftEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(edgeWidthDp.dp)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = zoneBorderColor,
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                    )
                }

                if (rightEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(edgeWidthDp.dp)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = zoneBorderColor,
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                    )
                }
            }

            if (topLeftCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(bottomEnd = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(bottomEnd = 20.dp)
                        )
                )
            }

            if (topRightCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(bottomStart = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(bottomStart = 20.dp)
                        )
                )
            }

            if (bottomLeftCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(topEnd = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(topEnd = 20.dp)
                        )
                )
            }

            if (bottomRightCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(topStart = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(topStart = 20.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun AppExclusionRow(
    app: TapLockAppInfo,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp)
            .testTag("app_exclusion_row_${app.packageName}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    disclaimerString: androidx.compose.ui.text.AnnotatedString
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.info_section_header),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.home_screen_description),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(R.string.home_screen_instructions),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        disclaimerString,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DoubleTapTestArea(timeoutMs: Int) {
    val detector = remember { DoubleTapDetector() }
    var showSuccess by remember { mutableStateOf(false) }
    val borderColor = if (showSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1000)
            showSuccess = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(timeoutMs) {
                detectTapGestures {
                    if (detector.onTap(timeoutMs)) {
                        showSuccess = true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showSuccess) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    stringResource(R.string.tap_test_success),
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Text(
                stringResource(R.string.tap_test_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
