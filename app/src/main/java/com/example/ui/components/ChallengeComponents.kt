package com.example.ui.components

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.ChallengeSchedule
import com.example.data.model.DailyLog
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.ui.state.DayBarStat

@Composable
fun CompletionRing(
  percent: Int,
  size: Dp = 84.dp,
  strokeWidth: Dp = 8.dp,
  trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  progressColor: Color = MaterialTheme.colorScheme.primary,
  label: String? = null,
) {
  val animatedSweep by animateFloatAsState(
    targetValue = (percent.coerceIn(0, 100) / 100f) * 360f,
    animationSpec = tween(durationMillis = 450),
    label = "completionSweep",
  )

  Box(
    modifier = Modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = strokeWidth.toPx()
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
      drawArc(
        color = progressColor,
        startAngle = -90f,
        sweepAngle = animatedSweep,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$percent%",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (label != null) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun DayNavigatorCard(
  selectedDayNumber: Int,
  logsByDay: Map<Int, DailyLog>,
  totalHabitsCount: Int,
  onSelectDay: (Int) -> Unit,
) {
  val dayInfo = ChallengeSchedule.dayInfoFor(selectedDayNumber)
  val scrollState = rememberScrollState()

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = { if (selectedDayNumber > 1) onSelectDay(selectedDayNumber - 1) },
          enabled = selectedDayNumber > 1,
          modifier = Modifier.testTag("prev_day_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Previous Day",
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Day ${dayInfo.dayNumber} of 30 • Week ${dayInfo.weekIndex}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = dayInfo.fullDateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = { if (selectedDayNumber < ChallengeSchedule.TOTAL_DAYS) onSelectDay(selectedDayNumber + 1) },
          enabled = selectedDayNumber < ChallengeSchedule.TOTAL_DAYS,
          modifier = Modifier.testTag("next_day_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Next Day",
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val denom = totalHabitsCount.coerceAtLeast(1)
        ChallengeSchedule.allDays.forEach { d ->
          val isSelected = d.dayNumber == selectedDayNumber
          val doneCount = logsByDay[d.dayNumber]?.completedHabitIds?.size ?: 0
          val pct = ((doneCount * 100) / denom).coerceIn(0, 100)

          val pillColor = when {
            isSelected -> MaterialTheme.colorScheme.primary
            pct >= 80 -> MaterialTheme.colorScheme.primaryContainer
            pct > 0 -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
          }
          val textColor = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            pct >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
            pct > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }

          Surface(
            color = pillColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSelectDay(d.dayNumber) }
              .testTag("day_pill_${d.dayNumber}"),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "D${d.dayNumber}",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
              )
              Text(
                text = d.shortDateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.85f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SevenDayPerformanceChart(
  days: List<DayBarStat>,
  selectedDayNumber: Int,
  onSelectDay: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val accentColor = MaterialTheme.colorScheme.secondary
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(156.dp),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        if (days.isEmpty()) return@Canvas
        val count = days.size
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.46f).coerceAtMost(36.dp.toPx())
        val maxBarHeight = size.height - 16.dp.toPx()

        days.forEachIndexed { idx, stat ->
          val left = idx * slotWidth + (slotWidth - barWidth) / 2f
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(left, 8.dp.toPx()),
            size = Size(barWidth, maxBarHeight),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          )
          val fillH = (maxBarHeight * (stat.percent / 100f)).coerceAtLeast(if (stat.percent > 0) 6.dp.toPx() else 0f)
          if (fillH > 0f) {
            val top = 8.dp.toPx() + (maxBarHeight - fillH)
            drawRoundRect(
              color = if (stat.dayNumber == selectedDayNumber) accentColor else primaryColor,
              topLeft = Offset(left, top),
              size = Size(barWidth, fillH),
              cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      days.forEach { stat ->
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelectDay(stat.dayNumber) }
            .padding(vertical = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "${stat.percent}%",
            style = MaterialTheme.typography.labelSmall,
            color = if (stat.dayNumber == selectedDayNumber) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = stat.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = stat.dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitDialog(
  initialHabit: Habit?,
  onDismiss: () -> Unit,
  onConfirm: (title: String, category: HabitCategory, targetDetail: String) -> Unit,
) {
  var title by remember { mutableStateOf(initialHabit?.title ?: "") }
  var targetDetail by remember { mutableStateOf(initialHabit?.targetDetail ?: "") }
  var selectedCategory by remember {
    mutableStateOf(HabitCategory.fromWire(initialHabit?.category))
  }
  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initialHabit == null) "Add Daily Habit" else "Edit Habit")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Habit Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_title_input"),
        )
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = !expanded },
        ) {
          OutlinedTextField(
            value = selectedCategory.wireValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
          ) {
            HabitCategory.entries.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.wireValue) },
                onClick = {
                  selectedCategory = cat
                  expanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = targetDetail,
          onValueChange = { targetDetail = it },
          label = { Text("Target / Timing (Optional)") },
          placeholder = { Text("e.g. 20 Minutes, 8 PM BD Time") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title.trim(), selectedCategory, targetDetail.trim()) },
        enabled = title.trim().isNotEmpty(),
        modifier = Modifier.testTag("confirm_habit_button"),
      ) {
        Text(if (initialHabit == null) "Add" else "Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun AddReminderDialog(
  onDismiss: () -> Unit,
  onConfirm: (title: String, targetText: String, hour: Int, minute: Int) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var targetText by remember { mutableStateOf("") }
  var hour by remember { mutableIntStateOf(20) }
  var minute by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Daily Reminder") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Reminder Title") },
          placeholder = { Text("e.g. LinkedIn Video Post") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = targetText,
          onValueChange = { targetText = it },
          label = { Text("Target Note") },
          placeholder = { Text("e.g. 8 PM Bangladesh Time") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hour (0–23)", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = { hour = (hour + 23) % 24 }) { Text("−") }
              Text(
                text = hour.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium,
              )
              TextButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
            }
          }
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Minute", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = { minute = (minute + 55) % 60 }) { Text("−") }
              Text(
                text = minute.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium,
              )
              TextButton(onClick = { minute = (minute + 5) % 60 }) { Text("+") }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title.trim(), targetText.trim(), hour, minute) },
        enabled = title.trim().isNotEmpty(),
      ) {
        Text("Save Reminder")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun ChallengeFooter(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(width = 40.dp, height = 3.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Designed for Nasrullah",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Text(
      text = "Nasrullah's 30-Day Challenge • 17 Nov – 16 Dec 2026",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
      textAlign = TextAlign.Center,
    )
  }
}

fun printChallengeReportHtml(
  context: Context,
  htmlContent: String,
  onError: (String) -> Unit,
) {
  try {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
      override fun onPageFinished(view: WebView?, url: String?) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null && view != null) {
          val jobName = "Nasrullah_30_Day_Challenge_Report"
          val adapter = view.createPrintDocumentAdapter(jobName)
          printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        }
      }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
  } catch (e: Exception) {
    onError(e.localizedMessage ?: "Printing is unavailable on this device.")
  }
}
