package com.ah.taplock

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ah.taplock.ui.theme.TapLockTheme
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TapLockScreen()
                }
            }
        }
    }

}

@Composable
fun TapLockScreen() {
    val context = LocalContext.current
    val githubUrl = stringResource(R.string.github_url)
    val sharedPrefName = stringResource(R.string.shared_pref_name)
    val doubleTapTimeoutKey = stringResource(R.string.double_tap_timeout)
    val timeoutUpdatedMsg = stringResource(R.string.timeout_updated)
    val showWidgetIconKey = stringResource(R.string.show_widget_icon)

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var showDialog by remember { mutableStateOf(false) } //

    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityEnabled(context))
    }

    var timeoutValue by remember { mutableStateOf("") }
    var showIcon by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        timeoutValue = prefs.getInt(doubleTapTimeoutKey, 300).toString()
        showIcon = prefs.getBoolean(showWidgetIconKey, false)
    }

    val uriHandler = LocalUriHandler.current
    val disclaimerString = buildAnnotatedString {
        append(stringResource(R.string.home_screen_disclaimer))
        append(" ")
        pushStringAnnotation(tag="URL", annotation = githubUrl)
        withStyle(style = SpanStyle(
            textDecoration = TextDecoration.Underline
        )) {
            append(githubUrl)
        }
        pop()
    }

    DisposableEffect(Unit) {
        val lifecycleOwner = context as ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.home_screen_description),
                    style = MaterialTheme.typography.bodyLarge
                )
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
                    stringResource(R.string.home_screen_instructions),
                    style = MaterialTheme.typography.bodyLarge
                )
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
                    disclaimerString,
                    modifier = Modifier.clickable {
                        uriHandler.openUri(githubUrl)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
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
                    stringResource(R.string.required_permissions),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    stringResource(R.string.settings_label),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        label = { Text(stringResource(R.string.timeout_label)) },
                        value = timeoutValue,
                        onValueChange = { newValue: String ->
                            timeoutValue = newValue
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        trailingIcon = {
                            if (timeoutValue.isNotEmpty()) {
                                IconButton(onClick = { timeoutValue = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_value))
                                }
                            }
                        },
                    )

                    Button(
                        enabled = timeoutValue.isNotEmpty(),
                        onClick = {
                            context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                                .edit {
                                    putInt(
                                        doubleTapTimeoutKey,
                                        timeoutValue.toInt()
                                    )
                                }
                            Toast.makeText(context, timeoutUpdatedMsg, Toast.LENGTH_SHORT).show()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ) {
                        Text(stringResource(R.string.update))
                    }
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

                            // Update Widgets
                            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TapLockWidgetProvider::class.java))
                            val intent = Intent(context, TapLockWidgetProvider::class.java).apply {
                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                            }
                            context.sendBroadcast(intent)
                        }
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
    }
}