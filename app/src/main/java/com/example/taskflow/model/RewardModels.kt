package com.example.taskflow.model

import kotlin.math.min
import kotlin.math.roundToInt

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val isUnlocked: Boolean,
    val rewardXp: Int
) {
    val progressFraction: Float
        get() = if (targetProgress == 0) 1f else (currentProgress.toFloat() / targetProgress.toFloat()).coerceIn(0f, 1f)
}

data class UserLevel(
    val levelNumber: Int,
    val title: String,
    val currentXp: Int,
    val levelStartXp: Int,
    val levelEndXp: Int
) {
    val progressFraction: Float
        get() {
            val range = levelEndXp - levelStartXp
            if (range <= 0) return 1f
            return ((currentXp - levelStartXp).toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }

    val xpToNextLevel: Int
        get() = (levelEndXp - currentXp).coerceAtLeast(0)
}

data class ProductivityRating(
    val score: Float, // e.g. 4.4
    val filledStars: Int, // e.g. 4
    val summary: String,
    val explanation: String
)

object RewardCalculator {

    fun calculateLevel(totalXp: Int): UserLevel {
        return when {
            totalXp < 100 -> UserLevel(1, "Novice Planner", totalXp, 0, 100)
            totalXp < 250 -> UserLevel(2, "Task Explorer", totalXp, 100, 250)
            totalXp < 500 -> UserLevel(3, "Focus Achiever", totalXp, 250, 500)
            totalXp < 1000 -> UserLevel(4, "Productivity Pro", totalXp, 500, 1000)
            totalXp < 2000 -> UserLevel(5, "Master of Flow", totalXp, 1000, 2000)
            else -> UserLevel(6, "Zen Grandmaster", totalXp, 2000, 5000)
        }
    }

    fun calculateProductivityRating(
        totalTasks: Int,
        completedTasks: Int,
        streakDays: Int
    ): ProductivityRating {
        if (totalTasks == 0) {
            return ProductivityRating(
                score = 5.0f,
                filledStars = 5,
                summary = "Fresh Slate",
                explanation = "Your workspace is clear! Create and complete tasks to build your active productivity rating."
            )
        }

        val completionRate = completedTasks.toFloat() / totalTasks.toFloat() // 0.0 to 1.0

        // Weightings:
        // - Completion rate: up to 3.0 points
        val rateScore = completionRate * 3.0f

        // - Volume bonus: up to 1.0 points (10 completed tasks = full 1.0)
        val volumeScore = min(1.0f, completedTasks / 10.0f)

        // - Consistency / Streak bonus: up to 1.0 points (5 day streak = full 1.0)
        val streakScore = min(1.0f, streakDays * 0.2f)

        // Baseline guaranteed offset when active tasks exist
        val rawScore = 1.0f + (rateScore * 0.8f) + (volumeScore * 0.6f) + (streakScore * 0.6f)
        val finalScore = ((rawScore * 10f).roundToInt() / 10f).coerceIn(1.0f, 5.0f)
        val stars = finalScore.toInt().coerceIn(1, 5)

        val completionPercent = (completionRate * 100).toInt()
        val summary = when {
            finalScore >= 4.5f -> "Exceptional Flow"
            finalScore >= 3.8f -> "Great Momentum"
            finalScore >= 3.0f -> "Consistent Progress"
            else -> "Building Momentum"
        }

        val explanation = "Rating is calculated from your $completionPercent% completion rate ($completedTasks of $totalTasks done) and a $streakDays-day active streak."

        return ProductivityRating(
            score = finalScore,
            filledStars = stars,
            summary = summary,
            explanation = explanation
        )
    }

    fun computeAchievements(
        completedCount: Int,
        streakDays: Int,
        highPriorityCompletedCount: Int
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Complete your very first task",
                currentProgress = min(1, completedCount),
                targetProgress = 1,
                isUnlocked = completedCount >= 1,
                rewardXp = 50
            ),
            Achievement(
                id = "high_five",
                title = "High Five",
                description = "Complete 5 tasks",
                currentProgress = min(5, completedCount),
                targetProgress = 5,
                isUnlocked = completedCount >= 5,
                rewardXp = 100
            ),
            Achievement(
                id = "quarter_century",
                title = "Quarter Century",
                description = "Complete 25 tasks",
                currentProgress = min(25, completedCount),
                targetProgress = 25,
                isUnlocked = completedCount >= 25,
                rewardXp = 250
            ),
            Achievement(
                id = "half_century",
                title = "Half Century",
                description = "Complete 50 tasks",
                currentProgress = min(50, completedCount),
                targetProgress = 50,
                isUnlocked = completedCount >= 50,
                rewardXp = 500
            ),
            Achievement(
                id = "century_club",
                title = "Century Club",
                description = "Complete 100 tasks",
                currentProgress = min(100, completedCount),
                targetProgress = 100,
                isUnlocked = completedCount >= 100,
                rewardXp = 1000
            ),
            Achievement(
                id = "streak_starter",
                title = "Streak Starter",
                description = "Maintain a 3-day completion streak",
                currentProgress = min(3, streakDays),
                targetProgress = 3,
                isUnlocked = streakDays >= 3,
                rewardXp = 150
            ),
            Achievement(
                id = "unstoppable",
                title = "Unstoppable",
                description = "Maintain a 7-day completion streak",
                currentProgress = min(7, streakDays),
                targetProgress = 7,
                isUnlocked = streakDays >= 7,
                rewardXp = 350
            ),
            Achievement(
                id = "priority_master",
                title = "Priority Master",
                description = "Complete 5 High-priority tasks",
                currentProgress = min(5, highPriorityCompletedCount),
                targetProgress = 5,
                isUnlocked = highPriorityCompletedCount >= 5,
                rewardXp = 200
            )
        )
    }
}
