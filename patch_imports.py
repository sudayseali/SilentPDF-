with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "r") as f:
    content = f.read()

imports_to_add = """
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
"""
content = content.replace("import android.widget.Toast\n", "") # Prevent duplicate
content = content.replace("import android.content.Context", "import android.content.Context\n" + imports_to_add)

with open("app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt", "w") as f:
    f.write(content)
