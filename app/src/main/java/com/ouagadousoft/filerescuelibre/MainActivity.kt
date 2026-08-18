package com.ouagadousoft.filerescuelibre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ouagadousoft.filerescuelibre.ui.screens.HomeScreen
import com.ouagadousoft.filerescuelibre.ui.screens.NoRootScreen
import com.ouagadousoft.filerescuelibre.ui.theme.FileRescueLibreTheme
import com.ouagadousoft.filerescuelibre.viewmodel.RootState
import com.ouagadousoft.filerescuelibre.viewmodel.RootViewModel

class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileRescueLibreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val rootState by rootViewModel.rootState.collectAsState()
                    when (rootState) {
                        RootState.Checking -> CheckingRootScreen()
                        RootState.Granted -> HomeScreen()
                        RootState.Denied -> NoRootScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckingRootScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.root_check_checking))
    }
}
