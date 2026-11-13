package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ChallengeSchedule
import com.example.ui.components.ChallengeFooter
import com.example.ui.components.CompletionRing
import com.example.ui.state.ChallengeUiState
import java.util.Locale

@Composable
fun ReportsScreen(
  state: ChallengeUiState.Ready,
  onPrintReport: () -> Unit,
  onExportCsv: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Final 30-Day Report
  val analytics = state.analytics

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Header & Export Actions
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Nasrullah's Progress Reports",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Challenge Window: ${ChallengeSchedule.START_DISPLAY} – ${ChallengeSchedule.END_DISPLAY} (30 Days)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("weekly_report_tab"),
              ) {
                Text("Weekly Reports")
              }
              SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("final_report_tab"),
              ) {
                Text("Final 30-Day Report")
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onPrintReport,
                modifier = Modifier
                  .weight(1f)
                  .testTag("print_report_button"),
              ) {
                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Report")
              }
              OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier
                  .weight(1f)
                  .testTag("reports_export_csv_button"),
              ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
              }
            }
          }
        }
      }

      if (selectedTab == 0) {
        // Weekly Summaries
        items(analytics.weeklySummaries, key = { it.weekNumber }) { week ->
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(week.title, style = MaterialTheme.typography.titleMedium)
                  Text(
                    week.dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                CompletionRing(percent = week.avgPercent, size = 56.dp, strokeWidth = 6.dp)
              }

              LinearProgressIndicator(
                progress = { (week.avgPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(7.dp)
                  .clip(RoundedCornerShape(4.dp)),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Completed: ${week.completedChecks}/${week.possibleChecks}",
                  style = MaterialTheme.typography.bodySmall,
                )
                Text(
                  text = "Best: ${week.bestDayLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Study: ${String.format(Locale.US, "%.1f", week.studyHours)}h",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "Fiverr: ${String.format(Locale.US, "%.1f", week.fiverrHours)}h",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "Outreach: ${week.outreachTotal}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      } else {
        // Final 30-Day Report
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                CompletionRing(percent = analytics.overallPercent, size = 88.dp, label = "Final")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  val tier = when {
                    analytics.overallPercent >= 90 -> "Elite Discipline Master"
                    analytics.overallPercent >= 75 -> "Consistent Builder"
                    analytics.overallPercent >= 50 -> "Steady Momentum"
                    else -> "Challenge In Progress"
                  }
                  Text(tier, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                  Text(
                    "Streak: ${analytics.currentStreak}d current • ${analytics.bestStreak}d best",
                    style = MaterialTheme.typography.bodySmall,
                  )
                  Text(
                    "Total Study: ${String.format(Locale.US, "%.1f", analytics.totalStudyHours)}h • Fiverr: ${String.format(Locale.US, "%.1f", analytics.totalFiverrHours)}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    "Outreach: ${analytics.totalLinkedinConnections} LinkedIn • ${analytics.totalInstagramDms} IG DMs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        // Category Mastery
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text("Category Mastery (30 Days)", style = MaterialTheme.typography.titleMedium)
              analytics.categoryMastery.forEach { catStat ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(catStat.category.wireValue, style = MaterialTheme.typography.bodyMedium)
                    Text(
                      "${catStat.percent}% (${catStat.completedChecks}/${catStat.possibleChecks})",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  LinearProgressIndicator(
                    progress = { (catStat.percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                  )
                }
              }
            }
          }
        }

        // Habit Consistency Breakdown
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text("Habit Consistency (Out of 30 Days)", style = MaterialTheme.typography.titleMedium)
              analytics.habitConsistency.forEach { stat ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = stat.habit.title,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "${stat.completedDays}/30 (${stat.percent}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }

      item {
        ChallengeFooter()
      }
    }
  }
}
