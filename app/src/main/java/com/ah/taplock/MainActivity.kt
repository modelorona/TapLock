package com.ah.taplock

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ah.taplock.ui.theme.TapLockTheme
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

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
    val vibrateOnLockKey = stringResource(R.string.vibrate_on_lock)
    val statusBarDoubleTapKey = stringResource(R.string.status_bar_double_tap)
    val lockScreenDoubleTapKey = stringResource(R.string.lock_screen_double_tap)
    val customIconUpdatedMsg = stringResource(R.string.custom_icon_updated)
    val customIconResetMsg = stringResource(R.string.custom_icon_reset)
    val hasSeenInfoKey = stringResource(R.string.has_seen_info)
    val hasCompletedOnboardingKey = stringResource(R.string.has_completed_onboarding)
    val lockDelayMsKey = stringResource(R.string.lock_delay_ms)
    val lockCountKey = stringResource(R.string.lock_count)
    val lockZonePercentKey = stringResource(R.string.lock_zone_percent)

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
    var vibrateOnLock by remember { mutableStateOf(true) }
    var vibrationPattern by remember { mutableStateOf(VibrationPattern.MEDIUM) }
    var statusBarDoubleTap by remember { mutableStateOf(false) }
    var lockScreenDoubleTap by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(0) }
    var lockDelayMs by remember { mutableIntStateOf(0) }
    var lockCount by remember { mutableIntStateOf(0) }
    var lockZonePercent by remember { mutableFloatStateOf(66f) }
    var widgetIconBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        timeoutValue = prefs.getInt(doubleTapTimeoutKey, 300).toFloat()
        showIcon = prefs.getBoolean(showWidgetIconKey, false)
        vibrateOnLock = prefs.getBoolean(vibrateOnLockKey, true)
        vibrationPattern = VibrationHelper.fromPrefs(context)
        statusBarDoubleTap = prefs.getBoolean(statusBarDoubleTapKey, false)
        lockScreenDoubleTap = prefs.getBoolean(lockScreenDoubleTapKey, false)
        infoExpanded = !prefs.getBoolean(hasSeenInfoKey, false)
        showOnboarding = !prefs.getBoolean(hasCompletedOnboardingKey, false)
        lockDelayMs = prefs.getInt(lockDelayMsKey, 0)
        lockCount = prefs.getInt(lockCountKey, 0)
        lockZonePercent = prefs.getInt(lockZonePercentKey, 66).toFloat()
        // Load custom icon for preview
        val iconFile = File(context.filesDir, "custom_widget_icon.png")
        widgetIconBitmap = if (iconFile.exists()) {
            BitmapFactory.decodeFile(iconFile.absolutePath)?.asImageBitmap()
        } else null
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
                            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TapLockWidgetProvider::class.java))
                            val intent = Intent(context, TapLockWidgetProvider::class.java).apply {
                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                            }
                            context.sendBroadcast(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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

    DisposableEffect(Unit) {
        val lifecycleOwner = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityEnabled(context)
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
                val prefs = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                lockCount = prefs.getInt(lockCountKey, 0)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
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

                    val hasNoWidgets = remember {
                        AppWidgetManager.getInstance(context).getAppWidgetIds(
                            ComponentName(context, TapLockWidgetProvider::class.java)
                        ).isEmpty()
                    }
                    if (hasNoWidgets) {
                        Button(
                            onClick = {
                                val appWidgetManager = AppWidgetManager.getInstance(context)
                                val widgetComponent = ComponentName(context, TapLockWidgetProvider::class.java)
                                appWidgetManager.requestPinAppWidget(widgetComponent, null, null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.add_widget_button))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

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
                                    .edit {
                                        putBoolean(showWidgetIconKey, isChecked)
                                    }

                                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TapLockWidgetProvider::class.java))
                                val intent = Intent(context, TapLockWidgetProvider::class.java).apply {
                                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                                }
                                context.sendBroadcast(intent)
                            },
                            enabled = isAccessibilityEnabled,
                            modifier = Modifier.testTag("switch_show_icon")
                        )
                    }

                    if (showIcon) {
                        // Widget icon preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.widget_preview_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val previewBitmap = widgetIconBitmap ?: remember {
                                val drawable = context.packageManager.getApplicationIcon(context.packageName)
                                val bmp = createBitmap(
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight
                                )
                                val canvas = android.graphics.Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bmp.asImageBitmap()
                            }
                            Image(
                                bitmap = previewBitmap,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

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
                                        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TapLockWidgetProvider::class.java))
                                        val intent = Intent(context, TapLockWidgetProvider::class.java).apply {
                                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                                        }
                                        context.sendBroadcast(intent)
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
                            ExposedDropdownMenu(
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