package com.radio_majak

import android.Manifest
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Runnable
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*


private const val MEDIA_SOURCE_LINK_LINK = "https://icecast-vgtrk.cdnvideo.ru/mayakfm"
lateinit var downloadManager: DownloadManager


private var isPlaying: Boolean = false
private var isRecording: Boolean = false
private lateinit var scrollListener: RecyclerView.OnScrollListener
private lateinit var wakeLock: PowerManager.WakeLock
private lateinit var request: DownloadManager.Request

class MainActivity : AppCompatActivity() {


    companion object {
        lateinit var player: MediaPlayer
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        val mContext = applicationContext
        val powerManager = mContext.getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "motionDetection:keepAwake")
        wakeLock.acquire(60 * 60 * 1000L /*60 minutes*/)
        initializeUIElements()
        downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        request = DownloadManager.Request(Uri.parse(MEDIA_SOURCE_LINK_LINK))

    }

    private fun initializeUIElements() {

        initializeVolume()


        ib_play.setOnClickListener {
            if (isPlaying) {
                stopPlaying()
                setBackGround(ib_play, R.drawable.ic_on)
                isPlaying = false
            } else {
                startPlaying()
                setBackGround(ib_play, R.drawable.ic_off)
                ib_play.isEnabled = false

            }
        }

        ib_record.setOnClickListener {
            if (isStoragePermissionGranted())
                isRecording = if (isRecording) {
                    stopRecord()
                    setBackGround(ib_record, R.drawable.ic_button_rec)
                    false
                } else {
                    startRecord(MEDIA_SOURCE_LINK_LINK)
                    setBackGround(ib_record, R.drawable.ic_button_rec_off)
                    true
                    //ib_record.isEnabled = false

                }
        }


    }


    private fun setBackGround(v: View, i: Int) {
        v.setBackgroundResource(i)
    }


    private fun startPlaying() {

        player.prepareAsync()

        player.setOnPreparedListener {


            val mHandler = Handler()
            mHandler.postDelayed(Runnable {
                run {
                    player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    player.start()
                    isPlaying = true
                    ib_play.isEnabled = true
                }
            }, player.duration.toLong())
        }

    }


    private fun stopPlaying() {
        if (player.isPlaying) {
            player.stop()
            player.release()
            initializeMediaPlayer()
        }

        ib_play.isEnabled = true

    }

    private fun initializeMediaPlayer() {
        player = MediaPlayer()
        try {
            player.setDataSource(MEDIA_SOURCE_LINK_LINK)

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        player.isLooping = true
        player.setVolume(0.1f, 0.1f)


    }

    private fun initializeVolume() {
        val groupAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(List(100) { ListItem() })
        }

        recycler_volume.setHasFixedSize(true)
        recycler_volume.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler_volume.adapter = groupAdapter
        setRecyclerViewScrollListener()

    }


    override fun onPause() {
        super.onPause()

        player.start()

    }

    override fun onResume() {
        super.onResume()
        initializeMediaPlayer()


    }


//    override fun onDestroy() {
//        super.onDestroy()
//        if (player!= null) player.release()
//    }

    private fun setRecyclerViewScrollListener() {

        scrollListener = object : RecyclerView.OnScrollListener() {
            var volumeValue: Int = 0
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recycler_volume, newState)

            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                volumeValue += dx
                setRadioVolume(volumeValue)
            }

        }
        recycler_volume.addOnScrollListener(scrollListener)
    }

    private fun setRadioVolume(volumeValue: Int) {
        if (isPlaying) {
            player.setVolume(volumeValue.toFloat() / 3000, volumeValue.toFloat() / 3000)
            println(volumeValue.toFloat())
        }
    }

    private fun startRecord(urls: String) {
        val title = System.currentTimeMillis().toString()
        DownloadFileAsync().execute(urls)
    }


    private fun stopRecord() {
        DownloadFileAsync().cancel(true)
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG", "Permission is granted")
                true
            } else {
                Log.v("TAG", "Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG", "Permission is granted")
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("TAG", "Permission: " + permissions[0] + "was " + grantResults[0])
            startRecord(MEDIA_SOURCE_LINK_LINK)
            //resume tasks needing this permission
        }
    }


    class ListItem : Item() {
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.apply {
            }
        }

        override fun getLayout() = R.layout.recycler_item
    }


}

internal class DownloadFileAsync : AsyncTask<String?, String?, String?>() {
    lateinit var input: InputStream
    override fun onPreExecute() {
        super.onPreExecute()

    }

    protected override fun doInBackground(vararg aurl: String?): String? {
        var count: Int
        val title = SimpleDateFormat("dd.MM.yyyy").format(Date(System.currentTimeMillis()))
        try {
            val url = URL(aurl[0])
            val urlConnection: URLConnection = url.openConnection()
            urlConnection.connect()

            input = BufferedInputStream(url.openStream())
            val output: OutputStream = FileOutputStream("/sdcard/запись радиостанции Маяк $title.mp3")
            val data = ByteArray(1024)

            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
        } catch (e: Exception) {

        }
        return null
    }






    override fun onPostExecute(unused: String?) {
        input.close()

    }
}



