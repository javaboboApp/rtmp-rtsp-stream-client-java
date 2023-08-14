package com.river.apollo.webserver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtplibrary.rtsp.RtspCamera1
import com.pedro.rtsp.rtsp.Protocol
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.river.apollo.R
import com.river.apollo.utils.NetworkUtils
import com.river.apollo.utils.PathUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WebServerViewActivity : AppCompatActivity(), View.OnClickListener, ConnectCheckerRtsp, SurfaceHolder.Callback,
    View.OnTouchListener {
    private var server: WebServer? = null

    private val orientations = arrayOf(0, 90, 180, 270)

    private var rtspCamera1: RtspCamera1? = null
    private var surfaceView: SurfaceView? = null
    private var bStartStop: Button? = null
    private  var bRecord:android.widget.Button? = null
    private var currentDateAndTime = ""
    private var folder: File? = null

    //options menu
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
    private var rgChannel: RadioGroup? = null
    private var rbTcp: RadioButton? = null
    private  var rbUdp:RadioButton? = null
    private var spResolution: Spinner? = null
    private var cbEchoCanceler: CheckBox? = null
    private  var cbNoiseSuppressor:CheckBox? = null
    private var etVideoBitrate: EditText? = null
    private  var etFps:EditText? = null
    private  var etAudioBitrate:EditText? = null
    private  var etSampleRate:EditText? = null
    private  var etWowzaUser:EditText? = null
    private var etWowzaPassword: EditText? = null
    private var lastVideoBitrate: String? = null
    private var tvBitrate: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_server_view)
        setUpWebCamLogic()
        initListeners()
        initViews()
    }

    private fun initViews() {
         findViewById<TextView>(R.id.ip_tv).text = "http://${NetworkUtils.getLocalIpAddress(this)}:8080"
    }


    private fun setUpWebCamLogic() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        folder = PathUtils.getRecordPath()

        surfaceView = findViewById(R.id.surfaceView)
        surfaceView?.getHolder()?.addCallback(this)
        surfaceView?.setOnTouchListener(this)
        rtspCamera1 = RtspCamera1(surfaceView, this)
        prepareOptionsMenuViews()

        tvBitrate = findViewById(R.id.tv_bitrate)
