package com.veyra.multispace

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MultiSpaceDashboard(
                    androidVersion = Build.VERSION.RELEASE ?: "نامشخص",
                    apiLevel = Build.VERSION.SDK_INT,
                    manufacturer = Build.MANUFACTURER ?: "نامشخص",
                    model = Build.MODEL ?: "نامشخص"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSpaceDashboard(
    androidVersion: String,
    apiLevel: Int,
    manufacturer: String,
    model: String
) {
    var selectedTab by remember { mutableStateOf(0) }
    var spaces by remember { mutableStateOf(listOf("فضای شخصی", "فضای آزمایشی")) }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("MultiSpace Ultimate", fontWeight = FontWeight.Bold)
                            Text("نسخه شخصی آزمایشی ۰٫۱٫۰", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    spaces = spaces + "فضای ${spaces.size + 1}"
                }) { Text("+") }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("⌂") },
                        label = { Text("داشبورد") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("▦") },
                        label = { Text("فضاها") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("⚙") },
                        label = { Text("سازگاری") }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        when (selectedTab) {
                            1 -> "مدیریت فضاها"
                            2 -> "گزارش دستگاه"
                            else -> "خوش آمدی"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (selectedTab == 2) {
                    item { InfoCard("سازنده", manufacturer) }
                    item { InfoCard("مدل دستگاه", model) }
                    item { InfoCard("نسخه اندروید", androidVersion) }
                    item { InfoCard("API Level", apiLevel.toString()) }
                    item {
                        Text(
                            "این گزارش فقط مشخصات دستگاه را می‌خواند؛ سازگاری موتور اجرای برنامه‌های شخص ثالث هنوز آزمایش نشده است.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("مدیریت فضاهای کاری", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("می‌توانی فضاهای مدیریتی بسازی. این نسخه هنوز برنامه‌های شخص ثالث را در محیط‌های ایزوله اجرا نمی‌کند.")
                            }
                        }
                    }
                    items(spaces.size) { index ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(spaces[index], Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = {
                                    spaces = spaces.filterIndexed { i, _ -> i != index }
                                }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
