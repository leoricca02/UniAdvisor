package com.riccaturrini.uniadvisor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for OCR (Optical Character Recognition) operations
 */
object OcrUtils {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract text from an image using ML Kit
     */
    suspend fun extractTextFromImage(context: Context, imageUri: Uri): String {
        return try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val result = textRecognizer.process(inputImage).await()

            val extractedText = result.text
            Log.d("OcrUtils", "✅ Text extracted: ${extractedText.length} characters")

            extractedText
        } catch (e: Exception) {
            Log.e("OcrUtils", "❌ Error extracting text", e)
            throw OcrException("Failed to extract text: ${e.message}")
        }
    }

    /**
     * Extract text from a bitmap
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(inputImage).await()
            result.text
        } catch (e: Exception) {
            Log.e("OcrUtils", "❌ Error extracting text from bitmap", e)
            throw OcrException("Failed to extract text: ${e.message}")
        }
    }

    /**
     * Generate a searchable PDF from images and extracted text
     */
    fun generateSearchablePdf(
        context: Context,
        images: List<Uri>,
        extractedTexts: List<String>,
        outputFileName: String = "scanned_note_${System.currentTimeMillis()}.pdf"
    ): File {
        try {
            val outputFile = File(context.cacheDir, outputFileName)
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            images.forEachIndexed { index, imageUri ->
                // Add image to PDF
                val bitmap = loadBitmapFromUri(context, imageUri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val imageData = ImageDataFactory.create(byteArrayOutputStream.toByteArray())
                val pdfImage = Image(imageData)

                // Scale image to fit page
                pdfImage.setAutoScale(true)
                document.add(pdfImage)

                // Add extracted text (invisible layer for searchability)
                if (index < extractedTexts.size && extractedTexts[index].isNotBlank()) {
                    val textParagraph = Paragraph(extractedTexts[index])
                        .setFontSize(1f) // Very small font
                        .setOpacity(0.01f) // Nearly invisible
                    document.add(textParagraph)
                }

                // Add page break if not last image
                if (index < images.size - 1) {
                    document.add(com.itextpdf.layout.element.AreaBreak())
                }
            }

            document.close()
            Log.d("OcrUtils", "✅ PDF generated: ${outputFile.absolutePath}")

            return outputFile
        } catch (e: Exception) {
            Log.e("OcrUtils", "❌ Error generating PDF", e)
            throw PdfGenerationException("Failed to generate PDF: ${e.message}")
        }
    }

    /**
     * Load bitmap from URI
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IllegalArgumentException("Cannot load bitmap from URI: $uri")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        textRecognizer.close()
    }
}

/**
 * Custom exceptions
 */
class OcrException(message: String) : Exception(message)
class PdfGenerationException(message: String) : Exception(message)