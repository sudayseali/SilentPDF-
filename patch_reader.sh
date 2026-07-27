sed -i 's/pdf = pdf/pdf = currentPdf/g' app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt
sed -i '1270s/pdf != null/currentPdf != null/g' app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt
sed -i '1276s/openPdf(pdf)/openPdf(currentPdf!!)/g' app/src/main/java/com/silentpdf/app/ui/screens/ReaderScreen.kt
