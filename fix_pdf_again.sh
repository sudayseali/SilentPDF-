sed -i 's/androidx.compose.foundation.gestures.detectDragGestures/detectDragGestures/g' app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
sed -i 's/androidx.compose.foundation.gestures.awaitEachGesture/awaitEachGesture/g' app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
sed -i '10i import androidx.compose.ui.input.pointer.positionChanged' app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
