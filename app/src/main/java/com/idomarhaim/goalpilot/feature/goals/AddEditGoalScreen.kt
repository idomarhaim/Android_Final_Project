package com.idomarhaim.goalpilot.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idomarhaim.goalpilot.domain.model.GoalCategory
import com.idomarhaim.goalpilot.ui.components.icon
import com.idomarhaim.goalpilot.ui.components.iconForKey
import com.idomarhaim.goalpilot.ui.components.toGoalAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    onDone: () -> Unit,
    viewModel: AddEditGoalViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val lifeAreas by viewModel.lifeAreas.collectAsStateWithLifecycle()

    LaunchedEffect(form.saved) {
        if (form.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEdit) "Edit goal" else "New goal") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalCategory.entries.toList()) { category ->
                    FilterChip(
                        selected = form.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(category.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon(),
                                contentDescription = null,
                                modifier = Modifier.padding(2.dp),
                            )
                        },
                    )
                }
            }

            // Which part of the user's life this goal belongs to. Separate from the
            // category above: the category is a fixed taxonomy the AI classifies
            // against, the life area is the user's own division of their life and
            // the unit the time-allocation chart reports on.
            Text("Life area", style = MaterialTheme.typography.labelLarge)
            if (lifeAreas.isEmpty()) {
                Text(
                    "No life areas yet — add them under Profile → Life areas, or sync " +
                        "them from your Google Tasks lists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = form.lifeAreaId == null,
                            onClick = { viewModel.onLifeAreaChange(null) },
                            label = { Text("None") },
                        )
                    }
                    items(lifeAreas) { area ->
                        FilterChip(
                            selected = form.lifeAreaId == area.id,
                            onClick = { viewModel.onLifeAreaChange(area.id) },
                            label = { Text(area.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = iconForKey(area.iconKey),
                                    contentDescription = null,
                                    tint = area.colorHex.toGoalAccent(),
                                    modifier = Modifier.padding(2.dp),
                                )
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.target,
                    onValueChange = viewModel::onTargetChange,
                    label = { Text("Target") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.unit,
                    onValueChange = viewModel::onUnitChange,
                    label = { Text("Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            form.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::save,
                enabled = !form.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (form.isEdit) "Save changes" else "Create goal")
                }
            }
        }
    }
}
