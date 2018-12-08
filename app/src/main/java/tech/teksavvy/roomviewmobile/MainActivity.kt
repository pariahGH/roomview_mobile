package tech.teksavvy.roomviewmobile

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.univocity.parsers.annotations.Parsed
import com.univocity.parsers.common.processor.BeanListProcessor
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException
import java.io.Serializable
import java.nio.charset.Charset

class MainActivity : AppCompatActivity(), Serializable {
    var fragments = arrayListOf<RoomFragment>().toMutableList()
    var dialog: AlertDialog? = null
    var message = "Help Request From:"

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
            frag.room = room
            frag.listener = this
            val helpIntent = createPendingResult(2, Intent(), 0)
            val errorIntent = createPendingResult(1, Intent(), 0)
            fragmentManager.beginTransaction().add(room_list.id, frag).commit()
            fragments.add(frag)
            frag.init(errorIntent, helpIntent)
        }
    }

    fun onRetryClick(){
        for(frag in fragments){
            if(!frag.state){
                val helpIntent = createPendingResult(2, Intent(), 0)
                val errorIntent = createPendingResult(1, Intent(), 0)
                frag.init(errorIntent, helpIntent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        if(resultCode == Activity.RESULT_OK && data != null){
            when(requestCode){
                1 -> fragments.filter{it.room.ip == (data.getSerializableExtra("room") as RoomItem).ip}[0].setError()
                2 -> dialog(data.extras.getSerializable("room") as RoomItem)
                1337 -> {
                    val path = data.data
                    getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().putString("filePath", path.toString()).apply()
                    parseCSV(path)
                }
            }
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
    }
}

class RoomItem : Serializable{
    @Parsed
    val ip:String = ""
    @Parsed
    val room:String = ""
    val teststring = "need help".toByteArray(Charset.forName("US-ASCII")).joinToString(separator="") { i -> String.format("%02x",i) }
    val data = intArrayOf(0x01,0x00,0x0b,0x0a,0xa6,0xca,0x37,0x00,0x72,0x40,0x00,0x00,0xf1,0x01)
}