package tech.teksavvy.roomviewmobile

import android.app.Activity
import android.app.Fragment
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.android.synthetic.main.room_fragment.*
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.io.Serializable
import java.nio.charset.Charset

class RoomFragment: Fragment(), Serializable{
    var state = true
    lateinit var room:RoomDb.RoomItem
    lateinit var listener: MainActivity

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v =inflater!!.inflate(R.layout.room_fragment, container, false)
        v.findViewById<TextView>(R.id.textView).text = "Room " + room.room +" Connected"
        return v
    }
    fun init(errorIntent: PendingIntent, helpIntent: PendingIntent){
        val intent = Intent(listener, RoomViewService::class.java).apply{
            putExtra("room",room);putExtra("help",helpIntent);putExtra("error",errorIntent)
        }
        listener.startService(intent)
        textView?.setTextColor(Color.BLACK)
        state = true
    }

    fun setError(){
        textView?.setTextColor(Color.RED)
        state = false
    }
}

class RoomViewService: Service(){
    val teststring = "need help".toByteArray(Charset.forName("US-ASCII"))
            .joinToString(separator="") { i -> String.format("%02x",i) }
    //I don't know what this means but the systems require this be sent to them for them to start talking to us
    val data = intArrayOf(0x01,0x00,0x0b,0x0a,0xa6,0xca,0x37,0x00,0x72,0x40,0x00,0x00,0xf1,0x01)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int{
        val args = intent.extras
        val room = args!!.getSerializable("room") as RoomDb.RoomItem
        val error = args.getParcelable("error")  as PendingIntent
        val help = args.getParcelable("help") as PendingIntent
        Thread{
            val socket = Socket(InetAddress.getByName(room.ip), 41794)
            try {
                val input = socket.getInputStream()
                for(i in data){
                    socket.getOutputStream().write(i)
                }
                socket.getOutputStream().flush()
                while(true){
                    val b = ByteArray(20)
                    input.read(b,0,20)
                    var resultstring = ""
                    for(by in b){
                        resultstring += String.format("%02x",by)
                    }
                    if(resultstring.contains(teststring)){
                        help.send(applicationContext, Activity.RESULT_OK, Intent().apply{putExtra("room",room)})
                        val rawIntent =  Intent(this, MainActivity::class.java).apply{addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)}
                        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, rawIntent, 0)
                        val mBuilder = NotificationCompat.Builder(this, "notifications")
                                .setSmallIcon(R.drawable.untitled)
                                .setContentTitle("Roomview Mobile")
                                .setContentText("Help Request From: "+room.room)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setLights(Color.RED, 500,500)
                                .setVibrate(arrayOf<Long>(1000, 1000, 1000).toLongArray())
                                .setAutoCancel(true)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setContentIntent(pendingIntent)

                        NotificationManagerCompat.from(this).notify(Integer.parseInt(room.room.split(".")[1]) , mBuilder.build())
                    }
                }
            }catch(e: IOException){
                error.send(applicationContext, Activity.RESULT_OK, Intent().apply{putExtra("room",room)})
            }
        }.start()

        return Service.START_STICKY
    }
}