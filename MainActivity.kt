package com.example.android.disclrucache

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Environment.isExternalStorageRemovable
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.jakewharton.disklrucache.DiskLruCache
import java.io.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock



class MainActivity : AppCompatActivity() {

    val TAG = MainActivity::class.java.simpleName
    private  val DISK_CACHE_SIZE = 1024 * 1024 * 10 // 10MB
    private  val DISK_CACHE_SUBDIR = "thumbnails"
    private var mDiskLruCache: DiskLruCache? = null
    private val mDiskCacheLock = ReentrantLock()
    private val mDiskCacheLockCondition: Condition = mDiskCacheLock.newCondition()
    private var mDiskCacheStarting = true
    val country = Country("Latvia", 50)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val cacheDir = getDiskCacheDir(this, DISK_CACHE_SUBDIR)
        InitDiskCacheTask().start(cacheDir)

        writeToDiskCache("New country", country)
        val newCountry = readFromDiskCache("New country")
        Log.v(TAG, newCountry.toString())

    }

    internal inner class InitDiskCacheTask  {
        fun start(vararg params: File): Void? {
                Log.v(TAG, "create LRU cache in:" + cacheDir.getAbsolutePath());
                val cacheDir = params[0]
                mDiskLruCache = DiskLruCache.open(cacheDir, DISK_CACHE_SIZE, 1, 10 * 1024 * 1024)
                mDiskCacheStarting = false // Finished initialization

            return null
        }
    }


    protected fun writeToDiskCache(key: String, country: Country): Boolean {
            var isOk = false
            if (mDiskLruCache != null) {
                val cacheKey = key.hashCode().toString()
                try {

                    var editor = mDiskLruCache?.edit(cacheKey)
                    if (editor != null) {
                        val out = editor!!.newOutputStream(0)
                        val oos = ObjectOutputStream(out)
                        oos.writeObject(country);
                        oos.close()
                        out.close()
                        editor!!.commit()
                        //Log.d(Constants.TAG_EMOP, "write to disk key:" + cacheKey + ", url:" + url.toString());
                        isOk = true
                        editor = null
                    }
                } catch (e: IOException) {
                    isOk = false
                    Log.v(TAG, "write disk lru cache error:" + e.toString(), e)
                }

            } else {
                Log.v(TAG, "read disk lru cache is null")
            }
            return isOk

    }


    protected fun readFromDiskCache(key: String): Country? {
            var country: Country? = null
            if (mDiskLruCache != null) {
                //diskCache.flush()
                val cacheKey = key.hashCode().toString()
                try {
                    var snapshot = mDiskLruCache?.get(cacheKey)
                    if (snapshot != null) {
                        val input: InputStream = snapshot!!.getInputStream(0)
                        val inputObject: ObjectInputStream = ObjectInputStream(input)
                        country = inputObject.readObject() as Country?
                        inputObject.close()
                        input.close()
                        snapshot!!.close()
                        snapshot = null
                        //Log.d(Constants.TAG_EMOP, "read from disk key:" + cacheKey + ", url:" + url.toString());
                    } else {
                        Log.v(TAG, "read disk lru cache error: snapshot is null")
                    }
                } catch (e: IOException) {
                    Log.v(TAG, "read disk lru cache error:" + e.toString(), e)
                }

            } else {
                Log.v(TAG, "read disk lru cache is null")
            }

            return country

    }



    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
// but if not mounted, falls back on internal storage.
    fun getDiskCacheDir(context: Context, uniqueName: String): File {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        val cachePath =
                if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
                        || !isExternalStorageRemovable()) {
                    context.externalCacheDir.path
                } else {
                    context.cacheDir.path
                }

        return File(cachePath + File.separator + uniqueName)
    }



}


data class Country(val name: String, val rate: Int): Serializable{

}