package com.astral.unwm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.astral.unwm.ui.theme.AstralUNWMTheme
import com.astral.unwm.ui.web.ExtractorScreen
import com.astral.unwm.ui.web.RemoverScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstralUNWMTheme {
                var currentSection by remember { mutableStateOf(AstralSection.Remover) }
                val sections = listOf(AstralSection.Remover, AstralSection.Extractor)
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(text = currentSection.title) }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            sections.forEach { section ->
                                val selected = section == currentSection
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentSection = section },
                                    icon = {
                                        Icon(
                                            imageVector = section.icon,
                                            contentDescription = section.title
                                        )
                                    },
                                    label = { Text(section.title) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (currentSection) {
                        AstralSection.Remover -> RemoverScreen(modifier = Modifier.padding(padding))
                        AstralSection.Extractor -> ExtractorScreen(modifier = Modifier.padding(padding))
                    }
                }
            }
        }
    }
}

enum class AstralSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Remover("Remover", Icons.Rounded.Photo),
    Extractor("Extractor", Icons.Rounded.Build)
}
