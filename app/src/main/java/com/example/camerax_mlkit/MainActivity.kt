/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.camerax_mlkit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax_mlkit.databinding.ActivityMainBinding
import com.google.mediapipe.examples.llminference.InferenceModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var recognizer: TextRecognizer
    private lateinit var inferenceModel: InferenceModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        this.cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraController = LifecycleCameraController(baseContext)
        val previewView: PreviewView = viewBinding.viewFinder

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        inferenceModel = InferenceModel.getInstance(this)
        // Flag to track inference status
        var isInferenceInProgress = false

        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            MlKitAnalyzer(
                listOf(recognizer),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this)
            ) { result: MlKitAnalyzer.Result? ->
                val textResults = result?.getValue(recognizer)
                if ((textResults == null) ||
                    (textResults.text == "") ||
                    (textResults.textBlocks.isEmpty())
                ) {
                    previewView.overlay.clear()
                    previewView.setOnTouchListener { _, _ -> false } //no-op
                    return@MlKitAnalyzer
                }


                previewView.overlay.clear()
                for (block in textResults.textBlocks) {
                    // Launch a coroutine to handle the inference
                    lifecycleScope.launch {
                        if (isInferenceInProgress) {
                            // Wait for the previous inference to complete
                            return@launch
                        }

                        isInferenceInProgress = true
                        inferenceModel.generateResponseAsync("Extract dish names from \"${block.text}\" and return them in JSON format with a key \"dishes\".")

                        // Collect partial results
                        var fullResult = ""
                        inferenceModel.partialResults.collect { (partialResult, done) ->
                            // Handle the partial result
                            fullResult += partialResult
                            println("MainActivity Inference result: $fullResult")
                            if (done) {
                                runOnUiThread {
                                    // Parse the JSON output
                                    try {
                                        val jsonObject = JSONObject(fullResult)
                                        val dishes = jsonObject.getJSONArray("dishes")
                                        val dishNames = mutableListOf<String>()
                                        for (i in 0 until dishes.length()) {
                                            dishNames.add(dishes.getString(i))
                                        }

                                        // Update the TextDrawable with the dish names
                                        val textDrawable = TextDrawable(block)
                                        textDrawable.updateText(dishNames.joinToString(", "))
                                        previewView.overlay.add(textDrawable)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                // Reset the flag
                                isInferenceInProgress = false
                            }
                        }


                    }
                }
            }
        )

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    companion object {
        private const val TAG = "AR-food-menu"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}