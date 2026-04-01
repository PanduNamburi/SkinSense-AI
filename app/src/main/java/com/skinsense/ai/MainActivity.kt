package com.skinsense.ai
 
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.skinsense.ai.data.AppDatabase
import com.skinsense.ai.ml.DiseaseClassifier
import com.skinsense.ai.ui.compose.SkinSenseNavigation
import com.skinsense.ai.ui.theme.SkinSenseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.skinsense.ai.utils.LocaleHelper
import com.skinsense.ai.utils.NotificationHelper
import com.skinsense.ai.data.FirestoreUserRepository
import com.skinsense.ai.data.FirebaseAuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.skinsense.ai.data.UserRole
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.skinsense.ai.utils.PaymentManager
import android.content.Intent
import com.razorpay.Checkout
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : AppCompatActivity(), PaymentResultWithDataListener, ExternalWalletListener {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    // Logic
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var database: AppDatabase
    private lateinit var notificationHelper: NotificationHelper
    
    // Permission launcher for notifications (Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("SkinSense", "Notification permission granted")
            } else {
                Log.w("SkinSense", "Notification permission denied")
            }
        }
    
    // Initializing classifier here for future use
    private lateinit var classifier: DiseaseClassifier

    // Permission launcher for potential future camera use
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("SkinSense", "MainActivity onCreate - VERSION 2.0")
        
        // Initialize Components
        cameraExecutor = Executors.newSingleThreadExecutor()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "skinsense-db"
        ).build()

        // Preload Razorpay
        Checkout.preload(applicationContext)

        // Initialize classifier in background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                classifier = DiseaseClassifier(this@MainActivity)
                withContext(Dispatchers.Main) {
                    Log.d("SkinSense", "Classifier initialized successfully")
                }
            } catch (e: Exception) {
                Log.e("SkinSense", "Failed to init classifier", e)
            }
        }

        // Initialize Notifications
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        checkAndRequestNotificationPermission()
        
        // Register token if user is already logged in (though forced sign out is below)
        registerFcmToken()
        
        // Setup Compose UI
        setContent {
            SkinSenseTheme {
                val authRepository = com.skinsense.ai.data.FirebaseAuthRepository()
                
                // Persistent session: No forced sign-out on every restart

                SkinSenseNavigation(
                    authRepository = authRepository
                )
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun registerFcmToken() {
        val authRepository = FirebaseAuthRepository()
        val userId = authRepository.getDirectUserId()
        
        if (userId != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("SkinSense", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("SkinSense", "FCM Token: $token")
                
                lifecycleScope.launch(Dispatchers.IO) {
                    val userRepository = FirestoreUserRepository()
                    val profileResult = userRepository.getUserProfile(userId)
                    profileResult.onSuccess { user ->
                        user?.let {
                            userRepository.updateFcmToken(userId, it.role, token)
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && data != null) {
            val response = data.getStringExtra("response")
            Log.d("SkinSense", "Paytm Activity Result: $response")
            // The actual logic is handled by the callback in PaymentManager.startPaytmPayment
            // but some SDK versions return here too.
        }
    }

    // Razorpay Callbacks
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Log.d("SkinSense", "Razorpay Success: $razorpayPaymentId")
        PaymentManager.handleRazorpaySuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        Log.e("SkinSense", "Razorpay Error $errorCode: $response")
        PaymentManager.handleRazorpayError(errorCode, response)
    }

    override fun onExternalWalletSelected(walletName: String?, paymentData: PaymentData?) {
        Log.d("SkinSense", "Razorpay External Wallet: $walletName")
        // Can be handled if external wallets (like Paytm) are integrated via Razorpay
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::classifier.isInitialized) {
            classifier.close()
        }
    }
}
