package com.skinsense.ai.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skinsense.ai.data.AppDatabase
import com.skinsense.ai.data.HistoryEntity
import com.skinsense.ai.ml.DiseaseClassifier
import com.skinsense.ai.data.AnalysisResult
import com.skinsense.ai.ui.compose.SeverityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import androidx.room.Room

class MainViewModel(
    application: Application,
    private val authRepository: com.skinsense.ai.data.AuthRepository,
    private val firestoreHistoryRepository: com.skinsense.ai.data.FirestoreHistoryRepository = com.skinsense.ai.data.FirestoreHistoryRepository()
) : AndroidViewModel(application) {

    private val classifier = DiseaseClassifier(application)
    private val database = AppDatabase.getDatabase(application)

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.6f
    }

    fun analyze(uri: Uri) {
        _isAnalyzing.value = true
        _error.value = null
        _analysisResult.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Load Bitmap
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap == null) {
                    _error.value = "Failed to load image"
                    _isAnalyzing.value = false
                    return@launch
                }

                // 2. Classify
                // Note: DiseaseClassifier handles resizing/preprocessing
                val predictions = classifier.classify(bitmap)

                if (predictions.isEmpty()) {
                    _error.value = "No skin condition detected"
                    _isAnalyzing.value = false
                    return@launch
                }

                val topPrediction = predictions[0]
                
                // 2.1 Check Confidence Threshold to filter "Normal" or low-quality images
                if (topPrediction.confidence < CONFIDENCE_THRESHOLD) {
                    Log.d("SkinSense", "Low confidence detection: ${topPrediction.label} (${topPrediction.confidence})")
                    _error.value = "No clear skin condition detected. Please ensure the photo is clear and focused on the affected area."
                    _isAnalyzing.value = false
                    return@launch
                }
                
                // 3. Estimate Severity based on Lesion Area
                val (severityLevel, severityPercentage) = com.skinsense.ai.utils.SeverityEstimator.estimateSeverity(bitmap)
                Log.d("SkinSense", "Severity Estimated: $severityLevel ($severityPercentage%)")

                // 4. Map to AnalysisResult
                val result = mapPredictionToResult(topPrediction, uri, severityLevel, severityPercentage)
                
                // Convert to Base64 for persistent Firestore storage
                val base64Image = uriToBase64(uri)
                val finalResult = result.copy(imageBase64 = base64Image)
                
                _analysisResult.value = finalResult
                
                // 4. Save to History (Always)
                saveToHistory(finalResult)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Analysis failed", e)
                _error.value = "Analysis failed: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading bitmap", e)
            null
        }
    }

    private suspend fun uriToBase64(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) return@withContext null

                // Resize to 300x300 max for better detail in history
                val maxDimension = 300
                val scale = Math.min(
                    maxDimension.toFloat() / originalBitmap.width,
                    maxDimension.toFloat() / originalBitmap.height
                ).coerceAtMost(1.0f)

                val matrix = android.graphics.Matrix()
                matrix.postScale(scale, scale)
                val resizedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                )

                val outputStream = java.io.ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()
                
                val result = "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                Log.d("SkinSense", "Scan image converted. Byte size: ${byteArray.size}")
                result
            } catch (e: Exception) {
                Log.e("SkinSense", "uriToBase64 failed", e)
                null
            }
        }
    }
    
    private fun saveToHistory(result: AnalysisResult) {
        val userId = authRepository.currentUserId.value
        if (userId == null) {
            return
        }
        
       viewModelScope.launch(Dispatchers.IO) {
           val entity = HistoryEntity(
               userId = userId,
               imagePath = result.imageUri.toString(),
               topDiagnosis = result.diseaseName,
               confidence = result.confidence,
               timestamp = System.currentTimeMillis()
           )
            database.historyDao().insert(entity)
            
            // Save to Firestore for persistence
            firestoreHistoryRepository.saveHistory(userId, result)
        }
    }




    private fun mapPredictionToResult(
        prediction: com.skinsense.ai.ml.Prediction, 
        uri: Uri,
        severityLevel: com.skinsense.ai.ui.compose.SeverityLevel,
        severityPercentage: Float
    ): AnalysisResult {
        // In a real app, we would look up details from a database or repository based on the label.
        // For now, we'll map common labels to rich data, and fallback to a genuine default.
        
        val name = prediction.label
        val confidence = prediction.confidence
        
        return when (name) {
            "Acne" -> AnalysisResult(
                diseaseName = "Acne (Acne Vulgaris)",
                category = "Common Skin Condition",
                confidence = confidence,
                description = "Acne is a skin condition driven by hormonal surges where hair follicles become plugged. It involves excess sebum, clogged keratin, and C. acnes bacteria.",
                symptoms = listOf("Open comedones (blackheads)", "Closed comedones (whiteheads)", "Inflammatory lesions (papules/pustules)", "Cysts and nodules (severe cases)"),
                causes = listOf("Excess sebum production", "Hair follicles clogged by keratin", "Overgrowth of C. acnes bacteria", "Hormonal surges (androgens)"),
                recommendedActions = listOf("Topical: Retinoids (Adapalene), Benzoyl Peroxide", "Systemic: Oral antibiotics for inflammation", "Isotretinoin (Accutane) for severe cases"),
                imageUri = uri
            )
            "Actinic_Keratosis" -> AnalysisResult(
                 diseaseName = "Actinic Keratosis (AK)",
                 category = "Pre-cancerous",
                 confidence = confidence,
                 description = "Rough, scaly patches on sun-exposed areas caused by DNA mutations from long-term UV radiation. Considered precancerous.",
                 symptoms = listOf("Rough, scaly, 'sandpaper-like' patches", "Often easier to feel than to see", "Located on sun-exposed areas"),
                 causes = listOf("DNA mutations in skin cells (keratinocytes)", "Long-term UV radiation exposure"),
                 recommendedActions = listOf("Physical: Cryotherapy (liquid nitrogen) or curettage", "Field Therapy: Prescription creams (5-Fluorouracil, Tirbanibulin)", "Sun protection"),
                 imageUri = uri
            )
            "Benign_tumors" -> AnalysisResult(
                diseaseName = "Benign Tumor",
                category = "Benign Growth",
                confidence = confidence,
                description = "Non-cancerous growths usually caused by genetics or trauma. Includes Lipomas (fatty deposits) and Dermatofibromas (fibrous nodules).",
                symptoms = listOf("Lipomas: Soft, doughy, and movable", "Dermatofibromas: Firm, brown/tan bumps that dimple when pinched"),
                causes = listOf("Genetic factors", "Localized skin trauma"),
                recommendedActions = listOf("Generally monitored", "Surgical excision if painful or restricted"),
                imageUri = uri
            )
            "Candidiasis" -> AnalysisResult(
                diseaseName = "Candidiasis (Yeast Infection)",
                category = "Fungal Infection",
                confidence = confidence,
                description = "Overgrowth of Candida fungi, usually in warm, moist environments like skin folds.",
                symptoms = listOf("Bright red, 'beefy' rash", "Distinct satellite lesions (small red spots)", "Common in groin, armpits, under-breast"),
                causes = listOf("Overgrowth of Candida fungi", "Warm, moist environments", "Diabetes, obesity, or antibiotic use"),
                recommendedActions = listOf("Topical: Antifungals (Nystatin or Clotrimazole)", "Lifestyle: Keep skin folds dry", "Use moisture-wicking fabrics"),
                imageUri = uri
            )
            "Eczema" -> AnalysisResult(
                diseaseName = "Eczema (Atopic Dermatitis)",
                category = "Inflammatory Skin Condition",
                confidence = confidence,
                description = "A condition with a dysfunctional skin barrier and overactive immune response to triggers like soap or pollen.",
                symptoms = listOf("Intense itching and redness", "'Weeping' clear fluid", "Lichenification (thick, leathery skin) from scratching"),
                causes = listOf("Dysfunctional skin barrier (Filaggrin deficiency)", "Immune response to environmental triggers"),
                recommendedActions = listOf("Repair: Thick emollients after bathing", "Control: Topical corticosteroids", "Non-steroidal JAK inhibitors for flares"),
                imageUri = uri
            )
            "Psoriasis" -> AnalysisResult(
                diseaseName = "Psoriasis",
                category = "Autoimmune Disease",
                confidence = confidence,
                description = "An autoimmune disorder where T-cells trigger skin cells to grow every 3-4 days instead of 30, creating plaques.",
                symptoms = listOf("Well-defined, raised red plaques", "Silvery-white scales coverage", "Affects elbows, knees, scalp"),
                causes = listOf("Autoimmune disorder", "T-cells triggering rapid cell growth"),
                recommendedActions = listOf("Mild: Vitamin D analogs and steroids", "Moderate/Severe: Biologics or Narrowband UVB light therapy"),
                imageUri = uri
            )
            "Rosacea" -> AnalysisResult(
                diseaseName = "Rosacea",
                category = "Chronic Skin Condition",
                confidence = confidence,
                description = "A condition of neurovascular dysregulation causing facial flushing and visible blood vessels.",
                symptoms = listOf("Facial flushing", "Visible 'spider veins' (telangiectasia)", "Acne-like bumps (no blackheads)"),
                causes = listOf("Neurovascular dysregulation", "Immune reaction to Demodex mites"),
                recommendedActions = listOf("Topical: Ivermectin or Metronidazole", "Procedural: Vascular lasers for redness"),
                imageUri = uri
            )
            "Seborrh_Keratoses" -> AnalysisResult(
                diseaseName = "Seborrheic Keratosis (SK)",
                category = "Benign Growth",
                confidence = confidence,
                description = "Benign growth of keratinocytes related to aging/genetics. Not caused by sun and not contagious.",
                symptoms = listOf("'Stuck-on' appearance", "Look like brown/black candle wax", "Waxy, crumbly, or scaly texture"),
                causes = listOf("Aging", "Genetics"),
                recommendedActions = listOf("Medically unnecessary to treat", "Cryotherapy or shave excision if irritated"),
                imageUri = uri
            )
            "SkinCancer" -> AnalysisResult(
                diseaseName = "Skin Cancer",
                category = "Oncology",
                confidence = confidence,
                description = "Includes Basal Cell, Squamous Cell, and Melanoma. Caused by cumulative or intense UV exposure damaging DNA.",
                symptoms = listOf("BCC/SCC: Non-healing sores, pearly bumps, scaly red patches", "Melanoma: Asymmetric, Irregular borders, Multiple colors, Large diameter, Evolving"),
                causes = listOf("Cumulative UV exposure", "Intense intermittent UV exposure"),
                recommendedActions = listOf("Surgery: Mohs Micrographic Surgery", "Advanced: Immunotherapy for metastatic cases", "Immediate professional consultation"),
                imageUri = uri
            )
            "Vitiligo" -> AnalysisResult(
                diseaseName = "Vitiligo",
                category = "Autoimmune Condition",
                confidence = confidence,
                description = "Autoimmune attack where the immune system destroys pigment-producing melanocytes.",
                symptoms = listOf("Stark white, depigmented patches", "Sharp borders", "Often symmetrical (face, hands, feet)"),
                causes = listOf("Autoimmune attack on melanocytes"),
                recommendedActions = listOf("Repigmentation: Topical JAK inhibitors", "Excimer laser therapy", "Protection: High-SPF sunscreen"),
                imageUri = uri
            )
            "Warts" -> AnalysisResult(
                diseaseName = "Warts (Verrucae)",
                category = "Viral Infection",
                confidence = confidence,
                description = "Infection of the top layer of skin by the Human Papillomavirus (HPV).",
                symptoms = listOf("Small, fleshy bumps with 'cauliflower' texture", "Tiny black dots ('seeds')", "Thrombosed capillaries"),
                causes = listOf("Human Papillomavirus (HPV) infection"),
                recommendedActions = listOf("Destructive: Salicylic acid or Cryotherapy", "Immune-based: Candida antigen injection"),
                imageUri = uri
            )
             else -> AnalysisResult(
                diseaseName = name, // Fallback to raw label
                category = "Uncategorized",
                confidence = confidence,
                description = "Analysis complete. Please consult a specialist for accurate diagnosis.",
                symptoms = emptyList(),
                causes = emptyList(),
                recommendedActions = listOf("Consult a dermatologist if concerned"),
                imageUri = uri,
                severityLevel = severityLevel,
                severityPercentage = severityPercentage
            )
        }.copy(severityLevel = severityLevel, severityPercentage = severityPercentage)
    }

    fun saveResultAsPdf(context: android.content.Context) {
        val result = _analysisResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = com.skinsense.ai.utils.PdfGenerator.generatePdf(context, result)
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        android.widget.Toast.makeText(context, "PDF saved to Downloads", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        _error.value = "Failed to save PDF"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                     _error.value = "Error saving PDF: ${e.message}"
                }
            }
        }
    }

    fun shareResult(context: android.content.Context) {
        val result = _analysisResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = com.skinsense.ai.utils.PdfGenerator.generateShareablePdf(context, result)
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Analysis Result"))
                    } else {
                        _error.value = "Failed to generate PDF for sharing"
                        android.widget.Toast.makeText(context, "Failed to generate PDF", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                     _error.value = "Error sharing PDF: ${e.message}"
                     android.widget.Toast.makeText(context, "Error sharing: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifier.close()
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val authRepository: com.skinsense.ai.data.AuthRepository,
    private val firestoreHistoryRepository: com.skinsense.ai.data.FirestoreHistoryRepository = com.skinsense.ai.data.FirestoreHistoryRepository()
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, authRepository, firestoreHistoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
