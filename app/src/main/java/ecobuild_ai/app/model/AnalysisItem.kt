package ecobuild_ai.app.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ecobuild_ai.app.R

data class AnalysisItem(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val score: Int = 0,
    val imageUrl: String? = null,
    @DrawableRes val imageRes: Int? = null,
    @StringRes val titleRes: Int? = null,
    @StringRes val dateRes: Int? = null
)

// Sample dynamic data for demo and fallback with English string resources
val sampleRecentAnalyses = listOf(
    AnalysisItem(
        id = "1",
        title = "Green House - Lot 12",
        date = "12 May 2024",
        score = 82,
        imageRes = R.drawable.counter_eco_img,
        titleRes = R.string.sample_project_1_title,
        dateRes = R.string.sample_project_1_date
    ),
    AnalysisItem(
        id = "2",
        title = "Solar Villa - New Estate",
        date = "08 May 2024",
        score = 91,
        imageRes = R.drawable.login_img,
        titleRes = R.string.sample_project_2_title,
        dateRes = R.string.sample_project_2_date
    ),
    AnalysisItem(
        id = "3",
        title = "Eco Loft - Blossom St",
        date = "01 May 2024",
        score = 76,
        imageRes = R.drawable.register_img,
        titleRes = R.string.sample_project_3_title,
        dateRes = R.string.sample_project_3_date
    ),
    AnalysisItem(
        id = "4",
        title = "Aurora Residence - Block B",
        date = "24 Apr 2024",
        score = 88,
        imageRes = R.drawable.counter_eco_img,
        titleRes = R.string.sample_project_4_title,
        dateRes = R.string.sample_project_4_date
    ),
    AnalysisItem(
        id = "5",
        title = "Sustainable Sea Building",
        date = "15 Apr 2024",
        score = 95,
        imageRes = R.drawable.logoapp_img,
        titleRes = R.string.sample_project_5_title,
        dateRes = R.string.sample_project_5_date
    )
)

