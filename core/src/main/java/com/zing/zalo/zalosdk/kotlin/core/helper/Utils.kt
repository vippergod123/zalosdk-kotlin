package com.zing.zalo.zalosdk.kotlin.core.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.os.Process
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.jetbrains.annotations.NotNull
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit

object Utils {
    private var language: String? = null

    @SuppressLint("WrongConstant")
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        val permissionCheck = if (Build.VERSION.SDK_INT >= 23) {
            context.checkSelfPermission(permission)
        } else {
            context.packageManager.checkPermission(permission, context.packageName)
        }
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    fun isOnline(ctx: Context): Boolean {
        if (!isPermissionGranted(ctx, Manifest.permission.ACCESS_NETWORK_STATE)) return true

        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }

    fun getLanguage(): String {
        return if (language != null) {
            if (!Locale.getDefault().language.equals("vi", ignoreCase = true)) {
                "my"
            } else {
                "vi"
            }
        } else Locale.getDefault().language
    }

    @Throws(IOException::class)
    fun readFileData(file: File): String {
        val fis = FileInputStream(file)
        val inputStreamReader = InputStreamReader(fis)
        val bufferedReader = BufferedReader(inputStreamReader)

        val stringBuilder = StringBuilder(256)

        var receiveString = bufferedReader.readLine()
        while (receiveString != null) {
            stringBuilder.append(receiveString)
            receiveString = bufferedReader.readLine()
        }

        fis.close()
        return stringBuilder.toString()
    }

    fun isPackageExisted(context: Context, namePackage: String): Boolean {
        val pm = context.packageManager
        try {
            pm.getPackageInfo(namePackage, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

        return true
    }


    fun getMethodQuietly(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        return try {
            clazz.getMethod(methodName, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            Log.w("getMethodQuietly", e)
            null
        }

    }

    fun getMethodQuietly(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method? {
        return try {
            val clazz = Class.forName(className)
            getMethodQuietly(clazz, methodName, *parameterTypes)
        } catch (e: ClassNotFoundException) {
            Log.w("getMethodQuietly", e)
            null
        }

    }

    fun invokeMethodQuietly(receiver: Any?, method: Method, vararg args: Any): Any? {
        return try {
            method.invoke(receiver, *args)
        } catch (e: IllegalAccessException) {
            Log.w("invokeMethodQuietly", e)
            null
        } catch (e: InvocationTargetException) {
            Log.w("invokeMethodQuietly", e)
            null
        }
    }

    fun getSignature(
        params: Array<String>,
        values: Array<String>,
        secretKey: String
    ): String {

        val builder = StringBuilder()
        val list = ArrayList<String>()
        try {
            list.clear()
            val len = params.size
            for (i in 0 until len) {
                list.add(params[i] + "=" + values[i])
            }

            list.sort()
            for (str in list) {
                builder.append(str)
            }
            builder.append(secretKey)
            Log.v("getSignature", "bsig: $builder")
            return md5(builder.toString())
        } catch (ex: Exception) {
            Log.e("Utils: getSignature()", ex)
        }

        return ""

    }

    fun isExternalStorageWritable(context: Context): Boolean {
        val hasPermission = isPermissionGranted(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (!hasPermission) return false
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    fun isExternalStorageReadable(context: Context): Boolean {
        val hasPermission = isPermissionGranted(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (!hasPermission) return false

        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    fun writeToFile(context: Context, content: String, filename: String) {
        // write external storage

        try {
            if (isExternalStorageWritable(context)) {
                val f = prepareFileInExternalStore(filename, true)
                f.createNewFile()
                writeFileData(content, f)
                return
            }
        } catch (e: Exception) {
            Log.e("writeToFile", e)
        }

        // external storage not available, write internal storage
        try {
            val f = File(
                if (Build.VERSION.SDK_INT >= 21) context.noBackupFilesDir else context.filesDir,
                filename
            )
            writeFileData(content, f)
            return
        } catch (e: Exception) {
            Log.e("writeToFile()", e)
        }

        Log.w("writeToFile: cannot write $filename")
    }

    fun readFromFile(context: Context, filename: String): String? {

        try {
            if (isExternalStorageReadable(context)) {
                val file = prepareFileInExternalStore(filename, false)
                if (file.exists()) {
                    return readFileData(file)
                }
            }
        } catch (e: Exception) {
            Log.e("readFromFile()", e)
        }

        // external storage not exists, read from internal storage
        try {
            val file = File(
                if (Build.VERSION.SDK_INT >= 21) context.noBackupFilesDir else context.filesDir,
                filename
            )
            return readFileData(file)
        } catch (ex: FileNotFoundException) {
            Log.w("readFromFile()", "file $filename not found in internal storage")
        } catch (e: Exception) {
            Log.e("readFromFile(): ", e)
        }

        Log.w("readFromFile()", "can't read file $filename")
        return null
    }

    //#region private supportive method
    private fun md5(input: String): String {
        var res = ""
        try {
            val algorithm = MessageDigest.getInstance("MD5")
            algorithm.reset()
            algorithm.update(input.toByteArray())
            val md5 = algorithm.digest()
            var tmp = ""
            for (i in md5.indices) {
                tmp = Integer.toHexString(0xFF and md5[i].toInt())
                res += if (tmp.length == 1) {
                    "0$tmp"
                } else {
                    tmp
                }
            }
        } catch (ex: NoSuchAlgorithmException) {
            Log.e("md5", ex)
        }

        return res
    }

    @Throws(IOException::class)
    private fun writeFileData(content: String, f: File) {
        val fos = FileOutputStream(f)
        val streamWriter = OutputStreamWriter(fos)
        streamWriter.write(content)
        streamWriter.close()
        fos.flush()
        fos.close()
    }

    private fun prepareFileInExternalStore(fileName: String, clearIfExists: Boolean): File {
        val path =
            Environment.getExternalStorageDirectory().absolutePath + "/Android/data/com.google.android.zdt.data/" + fileName
        val f = File(path)
        f.parentFile.mkdirs()

        if (clearIfExists && f.exists()) {
            f.delete()
        }

        return f
    }

    fun getBoolean(obj: JSONObject, key: String): Boolean? {
        if (obj.has(key)) {
            try {
                return obj.getBoolean(key)
            } catch (e: JSONException) {
                try {
                    return obj.getInt(key) != 0
                } catch (ignored: JSONException) {
                }

            }

        }
        return null
    }

    fun getCurrentProcessName(context: Context): String {
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val myPid = Process.myPid()
            for (processInfo in manager.runningAppProcesses) {
                if (processInfo.pid == myPid) {
                    return processInfo.processName
                }
            }

            return context.packageName
        } catch (ex: Exception) {
            return "default"
        }
    }

    fun convertTimeToMilliSeconds(@NotNull time: Int, @NotNull unit: TimeUnit): Long {
        return when (unit) {
            TimeUnit.SECONDS -> time * 1000L
            TimeUnit.HOURS -> time * 3600 * 1000L
            TimeUnit.MINUTES -> time * 60 * 1000L
            else -> time.toLong()
        }
    }

    fun isZaloSupportCallBack(context: Context): Boolean {
        return getVersionCodeOfPackage(context, Constant.ZALO_PACKAGE_NAME) > 1100123
    }

    private fun getVersionCodeOfPackage(oContext: Context, packageId: String): Long {
        try {
            val pInfo = oContext.packageManager.getPackageInfo(packageId, 0)
            if (pInfo != null) {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    pInfo.versionCode.toLong()
                }
            }
        } catch (ex: Exception) {
            Log.w("getVersionCodeOfPackage", ex)
        }

        return -1L
    }
}