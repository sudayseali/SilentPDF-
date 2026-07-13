cat app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt | sed '/data class DrawingStroke/,/)/d' > tmp_vm_clean.kt

cat << 'INNER_EOF' > final_vm.kt
package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.PdfEntity
import com.example.data.db.SilentPdfDatabase
import com.example.data.repository.PdfRenderEngine
import com.example.data.repository.PdfRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false
)
INNER_EOF

# Remove the package and imports from tmp_vm_clean.kt
grep -v "^package " tmp_vm_clean.kt | grep -v "^import " >> final_vm.kt
mv final_vm.kt app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt
