import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;
import java.util.List;

public class test_stripper extends PDFTextStripper {
    public test_stripper() throws java.io.IOException { super(); }
    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws java.io.IOException {
        super.writeString(text, textPositions);
    }
}
