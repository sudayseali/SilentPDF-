cat app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt | sed '5,9d' > tmp_vm2.kt
echo "" >> tmp_vm2.kt
echo "data class DrawingStroke(" >> tmp_vm2.kt
echo "    val points: List<Offset>," >> tmp_vm2.kt
echo "    val color: Color," >> tmp_vm2.kt
echo "    val width: Float" >> tmp_vm2.kt
echo ")" >> tmp_vm2.kt
mv tmp_vm2.kt app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt
