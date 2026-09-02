package ecobuild_ai.app.model

import ecobuild_ai.app.R

data class AnalysisItem(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val score: Int = 0,
    val imageUrl: String? = null,
    val imageRes: Int? = null
)

// Sample dynamic data for demo and fallback
val sampleRecentAnalyses = listOf(
    AnalysisItem(
        id = "1",
        title = "Casa Verde - Lote 12",
        date = "12 May 2024",
        score = 82,
        imageRes = R.drawable.counter_eco_img
    ),
    AnalysisItem(
        id = "2",
        title = "Villa Solar - Quinta Nova",
        date = "08 May 2024",
        score = 91,
        imageRes = R.drawable.login_img
    ),
    AnalysisItem(
        id = "3",
        title = "Eco Loft - Rua das Flores",
        date = "01 May 2024",
        score = 76,
        imageRes = R.drawable.register_img
    ),
    AnalysisItem(
        id = "4",
        title = "Residência Aurora - Bloco B",
        date = "24 Apr 2024",
        score = 88,
        imageRes = R.drawable.counter_eco_img
    ),
    AnalysisItem(
        id = "5",
        title = "Edifício Sustentável Mar",
        date = "15 Apr 2024",
        score = 95,
        imageRes = R.drawable.logoapp_img
    )
)
