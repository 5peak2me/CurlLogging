package io.github.speak2me.kmp.logging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
@Preview
fun App(viewModel: AppViewModel = viewModel()) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = viewModel::httpDELETE) {
                Text("DELETE")
            }
            Button(onClick = viewModel::httpGET) {
                Text("GET")
            }
            Button(onClick = viewModel::httpPATCH) {
                Text("PATCH")
            }
            Button(onClick = viewModel::httpPOST) {
                Text("POST")
            }
            Button(onClick = viewModel::httpPUT) {
                Text("PUT")
            }
        }
    }
}