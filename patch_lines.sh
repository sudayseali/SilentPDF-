head -n 109 app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt > temp2_pdf.kt
cat temp_pdf.kt >> temp2_pdf.kt
tail -n +182 app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt >> temp2_pdf.kt
mv temp2_pdf.kt app/src/main/java/com/silentpdf/app/ui/screens/PdfPageItem.kt
