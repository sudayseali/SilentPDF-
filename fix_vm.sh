cat app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt | sed '/package com.example.ui.viewmodel/d' > tmp_vm.kt
sed -i '1i package com.example.ui.viewmodel' tmp_vm.kt
mv tmp_vm.kt app/src/main/java/com/example/ui/viewmodel/SilentPdfViewModel.kt
