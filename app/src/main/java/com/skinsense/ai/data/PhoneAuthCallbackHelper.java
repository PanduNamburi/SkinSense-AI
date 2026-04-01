package com.skinsense.ai.data;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.FirebaseException;
import java.util.concurrent.TimeUnit;

public class PhoneAuthCallbackHelper {

    public interface PhoneAuthCallback {
        void onVerificationCompleted(@NonNull PhoneAuthCredential credential);
        void onVerificationFailed(@NonNull FirebaseException e);
        void onCodeSent(@NonNull String verificationId, @NonNull Object token);
    }

    public static void startPhoneAuth(FirebaseAuth auth, String phoneNumber, Activity activity, final PhoneAuthCallback callback) {
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d("PhoneAuthCallbackHelper", "onVerificationCompleted:" + credential.getSmsCode());
                callback.onVerificationCompleted(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.w("PhoneAuthCallbackHelper", "onVerificationFailed", e);
                callback.onVerificationFailed(e);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d("PhoneAuthCallbackHelper", "onCodeSent:" + verificationId);
                callback.onCodeSent(verificationId, token);
            }
        };

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}
