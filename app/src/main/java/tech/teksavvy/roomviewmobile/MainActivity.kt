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
import java.io.File
import java.io.FileNotFoundException
import java.io.Serializable
import java.nio.charset.Charset

class MainActivity : AppCompatActivity(), Serializable {
    private var fragments = arrayListOf<RoomFragment>().toMutableList()
    private var dialog: AlertDialog? = null
    var message = "Help Request From:"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dirs = getExternalFilesDirs(null)
        val path = if(dirs.size == 2) dirs[1] else dirs[0]
        val logFile = File(path.absolutePath+"/logfile.txt")
        System.out.println(path)
        getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().remove("filePath").apply()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            logFile.writeText(throwable.message+"\n")
            Toast.makeText(this, "Uncaight exception logged", Toast.LENGTH_LONG).show()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("notifications", "roomview_mobile", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "channel for roomview mobile"
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val filePath = getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).getString("filePath", "")
        if(filePath == ""){
            status_text.text = "Please select CSV"
        }else{
            val uri = Uri.parse(filePath)
            status_text.text = "CSV Loaded"
            parseCSV(uri)
        }
        load_button.setOnClickListener {
            val fileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }
            fileIntent.type = "text/csv"
            startActivityForResult(fileIntent, 1337)
        }

        retry_button.setOnClickListener{
            onRetryClick()
        }
    }

    private fun parseCSV(filePath: Uri){
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

    private fun loadRooms(rooms: List<RoomItem>){
        fragments.map{fragmentManager.beginTransaction().remove(it).commit()}
        rooms.map{room ->
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

    private fun onRetryClick(){
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
                    contentResolver.takePersistableUriPermission(data.data, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().putString("filePath", path.toString()).apply()
                    parseCSV(path)
                }
            }
        }
    }

    private fun dialog(room:RoomItem){
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
    val teststring = "need help".toByteArray(Charset.forName("US-ASCII"))
            .joinToString(separator="") { i -> String.format("%02x",i) }
    //I don't know what this means but the systems require this be sent to them for them to start talking to us
    val data = intArrayOf(0x01,0x00,0x0b,0x0a,0xa6,0xca,0x37,0x00,0x72,0x40,0x00,0x00,0xf1,0x01)
}