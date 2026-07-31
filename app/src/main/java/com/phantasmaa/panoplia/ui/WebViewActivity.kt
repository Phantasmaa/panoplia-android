package com.phantasmaa.panoplia.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import com.phantasmaa.panoplia.BuildConfig
import com.phantasmaa.panoplia.R
import com.phantasmaa.panoplia.databinding.ActivityWebviewBinding
import java.io.File
import java.io.IOException

/**
 * Single-screen WebView wrapper around the Phantasmaa SSO + microservice suite.
 *
 * The web app already handles login (via SSO cookies) and session persistence,
 * so this Activity is intentionally thin: load URL, render toolbar + progress,
 * forward file picker / download / camera / notification events, and provide
 * a Salir button that wipes cookies and storage and reloads the login page.
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding

    /** File picker callbacks set by the WebView's <input type=file> and onShowFileChooser. */
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        if (result.resultCode != Activity.RESULT_OK) {
            callback.onReceiveValue(null)
            return@registerForActivityResult
        }
        val uris = mutableListOf<Uri>()
        // Multi-select
        val clip = result.data?.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
        } else {
            result.data?.data?.let { uris.add(it) }
        }
        // If camera was taken, append it
        cameraUri?.let { uris.add(0, it) }
        callback.onReceiveValue(uris.toTypedArray())
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        // Camera file is already at cameraUri. Re-launch the file picker so
        // the WebView picks it up via the same callback path.
        if (success && cameraUri != null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            // Append camera result to the in-flight picker callback directly
            filePathCallback?.onReceiveValue(arrayOf(cameraUri!!))
            filePathCallback = null
        }
        cameraUri = null
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // No-op for now — the next showFileChooser call will re-trigger
        // and the system will allow access to already-granted media.
        val denied = results.filterValues { !it }
        if (denied.isNotEmpty()) {
            Toast.makeText(
                this,
                "Sin permisos no podemos acceder a tus archivos. Andá a Ajustes para habilitarlos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == -1L) return
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val q = DownloadManager.Query().setFilterById(id)
            dm.query(q).use { c ->
                if (c != null && c.moveToFirst()) {
                    val titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val title = if (titleIdx >= 0) c.getString(titleIdx) else "Archivo"
                    val uriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val local = if (uriIdx >= 0) c.getString(uriIdx) else null
                    Toast.makeText(
                        this@WebViewActivity,
                        "Descargado: $title",
                        Toast.LENGTH_LONG
                    ).show()
                    // Open the downloaded file
                    local?.let { openDownload(Uri.parse(it)) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge with status bar visible
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        configureWebView(binding.webview)
        binding.btnExit.setOnClickListener { confirmExit() }
        binding.btnRefresh.setOnClickListener { binding.webview.reload() }
        binding.swipeRefresh.setOnRefreshListener { binding.webview.reload() }

        // Handle back press: navigate back in the WebView if possible, otherwise finish()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webview.canGoBack()) {
                    binding.webview.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Show progress until the page is loaded
        binding.progress.progress = 0
        binding.progress.visibility = View.VISIBLE

        binding.webview.loadUrl(BuildConfig.WEB_BASE_URL)

        // Register download listener on the DownloadManager side
        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )

        // Permission for camera and notifications
        requestInitialPermissions()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(downloadCompleteReceiver) }
        binding.webview.destroy()
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(web: WebView) {
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportMultipleWindows(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            // Allow file access for tools that produce file:// URLs (rare)
            allowFileAccess = true
            allowContentAccess = true
            // We need the WebView to be able to handle popups (target="_blank")
            // and to receive upload events
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            // We want mixed content if the user accidentally opens a tunnel
            // that redirects to http://. Keep this on for legacy tunnels.
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // User agent — identify ourselves so the backend can log the
            // source. Don't lie about being mobile Safari.
            userAgentString = "$userAgentString PanopliaAndroid/${BuildConfig.VERSION_NAME}"
            // Make storage persistent across app launches
            databasePath = filesDir.path + "/databases"
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }

        web.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            handleDownload(url, userAgent, contentDisposition, mimetype)
        })

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // Open external links (mailto:, tel:, intent://, https://maps.google.com, etc.)
                // in the system browser/app, not in the WebView.
                if (url.startsWith("mailto:") ||
                    url.startsWith("tel:") ||
                    url.startsWith("intent:") ||
                    url.startsWith("market:") ||
                    url.startsWith("sms:") ||
                    url.startsWith("whatsapp:")
                ) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    } catch (_: Exception) {
                        Toast.makeText(this@WebViewActivity, "No hay app para abrir esto", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progress.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progress.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                updateUrlBar(url)
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (errorCode == ERROR_CONNECT || errorCode == ERROR_HOST_LOOKUP) {
                    binding.errorBanner.visibility = View.VISIBLE
                    binding.errorBanner.text = "Sin conexión al servidor — tocá ↻ para reintentar"
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progress.progress = newProgress
                if (newProgress >= 100) {
                    binding.progress.visibility = View.GONE
                    binding.errorBanner.visibility = View.GONE
                } else {
                    binding.progress.visibility = View.VISIBLE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Cancel any pending callback
                this@WebViewActivity.filePathCallback?.onReceiveValue(null)
                this@WebViewActivity.filePathCallback = filePathCallback

                val rawTypes: Array<String>? = fileChooserParams?.acceptTypes
                val acceptTypes: Array<String> = if (rawTypes == null || rawTypes.isEmpty()) {
                    arrayOf("*/*")
                } else {
                    rawTypes.filter { it.isNotBlank() }.toTypedArray().ifEmpty { arrayOf("*/*") }
                }
                val mime = if (acceptTypes.size == 1 && acceptTypes[0] != "*/*") {
                    acceptTypes[0]
                } else {
                    "*/*"
                }

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mime
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }

                // For images/videos, also offer camera capture
                val chooser = if (mime.startsWith("image/") || mime == "*/*") {
                    val cameraIntent = createCameraIntent()
                    Intent.createChooser(intent, "Elegí archivo").apply {
                        if (cameraIntent != null) {
                            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
                        }
                    }
                } else {
                    Intent.createChooser(intent, "Elegí archivo")
                }

                return try {
                    filePickerLauncher.launch(chooser)
                    true
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    this@WebViewActivity.filePathCallback = null
                    Toast.makeText(this@WebViewActivity, "No se pudo abrir el selector de archivos", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // Grant camera + audio so tools that use getUserMedia work.
                request?.grant(request.resources)
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                binding.fullscreenContainer.addView(view)
                binding.fullscreenContainer.visibility = View.VISIBLE
                binding.webview.visibility = View.GONE
                binding.toolbar.visibility = View.GONE
            }

            override fun onHideCustomView() {
                if (customView == null) return
                binding.fullscreenContainer.removeView(customView)
                binding.fullscreenContainer.visibility = View.GONE
                binding.webview.visibility = View.VISIBLE
                binding.toolbar.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // Handle target=_blank by loading the URL in the same WebView.
                if (resultMsg == null) return false
                val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                val newWeb = WebView(this@WebViewActivity).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                }
                transport.webView = newWeb
                resultMsg.sendToTarget()
                return true
            }
        }

        // Long-press on the toolbar URL bar lets the user copy the URL or
        // change it (debug only). Tap reloads.
        binding.urlBar.setOnClickListener {
            promptForUrl()
        }
    }

    private fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String?
    ) {
        try {
            val guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(url)
            ) ?: mimetype ?: "application/octet-stream"

            val name = URLUtil.guessFileName(url, contentDisposition, guessed)

            // Ask DownloadManager to do it — this respects user prefs and
            // works on all Android versions.
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(guessed)
                addRequestHeader("User-Agent", userAgent)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                setDescription("Descargando $name")
                setTitle(name)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Panoplia/$name")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Descargando: $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo iniciar la descarga: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openDownload(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Archivo guardado en Descargas/Panoplia", Toast.LENGTH_LONG).show()
        }
    }

    private fun createCameraIntent(): Intent? {
        return try {
            val photo = File.createTempFile(
                "panoplia_${System.currentTimeMillis()}",
                ".jpg",
                cacheDir
            )
            cameraUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                photo
            )
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle("¿Cerrar sesión?")
            .setMessage("Se va a borrar tu sesión y volverás al login.")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                // Wipe everything so the next launch hits the login page
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                binding.webview.clearCache(true)
                binding.webview.clearHistory()
                binding.webview.clearFormData()
                WebStorage.getInstance().deleteAllData()
                binding.webview.loadUrl(BuildConfig.WEB_BASE_URL)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateUrlBar(url: String?) {
        if (url == null) return
        binding.urlBar.text = url
    }

    private fun promptForUrl() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(binding.webview.url ?: BuildConfig.WEB_BASE_URL)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Ir a URL")
            .setView(input)
            .setPositiveButton("Ir") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotBlank()) {
                    binding.webview.loadUrl(if (url.startsWith("http")) url else "https://$url")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun requestInitialPermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED
            ) needed.add("android.permission.POST_NOTIFICATIONS")
            if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES")
                != PackageManager.PERMISSION_GRANTED
            ) needed.add("android.permission.READ_MEDIA_IMAGES")
            if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO")
                != PackageManager.PERMISSION_GRANTED
            ) needed.add("android.permission.READ_MEDIA_VIDEO")
            if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_AUDIO")
                != PackageManager.PERMISSION_GRANTED
            ) needed.add("android.permission.READ_MEDIA_AUDIO")
        }
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
            != PackageManager.PERMISSION_GRANTED
        ) needed.add("android.permission.CAMERA")
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

// Mini shim — we don't import the full WebStorage here because it's a
// singleton anyway. Move this out if it grows.
object WebStorage {
    fun getInstance() = android.webkit.WebStorage.getInstance()
    fun deleteAllData() = android.webkit.WebStorage.getInstance().deleteAllData()
}
