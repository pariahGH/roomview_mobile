package tech.teksavvy.roomviewmobile

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.univocity.parsers.common.processor.BeanListProcessor
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.Serializable
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), Serializable {
    private var fragments = arrayListOf<RoomFragment>().toMutableList()
    private var dialog: AlertDialog? = null
    var message = "Help Request From:"
    val database :RoomDb.RoomDao by lazy { (RoomDb.RoomViewDb.getInstance(this)).roomDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dirs = getExternalFilesDirs(null)
        val path = if(dirs.size == 2) dirs[1] else dirs[0]
        val logFile = File(path.absolutePath+"/logfile.txt")
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            System.out.println(throwable.message)
            logFile.writeText(throwable.message+"\n")
            Toast.makeText(this@MainActivity, "Uncaught exception logged", Toast.LENGTH_LONG).show()
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

        val loaded = getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).getBoolean("loaded", false)
        if(loaded){
            loadRooms(null)
            status_text.text = "Rooms loaded"
        }else{
            status_text.text = "Please load rooms"
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
            val processor = BeanListProcessor<RoomDb.RoomItem>(RoomDb.RoomItem::class.java)
            settings.setProcessor(processor)
            val parser = CsvParser(settings)
            parser.parse(contentResolver.openInputStream(filePath))
            doAsync{
                database.deleteAllRooms()
                processor.beans.map{
                    database.insertRoom(it)
                }
                this.runOnUiThread {
                    getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().putBoolean("loaded", true).apply()
                    loadRooms(processor.beans)
                }
            }.execute()
        }catch(e: FileNotFoundException) {
            status_text.text = "Please select CSV"
            Toast.makeText(this, "Currently selected file does not exist", Toast.LENGTH_LONG).show()
            getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().remove("filePath").apply()
        }
    }

    private fun loadRooms(rooms : List<RoomDb.RoomItem>?){
        fragments.map{fragmentManager.beginTransaction().remove(it).commit()}
        doAsync {
            (rooms ?: database.getRooms()).map { room ->
                val frag = RoomFragment()
                frag.room = room
                frag.listener = this
                val helpIntent = createPendingResult(2, Intent(), 0)
                val errorIntent = createPendingResult(1, Intent(), 0)
                this.runOnUiThread{
                    fragmentManager.beginTransaction().add(room_list.id, frag).commit()
                    fragments.add(frag)
                    frag.init(errorIntent, helpIntent)
                }
            }
        }.execute()
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
                1 -> fragments.filter{it.room.ip == (data.getSerializableExtra("room") as RoomDb.RoomItem).ip}[0].setError()
                2 -> dialog(data.extras!!.getSerializable("room") as RoomDb.RoomItem)
                1337 -> {
                    val path = data.data!!
                    contentResolver.takePersistableUriPermission(path, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().putString("filePath", path.toString()).apply()
                    parseCSV(path)
                }
            }
        }
    }

    private fun dialog(room:RoomDb.RoomItem){
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

class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}
