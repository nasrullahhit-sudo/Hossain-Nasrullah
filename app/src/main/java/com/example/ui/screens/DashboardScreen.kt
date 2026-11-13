package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChallengeSchedule
import com.example.data.model.ChallengeReminder
import com.example.ui.components.ChallengeFooter
import com.example.ui.components.CompletionRing
import com.example.ui.components.SevenDayPerformanceChart
import com.example.ui.state.ChallengeUiState
import java.util.Locale

@Composable
fun DashboardScreen(
  state: ChallengeUiState.Ready,
  onNavigateToDay: (Int) -> Unit,
  onOpenChecklist: () -> Unit,
  onToggleReminder: (ChallengeReminder, Boolean) -> Unit,
) {
  val status = ChallengeSchedule.currentStatus()
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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner Card
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_challenge_hero),
              contentDescription = "Nasrullah's 30-Day Challenge Banner",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color(0x55061E1C), Color(0xE6051A18)),
                  )
                ),
            )
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  color = Color(0x33FFFFFF),
                  shape = RoundedCornerShape(50),
                ) {
                  Text(
                    text = status.phaseLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  )
                }
                Surface(
                  color = Color(0x3314B8A6),
                  shape = RoundedCornerShape(50),
                ) {
                  Text(
                    text = "BD Time: ${ChallengeSchedule.bangladeshTimeSummary()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Welcome, Nasrullah!",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color(0xFF99F6E4),
                )
                Text(
                  text = "Nasrullah's 30-Day Challenge",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Discipline is Power | Keep Going!",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFFCD34D),
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${ChallengeSchedule.START_DISPLAY} – ${ChallengeSchedule.END_DISPLAY} • ${status.countdownLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFE2E8F0),
                )
              }
            }
          }
        }
      }

      // Overall & Selected Day Progress Rings
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CompletionRing(
                percent = analytics.overallPercent,
                size = 92.dp,
                label = "30 Days",
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Overall Progress",
                style = MaterialTheme.typography.labelLarge,
              )
              Text(
                text = "${analytics.totalCompletedChecks} tasks done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CompletionRing(
                percent = analytics.selectedDayPercent,
                size = 92.dp,
                progressColor = MaterialTheme.colorScheme.secondary,
                label = "Day ${state.selectedDayNumber}",
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Selected Day",
                style = MaterialTheme.typography.labelLarge,
              )
              Text(
                text = "${analytics.selectedDayCompletedCount} / ${analytics.totalHabitsCount} habits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Key Metrics Grid (Streaks, Fiverr, Study, Outreach)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricSummaryTile(
              icon = Icons.Rounded.LocalFireDepartment,
              label = "Streak",
              value = "${analytics.currentStreak}d (Best ${analytics.bestStreak}d)",
              modifier = Modifier.weight(1f),
            )
            MetricSummaryTile(
              icon = Icons.Rounded.WorkOutline,
              label = "Fiverr Online",
              value = "${String.format(Locale.US, "%.1f", analytics.totalFiverrHours)} hrs",
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricSummaryTile(
              icon = Icons.Rounded.AutoGraph,
              label = "CST & Study",
              value = "${String.format(Locale.US, "%.1f", analytics.totalStudyHours)} hrs",
              modifier = Modifier.weight(1f),
            )
            MetricSummaryTile(
              icon = Icons.Rounded.Bolt,
              label = "Outreach Total",
              value = "${analytics.totalLinkedinConnections} LI • ${analytics.totalInstagramDms} IG",
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Last 7 Days Performance Chart
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Last 7 Days Performance",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Tap any day bar to open its checklist",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(
                onClick = onOpenChecklist,
                modifier = Modifier.testTag("dashboard_open_checklist_button"),
              ) {
                Text("Open Day ${state.selectedDayNumber}")
              }
            }
            SevenDayPerformanceChart(
              days = analytics.last7Days,
              selectedDayNumber = state.selectedDayNumber,
              onSelectDay = onNavigateToDay,
            )
          }
        }
      }

      // 30-Day Challenge Heatmap Grid (Nov 17 - Dec 16, 2026)
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
            Text(
              text = "30-Day Challenge Map (17 Nov – 16 Dec)",
              style = MaterialTheme.typography.titleMedium,
            )
            val denom = state.habits.size.coerceAtLeast(1)
            val rows = ChallengeSchedule.allDays.chunked(6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              rows.forEach { rowDays ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  rowDays.forEach { dayInfo ->
                    val done = state.logsByDay[dayInfo.dayNumber]?.completedHabitIds?.size ?: 0
                    val pct = ((done * 100) / denom).coerceIn(0, 100)
                    val isSel = dayInfo.dayNumber == state.selectedDayNumber
                    val bg = when {
                      isSel -> MaterialTheme.colorScheme.secondary
                      pct >= 80 -> MaterialTheme.colorScheme.primary
                      pct >= 40 -> MaterialTheme.colorScheme.primaryContainer
                      pct > 0 -> MaterialTheme.colorScheme.secondaryContainer
                      else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val fg = when {
                      isSel -> MaterialTheme.colorScheme.onSecondary
                      pct >= 80 -> MaterialTheme.colorScheme.onPrimary
                      pct >= 40 -> MaterialTheme.colorScheme.onPrimaryContainer
                      pct > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                      else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onNavigateToDay(dayInfo.dayNumber) },
                      contentAlignment = Alignment.Center,
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                          text = "${dayInfo.dayNumber}",
                          style = MaterialTheme.typography.labelLarge,
                          color = fg,
                        )
                        Text(
                          text = "$pct%",
                          style = MaterialTheme.typography.labelSmall,
                          color = fg.copy(alpha = 0.85f),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Daily Targets & Reminders Preview
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Text(
                text = "Daily Targets & Reminders",
                style = MaterialTheme.typography.titleMedium,
              )
            }

            TargetQuickBadgeRow()

            if (state.reminders.isEmpty()) {
              Text(
                text = "No reminders configured yet. Add one in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              state.reminders.forEach { reminder ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.NotificationsActive,
                      contentDescription = null,
                      tint = if (reminder.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(20.dp),
                    )
                    Column {
                      Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "${reminder.timeLabel} • ${reminder.targetText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                  Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { onToggleReminder(reminder, it) },
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

@Composable
private fun MetricSummaryTile(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = value,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun TargetQuickBadgeRow() {
  val targets = listOf(
    Icons.Rounded.WorkOutline to "Fiverr Online: 7–8 Hours Daily",
    Icons.Rounded.AccessTime to "LinkedIn Video Post: 8:00 PM Bangladesh Time",
    Icons.Rounded.CheckCircle to "CST Study 3h • 10 LI Connections • 20 IG DMs",
  )
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    targets.forEach { (icon, text) ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = text,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