//        bStartStop = findViewById(R.id.b_start_stop)
//        bStartStop?.setOnClickListener(this)
//        bRecord = findViewById(R.id.b_record)
//        bRecord?.setOnClickListener(this)
        val switchCamera = findViewById<Button>(R.id.switch_camera)
        switchCamera.setOnClickListener(this)
    }
    private fun prepareOptionsMenuViews() {
        drawerLayout = findViewById(R.id.activity_custom)
        navigationView = findViewById(R.id.nv_rtp)
        navigationView?.inflateMenu(R.menu.options_rtsp)
        actionBarDrawerToggle = object : ActionBarDrawerToggle(
            this, drawerLayout, R.string.rtsp_streamer,
            R.string.rtsp_streamer
        ) {
            override fun onDrawerOpened(drawerView: View) {
                actionBarDrawerToggle!!.syncState()
                lastVideoBitrate = etVideoBitrate!!.text.toString()
            }

            override fun onDrawerClosed(view: View) {
                actionBarDrawerToggle!!.syncState()
                if (lastVideoBitrate != null && lastVideoBitrate != etVideoBitrate!!.text.toString() && rtspCamera1!!.isStreaming
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        val bitrate = etVideoBitrate!!.text.toString().toInt() * 1024
                        rtspCamera1!!.setVideoBitrateOnFly(bitrate)
                        Toast.makeText(
                            this@WebServerViewActivity,
                            "New bitrate: $bitrate",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@WebServerViewActivity, "Bitrate on fly ignored, Required min API 19",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        drawerLayout?.addDrawerListener(actionBarDrawerToggle as ActionBarDrawerToggle)
        //checkboxs
        cbEchoCanceler =
            navigationView?.getMenu()?.findItem(R.id.cb_echo_canceler)?.actionView as CheckBox?
        cbNoiseSuppressor =
            navigationView?.getMenu()?.findItem(R.id.cb_noise_suppressor)?.actionView as CheckBox?
        //radiobuttons
        rbTcp = navigationView?.getMenu()?.findItem(R.id.rb_tcp)?.actionView as RadioButton?
//        rbUdp = navigationView?.getMenu()?.findItem(R.id.rb_udp)?.actionView as RadioButton?
        rgChannel = navigationView?.getMenu()?.findItem(R.id.channel)?.actionView as RadioGroup?
        rbTcp!!.isChecked = true
        rbTcp!!.setOnClickListener(this)
//        rbUdp!!.setOnClickListener(this)
        //spinners
        spResolution = navigationView?.getMenu()?.findItem(R.id.sp_resolution)?.actionView as Spinner?
        val orientationAdapter =
            ArrayAdapter<Int>(this, android.R.layout.simple_spinner_dropdown_item)
        orientationAdapter.addAll(*orientations)
        val resolutionAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item)
        val list: MutableList<String> = ArrayList()
        for (size in rtspCamera1!!.resolutionsBack) {
            list.add(size.width.toString() + "X" + size.height)
        }
        resolutionAdapter.addAll(list)
        spResolution!!.adapter = resolutionAdapter
        //edittexts
        etVideoBitrate =
            navigationView?.getMenu()?.findItem(R.id.et_video_bitrate)?.actionView as EditText?
        etFps = navigationView?.getMenu()?.findItem(R.id.et_fps)?.actionView as EditText?
        etAudioBitrate =
            navigationView?.getMenu()?.findItem(R.id.et_audio_bitrate)?.actionView as EditText?
        etSampleRate = navigationView?.getMenu()?.findItem(R.id.et_samplerate)?.actionView as EditText?
        etVideoBitrate!!.setText("2500")
        etFps!!.setText("30")
        etAudioBitrate!!.setText("128")
        etSampleRate!!.setText("44100")
        etWowzaUser = navigationView?.getMenu()?.findItem(R.id.et_user)?.actionView as EditText?
        etWowzaPassword =
            navigationView?.getMenu()?.findItem(R.id.et_password)?.actionView as EditText?
    }

    private fun initListeners() {
        findViewById<Button>(R.id.start_webserver_bt).setOnClickListener {
            // Start the server
            startWebServer()
            Toast.makeText(this, "Webserver started !!", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.ip_tv).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.stop_webserver_bt).setOnClickListener {
            // Stop the server
            stopWebserver()
            Toast.makeText(this, "Webserver stopped !!", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.ip_tv).visibility = View.GONE
        }
    }

    private fun stopWebserver() {
        server?.stop()
    }

    private fun startWebServer() {
        try {
            server = WebServer( port = 8080, streamingUrl = "") // Use your desired port


            server?.start()


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    override fun onConnectionStartedRtsp(rtspUrl: String) {}

    override fun onConnectionSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@WebServerViewActivity, "Connection success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtsp(reason: String) {
        runOnUiThread {
            Toast.makeText(this@WebServerViewActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
                .show()
            rtspCamera1!!.stopStream()
            bStartStop!!.text = resources.getString(R.string.start_button)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtspCamera1!!.isRecording
            ) {
                rtspCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                bRecord!!.setText(R.string.start_record)
                Toast.makeText(
                    this@WebServerViewActivity,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
    }

    override fun onNewBitrateRtsp(bitrate: Long) {
        runOnUiThread { tvBitrate!!.text = "$bitrate bps" }
    }

    override fun onDisconnectRtsp() {
        runOnUiThread {
            Toast.makeText(this@WebServerViewActivity, "Disconnected", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtspCamera1!!.isRecording
            ) {
                rtspCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                bRecord!!.setText(R.string.start_record)
                Toast.makeText(
                    this@WebServerViewActivity,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
    }

    override fun onAuthErrorRtsp() {
        runOnUiThread {
            bStartStop!!.text = resources.getString(R.string.start_button)
            rtspCamera1!!.stopStream()
            Toast.makeText(this@WebServerViewActivity, "Auth error", Toast.LENGTH_SHORT).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && rtspCamera1!!.isRecording
            ) {
                rtspCamera1!!.stopRecord()
                PathUtils.updateGallery(
                    applicationContext,
                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                )
                bRecord!!.setText(R.string.start_record)
                Toast.makeText(
                    this@WebServerViewActivity,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
    }

    override fun onAuthSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@WebServerViewActivity, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }


  override  fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        drawerLayout!!.openDrawer(GravityCompat.START)
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        rtspCamera1!!.startPreview()
        // optionally:
        //rtspCamera1.startPreview(CameraHelper.Facing.BACK);
        //or
        //rtspCamera1.startPreview(CameraHelper.Facing.FRONT);
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtspCamera1!!.isRecording) {
            rtspCamera1!!.stopRecord()
            PathUtils.updateGallery(this, folder!!.absolutePath + "/" + currentDateAndTime + ".mp4")
            bRecord!!.setText(R.string.start_record)
            Toast.makeText(
                this,
                "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                Toast.LENGTH_SHORT
            ).show()
            currentDateAndTime = ""
        }
        if (rtspCamera1!!.isStreaming) {
            rtspCamera1!!.stopStream()
            bStartStop!!.text = resources.getString(R.string.start_button)
        }
        rtspCamera1!!.stopPreview()
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action
        if (motionEvent.pointerCount > 1) {
            if (action == MotionEvent.ACTION_MOVE) {
                rtspCamera1!!.setZoom(motionEvent)
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            rtspCamera1!!.tapToFocus(view, motionEvent)
        }
        return true
    }


    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.b_start_stop) {
            if (!rtspCamera1!!.isStreaming) {
                bStartStop!!.text = resources.getString(R.string.stop_button)
                if (rbTcp!!.isChecked) {
                    rtspCamera1!!.setProtocol(Protocol.TCP)
                } else {
                    rtspCamera1!!.setProtocol(Protocol.UDP)
                }
                val user = etWowzaUser!!.text.toString()
                val password = etWowzaPassword!!.text.toString()
                if (!user.isEmpty() && !password.isEmpty()) {
                    rtspCamera1!!.setAuthorization(user, password)
                }
                if (rtspCamera1!!.isRecording || prepareEncoders()) {
//                    rtspCamera1!!.startStream(etUrl!!.text.toString())
                } else {
                    //If you see this all time when you start stream,
                    //it is because your encoder device dont support the configuration
                    //in video encoder maybe color format.
                    //If you have more encoder go to VideoEncoder or AudioEncoder class,
                    //change encoder and try
                    Toast.makeText(
                        this, "Error preparing stream, This device cant do it",
                        Toast.LENGTH_SHORT
                    ).show()
                    bStartStop!!.text = resources.getString(R.string.start_button)
                }
            } else {
                bStartStop!!.text = resources.getString(R.string.start_button)
                rtspCamera1!!.stopStream()
            }
        } else if (id == R.id.b_record) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (!rtspCamera1!!.isRecording) {
                    try {
                        if (!folder!!.exists()) {
                            folder!!.mkdir()
                        }
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        currentDateAndTime = sdf.format(Date())
                        if (!rtspCamera1!!.isStreaming) {
                            if (prepareEncoders()) {
                                rtspCamera1!!.startRecord(
                                    folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                                )
                                bRecord!!.setText(R.string.stop_record)
                                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    this, "Error preparing stream, This device cant do it",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            rtspCamera1!!.startRecord(
                                folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                            )
                            bRecord!!.setText(R.string.stop_record)
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: IOException) {
                        rtspCamera1!!.stopRecord()
                        PathUtils.updateGallery(
                            this,
                            folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                        )
                        bRecord!!.setText(R.string.start_record)
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    rtspCamera1!!.stopRecord()
                    PathUtils.updateGallery(
                        this,
                        folder!!.absolutePath + "/" + currentDateAndTime + ".mp4"
                    )
                    bRecord!!.setText(R.string.start_record)
                    Toast.makeText(
                        this,
                        "file " + currentDateAndTime + ".mp4 saved in " + folder!!.absolutePath,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this, "You need min JELLY_BEAN_MR2(API 18) for do it...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (id == R.id.switch_camera) {
            try {
                rtspCamera1!!.switchCamera()
            } catch (e: CameraOpenException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            //options menu
        } else if (id == R.id.rb_tcp) {
            if (rbUdp!!.isChecked) {
                rbUdp!!.isChecked = false
                rbTcp!!.isChecked = true
            }
        }

//        else if (id == R.id.rb_udp) {
//            if (rbTcp!!.isChecked) {
//                rbTcp!!.isChecked = false
//                rbUdp!!.isChecked = true
//            }
//        }
    }

    private fun prepareEncoders(): Boolean {
        val resolution = rtspCamera1!!.resolutionsBack[spResolution!!.selectedItemPosition]
        val width = resolution.width
        val height = resolution.height
        return rtspCamera1!!.prepareVideo(
            width, height, etFps!!.text.toString().toInt(),
            etVideoBitrate!!.text.toString().toInt() * 1024,
            CameraHelper.getCameraOrientation(this)
        ) && rtspCamera1!!.prepareAudio(
            etAudioBitrate!!.text.toString().toInt() * 1024, etSampleRate!!.text.toString().toInt(),
            rgChannel!!.checkedRadioButtonId == R.id.rb_stereo, cbEchoCanceler!!.isChecked,
            cbNoiseSuppressor!!.isChecked
        )
    }


}