sed -i '/val bitmap = try {/i \
            val bitmap = try {\
                Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888).apply {\
                    eraseColor(android.graphics.Color.WHITE)\
                }\
            } catch (e: OutOfMemoryError) {\
                Log.e("PdfRenderEngine", "OOM when creating bitmap for page $pageIndex", e)\
                System.gc()\
                val smallerWidth = (renderWidth / 2).coerceAtLeast(100)\
                val smallerHeight = (renderHeight / 2).coerceAtLeast(100)\
                Bitmap.createBitmap(smallerWidth, smallerHeight, Bitmap.Config.ARGB_8888).apply {\
                    eraseColor(android.graphics.Color.WHITE)\
                }\
            }' app/src/main/java/com/example/data/repository/PdfRenderEngine.kt

sed -i '/val bitmap = try {/,/}/d' app/src/main/java/com/example/data/repository/PdfRenderEngine.kt
