import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

class BoundingBoxStripper : PDFTextStripper() {
    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        super.writeString(text, textPositions)
    }
}
