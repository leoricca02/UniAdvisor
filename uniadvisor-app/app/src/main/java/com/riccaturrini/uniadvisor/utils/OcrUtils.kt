package com.riccaturrini.uniadvisor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object OcrUtils {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(context: Context, imageUri: Uri): Text {
        return try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            textRecognizer.process(inputImage).await()
        } catch (e: Exception) {
            Log.e("OcrUtils", "❌ Error extracting text", e)
            throw OcrException("Failed to extract text: ${e.message}")
        }
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): Text {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(inputImage).await()
        } catch (e: Exception) {
            Log.e("OcrUtils", "❌ Error extracting text from bitmap", e)
            throw OcrException("Failed to extract text: ${e.message}")
        }
    }

    fun generateSearchablePdf(
        context: Context,
        images: List<Uri>,
        extractedTexts: List<Text?>,
        outputFileName: String = "scanned_note_${System.currentTimeMillis()}.pdf"
    ): File {
        try {
            val outputFile = File(context.cacheDir, outputFileName)
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            document.setMargins(0f, 0f, 0f, 0f)

            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)

            images.forEachIndexed { index, imageUri ->
                val bitmap = loadBitmapFromUri(context, imageUri)
                val pageWidth = bitmap.width.toFloat()
                val pageHeight = bitmap.height.toFloat()
                val pageSize = PageSize(pageWidth, pageHeight)

                // 1. Crea Pagina
                val page = pdfDocument.addNewPage(pageSize)

                // 2. Prepara l'immagine
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val imageData = ImageDataFactory.create(byteArrayOutputStream.toByteArray())

                // CORREZIONE: Usiamo l'oggetto Image (Layout API) invece di canvas.addImage
                // Questo risolve l'errore "Unresolved reference"
                val img = Image(imageData)
                img.setFixedPosition(index + 1, 0f, 0f) // Pagina corrente (index+1)
                img.scaleAbsolute(pageWidth, pageHeight) // Scala per riempire la pagina
                document.add(img)

                // 3. Sovrapponi il Testo Invisibile (usando PdfCanvas per precisione)
                val mlText = extractedTexts.getOrNull(index)
                if (mlText != null) {
                    val canvas = PdfCanvas(page)
                    canvas.beginText()
                    canvas.setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.INVISIBLE)

                    for (block in mlText.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox
                            if (box != null) {
                                // Coordinate
                                val pdfX = box.left.toFloat()
                                val pdfY = pageHeight - box.bottom.toFloat()
                                val pdfW = box.width().toFloat()
                                val pdfH = box.height().toFloat()

                                val textWidth = font.getWidth(line.text, pdfH)
                                val hScale = if (textWidth > 0) pdfW / textWidth else 1f

                                // Posiziona e "stira" il testo invisibile
                                canvas.setTextMatrix(hScale, 0f, 0f, 1f, pdfX, pdfY)

                                canvas.setFontAndSize(font, pdfH)
                                canvas.showText(line.text)
                            }
                        }
                    }
                    canvas.endText()
                }
            }

            document.close()
            Log.d("OcrUtils", "✅ Searchable PDF generated: ${outputFile.absolutePath}")
            return outputFile

        } catch (e: Exception) {
            Log.e("OcrUtils", "❌ Error generating PDF", e)
            throw PdfGenerationException("Failed to generate PDF: ${e.message}")
        }
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot load bitmap from URI: $uri")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (bitmap == null) throw IllegalArgumentException("Failed to decode bitmap from: $uri")
        return rotateBitmapIfRequired(context, bitmap, uri)
    }

    private fun rotateBitmapIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            input.close()

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            Log.e("OcrUtils", "Failed to rotate bitmap", e)
            return bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cleanup() {
        textRecognizer.close()
    }
}

class OcrException(message: String) : Exception(message)
class PdfGenerationException(message: String) : Exception(message)