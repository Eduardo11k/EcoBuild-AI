package ecobuild_ai.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import ecobuild_ai.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val UbuntuFontName = GoogleFont("Ubuntu")
val MontserratFontName = GoogleFont("Montserrat")

val UbuntuFont = FontFamily(
    Font(googleFont = UbuntuFontName, fontProvider = provider),
    Font(googleFont = UbuntuFontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = UbuntuFontName, fontProvider = provider, weight = FontWeight.Medium),
)

val MontserratFont = FontFamily(
    Font(googleFont = MontserratFontName, fontProvider = provider),
    Font(googleFont = MontserratFontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = MontserratFontName, fontProvider = provider, weight = FontWeight.SemiBold),
)
