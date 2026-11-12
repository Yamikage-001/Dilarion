package com.dilarion.app.update

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dilarion.app.ui.theme.DilarionRed
import com.dilarion.app.ui.theme.TextSecondary

/** Covers the whole app. Back does nothing. It stays until the user taps Update. */
@Composable
fun UpdateRequiredScreen() {
    val info by AppUpdater.state.collectAsState()
    val update = info ?: return
    val activity = LocalContext.current as? Activity
    BackHandler { }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(DilarionRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.SystemUpdate, null, tint = DilarionRed, modifier = Modifier.size(32.dp))
            }
            Text("There is an update", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Version ${update.versionName}", color = TextSecondary, fontSize = 16.sp)
            update.message?.let {
                Text(it, color = Color(0xFFEF4444), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Button(
                onClick = { activity?.let { AppUpdater.beginUpdate(it) } },
                enabled = !update.busy && activity != null,
                colors = ButtonDefaults.buttonColors(containerColor = DilarionRed),
                modifier = Modifier.widthIn(min = 180.dp).height(48.dp),
            ) {
                if (update.busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
