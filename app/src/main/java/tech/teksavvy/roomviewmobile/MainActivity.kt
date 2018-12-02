package tech.teksavvy.roomviewmobile

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.widget.Toast
import com.univocity.parsers.annotations.Parsed
import com.univocity.parsers.common.processor.BeanListProcessor
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    val teststring = "need help".toByteArray(Charset.forName("US-ASCII")).joinToString(separator="") { i -> String.format("%02x",i) }
    //must be sent to the unit in order for it to start talking to us
    val data = intArrayOf(0x01,0x00,0x0b,0x0a,0xa6,0xca,0x37,0x00,0x72,0x40,0x00,0x00,0xf1,0x01)
    var fragments = arrayListOf<RoomFragment>().toMutableList()
    var dialog: AlertDialog? = null
    var message = "Help Request From:"
    val service = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("notifications", "roomview_mobile", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "channel for roomview mobile"
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        //check if path to csv is set in preferences
        //if not then we set status text
        //if it is, go ahead and load and connect to all rooms
        val filePath = getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).getString("filePath", "")
        if(filePath == ""){
            status_text.text = "Please select CSV"
        }else{
            val uri = Uri.parse(filePath)
            status_text.text = "CSV Loaded"
            parseCSV(uri)
        }
        load_button.setOnClickListener {
            //open file select
            val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            fileIntent.type = "text/csv"
            startActivityForResult(fileIntent, 1337)
        }

        retry_button.setOnClickListener{
            onRetryClick()
        }
    }

    fun parseCSV(filePath: Uri){
        try {
            val settings = CsvParserSettings()
            val processor = BeanListProcessor<RoomItem>(RoomItem::class.java)
            settings.setProcessor(processor)
            val parser = CsvParser(settings)
            parser.parse(contentResolver.openInputStream(filePath))
            val rooms: List<RoomItem> = processor.beans
            loadRooms(rooms)
        }catch(e: FileNotFoundException) {
            status_text.text = "Please select CSV"
            Toast.makeText(this, "Currently selected file does not exist", Toast.LENGTH_LONG).show()
            getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().remove("filePath").apply()
        }
    }

    fun loadRooms(rooms: List<RoomItem>){
        for(frag in fragments){
            fragmentManager.beginTransaction().remove(frag).commit()
        }
        for(room in rooms){
            val frag = RoomFragment()
            frag.init(room, teststring, data, this, service)
            fragmentManager.beginTransaction().add(room_list.id, frag).commit()
            fragments.add(frag)
        }
    }

    fun onRetryClick(){
        for(frag in fragments){
            if(!frag.state){
                frag.init(frag.fragRoom!!, teststring, data, this, service)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        if(requestCode == 1337 && resultCode == Activity.RESULT_OK && data != null) {
            val path = data.data
            getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().putString("filePath", path.toString()).apply()
            parseCSV(path)
        }
    }

    fun dialog(room:RoomItem){
        message += "\n" + room.room
        if(dialog == null) {
            dialog = AlertDialog.Builder(this).setMessage(message).setTitle("Help Request")
                    .setPositiveButton("OK") { _, _ -> dialog = null; message = "Help Request From:"}.create()
            dialog!!.show()
        }else{
            dialog!!.setMessage(message)
        }
        //TODO: show notification and vibrate
        val mBuilder = NotificationCompat.Builder(this, "notifications")
                .setSmallIcon(R.drawable.notification_icon_background)
                .setContentTitle("Roomview Mobile")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setLights(Color.RED, 500,500)
                .setVibrate(arrayOf<Long>(1000, 1000, 1000).toLongArray())
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, mBuilder.build())
        }
    }
}

class RoomItem {
    @Parsed
    val ip:String = ""
    @Parsed
    val room:String = ""
}