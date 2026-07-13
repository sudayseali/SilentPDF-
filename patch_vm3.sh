cat app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt | sed '5,9d' > tmp_vm3.kt
echo "data class DrawingStroke(" > tmp_stroke.kt
echo "    val points: List<Offset>," >> tmp_stroke.kt
echo "    val color: Color," >> tmp_stroke.kt
echo "    val width: Float," >> tmp_stroke.kt
echo "    val isEraser: Boolean = false" >> tmp_stroke.kt
echo ")" >> tmp_stroke.kt
sed -i '4r tmp_stroke.kt' tmp_vm3.kt
mv tmp_vm3.kt app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt
