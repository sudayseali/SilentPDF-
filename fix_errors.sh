sed -i 's/key = { it.id }/key = { it.uriString }/g' app/src/main/java/com/example/ui/screens/LibraryScreen.kt

sed -i '/import androidx.compose.material.icons.outlined.\*/a \
import androidx.compose.material.icons.automirrored.filled.*\
import androidx.compose.material.icons.automirrored.outlined.*\
import androidx.compose.ui.graphics.StrokeCap' app/src/main/java/com/example/ui/screens/LibraryScreen.kt
