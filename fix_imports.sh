sed -i '1,4d' app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
sed -i '/package com.silentpdf.app.ui.screens/a \
import androidx.compose.foundation.gestures.awaitEachGesture\
import androidx.compose.foundation.gestures.awaitFirstDown\
import androidx.compose.foundation.gestures.calculateZoom\
import androidx.compose.foundation.gestures.calculatePan\
' app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
