import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

class CustomStripper : PDFTextStripper() {
    override fun writeString(text: String, textPositions: List<TextPosition>) {
        super.writeString(text, textPositions)
    }
}
