package com.example.vinhapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.example.vinhapp.databinding.ActivityEditBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    var MY_REQUEST_CODE = 10
    private var revertImage= RevertImage()
    private var currentIndex=-1
    private lateinit var bitmapMain: Bitmap
    private var imageMain: ImageView?=null
    private lateinit var Activity: EditActivity
    private val url="https://c91a-14-191-36-186.ngrok-free.app/"
    private val urlServer=url+"post"
    private val urlSegment=url+"segment"
    private val urlGetFile=url+"sendfile"
    private val urlDeleteObject=url+"send"
    private var num=0
    private var fileextension="jpg"
    private var mUri: Uri? = null
    private var mfilePath:String?=null
    private var mfileName:String?=null
    private var path:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imgEdit.setOnClickListener{
            onClickRequestPermission()
        }
        enableButton()
        Activity = this
        imageMain=Activity.findViewById(R.id.imgEdit)
        bitmapMain=imageMain!!.drawable.toBitmap()
        revertImage.addStack(bitmapMain)
        binding.btnFind.setOnClickListener{
            Find()
        }
        binding.btnRemove.setOnClickListener{
            Remove()
        }
        binding.btnReturn.setOnClickListener{
            Return()
            enableButton()
        }
        binding.btnForward.setOnClickListener{
            Forward()
            enableButton()
        }
        binding.btnReset.setOnClickListener{
            Reset()
        }

    }
    private fun Reset() {
        revertImage= RevertImage()
        imageMain!!.setImageBitmap(bitmapMain)
        revertImage.addStack(bitmapMain)
        enableButton()
    }

    private fun Forward() {
        val image=revertImage.getNextImage()
        imageMain!!.setImageBitmap(image)
    }

    private fun Return() {
        val image=revertImage.getBackImage()
        imageMain!!.setImageBitmap(image)
    }

    fun RemoveObject(){
        val requestBody: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "index",(revertImage.count-1).toString()
            )
            .addFormDataPart("file_name", "$mfileName")
            .build()
        Log.d("FileName","$mfileName")
        val request: Request = Request.Builder()
            .post(requestBody)
            .url(urlSegment)
            .build()
        var client: OkHttpClient = OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES).build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                val Scontext="Xóa đối tượng thất bại"
                val endContext="Đang gợi ý đối tượng"
                Activity.onFinishDetect(Scontext,endContext)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                val url = URL(urlDeleteObject)
                val image =
                    BitmapFactory.decodeStream(url.openConnection().getInputStream())
                revertImage.addStack(image)
                LoadDeleteImage(image)
            }

        })
    }
    private fun Remove() {
        val context="Đang Xóa đối tượng ..."
        Activity.onStartDetect(context)
        RemoveObject()
    }

    fun segemntObject(){
        val context="Đang gợi ý đối tượng ..."
        Activity.onStartDetect(context)
        val file: File?= File(mfilePath!!)
        uploadFile(urlServer,file!!)
    }
    fun segmentObjectNext(index:Int){
        val context="Đang gợi ý đối tượng ..."
        Activity.onStartDetect(context)
        uploadFile(urlSegment,index)
    }
    fun uploadFile(serverURL: String?, file: File): Boolean? {
        try {

            val requestBody: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", file.name,
                    RequestBody.create("text/csv".toMediaTypeOrNull(), file)
                )
                .build()
            val request: Request = Request.Builder()
                .url(serverURL!!)
                .post(requestBody)
                .build()
            var client: OkHttpClient = OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val Scontext="Gợi ý đối tượng thất bại"
                    val endContext="Đang gợi ý đối tượng"
                    Activity.onFinishDetect(Scontext,endContext)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val num=response.peekBody(2048).string().toInt()
                    }catch (e:NumberFormatException){
                        num=1
                    }
                    Log.d("Send_Success", num.toString())
                    val url = URL(urlGetFile)
                    val image =
                        BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    revertImage.addStack(image)
                    Log.d("ReverImageCount",revertImage.count.toString())
                    this@EditActivity.runOnUiThread(Runnable {
                        val Scontext="Gợi ý đối tượng thành công"
                        val endContext="Đang gợi ý đối tượng"
                        Activity.onFinishDetect(Scontext,endContext)
                        if(image!=null){
                            imageMain!!.setImageBitmap(image)
                        }else{
                            imageMain!!.setImageDrawable(resources.getDrawable(R.drawable.ic_error))
                        }
                        enableButton()
//                        binding.btnDeleteObject.isEnabled=true
                        client.dispatcher.executorService.shutdown()
                    })


                }
            })
            return true
        } catch (ex: java.lang.Exception) {
            // Handle the error
        }
        return false
    }
    fun uploadFile(serverURL: String?,index:Int): Boolean? {
        try {
            val requestBody: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "index",index.toString()
                )
                .addFormDataPart("file_name","$mfileName")
                .build()
            Log.d("Mindex",index.toString())
            val request: Request = Request.Builder()
                .url(serverURL!!)
                .post(requestBody)
                .build()
            var client: OkHttpClient = OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES)
                .cache(null)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val Scontext="Gợi ý đối tượng thất bại"
                    val endContext="Đang gợi ý đối tượng"
                    Activity.onFinishDetect(Scontext,endContext)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result=response.body!!.string()
                    val url = URL(urlGetFile)
                    val image =
                        BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    revertImage.addStack(image)
                    Log.d("ReverImageCount",revertImage.count.toString())
                    Log.d("Send_Success", result)
                    this@EditActivity.runOnUiThread(Runnable {
                        val Scontext="Gợi ý đối tượng thành công"
                        val endContext="Đang gợi ý đối tượng"
                        Activity.onFinishDetect(Scontext,endContext)
                        Log.d("LoadSegmentSuccess",urlGetFile)
                        if(image!=null){
                            imageMain!!.setImageBitmap(image)
                        }else{
                            imageMain!!.setImageDrawable(resources.getDrawable(R.drawable.ic_error))
                        }
                        enableButton()
//                                binding.btnDeleteObject.isEnabled=true
                        client.dispatcher.executorService.shutdown()
//                                val bitmap=BitmapFactory.decodeFile("")
//                                imageMain!!.setImageBitmap(bitmap)
                        // imageMain!!.setImageDrawable(resources.getDrawable(R.drawable.ic_error))
                    })



                }
            })
            return true
        } catch (ex: java.lang.Exception) {
            // Handle the error
        }
        return false
    }
    fun LoadDeleteImage(image:Bitmap){
        this.runOnUiThread(Runnable {
            val Scontext="Xóa đối tượng thành công"
            val endContext="Đang gợi ý đối tượng ..."
            Activity.onFinishDetect(Scontext,endContext)
            imageMain!!.setImageBitmap(image)
            enableButton()

        })
    }
    private fun Find() {
        if(revertImage.listReveer.size<=1){
            Log.d("Send_post","true")
            segemntObject()

        }else{
            Log.d("Send_post_2","true")
            segmentObjectNext(revertImage.count)
        }
    }

    fun enableButton(){
        binding.btnRemove.isEnabled=revertImage.count>=1
        binding.btnReset.isEnabled=revertImage.listReveer.size>0
        binding.btnReturn.isEnabled=revertImage.count>0
        binding.btnForward.isEnabled = revertImage.count<(revertImage.listReveer.size-1)
        if(revertImage.count>0){
            binding.iconReturn.setImageBitmap(resources.getDrawable(R.drawable.icon_undo_enable).toBitmap())
        }else{
            binding.iconReturn.setImageBitmap(resources.getDrawable(R.drawable.icon_undo_unenabe).toBitmap())
        }
        if(revertImage.count<(revertImage.listReveer.size-1)){
            binding.iconForward.setImageBitmap(resources.getDrawable(R.drawable.icon_redo_enable).toBitmap())
        }else{
            binding.iconForward.setImageBitmap(resources.getDrawable(R.drawable.icon_redo_unenable).toBitmap())
        }
    }



    @SuppressLint("UseRequireInsteadOfGet")
    fun onClickRequestPermission() {
        Log.d("mOnclick_img","True")
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
            if (this.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                val permission = arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                this.requestPermissions(permission, MY_REQUEST_CODE)
            }
        }else if (Build.VERSION.SDK_INT >= 30){
            if (!Environment.isExternalStorageManager()){
                val getpermission:Intent =  Intent();
                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(getpermission);
            }else{
                openGallery()
            }} else{
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                this.requestPermissions(permission, MY_REQUEST_CODE)
            }
        }
    }
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode === android.app.Activity.RESULT_OK) {
            val intent = result.data ?: return@registerForActivityResult
            val uri = intent.data
            val docId: String = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val type = split[0]
            val fullPath: String? = getRealPathFromURI_API19(this,uri!!)
            mfileName=getNameFromContentUri(this,uri)
            mfilePath=fullPath
            Log.d("Path_and_name",mfilePath+"_"+mfileName)
            setUri(uri)
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                setBitmapImageView(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    fun getNameFromContentUri(context: Context, contentUri: Uri?): String? {
        val returnCursor: Cursor =
            context.getContentResolver().query(contentUri!!, null, null, null, null)!!
        val nameColumnIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        return returnCursor.getString(nameColumnIndex)
    }
    private fun setBitmapImageView(bitmap: Bitmap?) {
        binding.imgEdit.setImageBitmap(bitmap)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        Log.d("mOnclick_img","True")
        activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }
    fun setUri(mUri: Uri?) {
        this.mUri = mUri
    }
    companion object {
        @JvmStatic
        @SuppressLint("NewApi")
        fun getPath(context: Context, uri: Uri): String? {
            val isKitKat: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    return if ("primary".equals(type, ignoreCase = true)) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    } else { // non-primary volumes e.g sd card
                        var filePath = "non"
                        //getExternalMediaDirs() added in API 21
                        val extenal = context.externalMediaDirs
                        for (f in extenal) {
                            filePath = f.absolutePath
                            if (filePath.contains(type)) {
                                val endIndex = filePath.indexOf("Android")
                                filePath = filePath.substring(0, endIndex) + split[1]
                            }
                        }
                        filePath
                    }
                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                return getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                                  selectionArgs: Array<String>?): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )
            try {
                cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs,
                    null)
                if (cursor != null && cursor.moveToFirst()) {
                    val column_index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(column_index)
                }
            } catch (e: java.lang.Exception) {
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

    }

    @SuppressLint("NewApi")
    fun getRealPathFromURI_API19(context: Context, uri: Uri?): String? {
        var filePath = ""
        val wholeID = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            column, sel, arrayOf(id), null
        )
        val columnIndex = cursor!!.getColumnIndex(column[0])
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex)
        }
        cursor.close()
        return filePath
    }
    fun onStartDetect(context:String)
    {
        binding.txtStatusDetect.text=context
        binding.foreground.isVisible=true
        binding.txtStatusDetect.isVisible=true   }
    fun onFinishDetect(context:String,endContext:String){
        binding.foreground.isVisible=false
        binding.txtStatusDetect.text=context
        Handler().postDelayed(Runnable {
            binding.txtStatusDetect.visibility= View.INVISIBLE
            binding.txtStatusDetect.text=endContext
        },1000)

    }
}