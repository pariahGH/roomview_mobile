package tech.teksavvy.roomviewmobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.univocity.parsers.annotations.Parsed
import com.univocity.parsers.common.processor.BeanListProcessor
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.Charset
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {
    var roomSockets = mapOf<Socket, RoomItem>().toMutableMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //check if path to csv is set in preferences
        //if not then we set status text
        //if it is, go ahead and load and connect to all rooms
        val filePath = getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).getString("filePath", "")
        if(filePath == ""){
            status_text.text = "Please select CSV"
        }else{
            status_text.text = "CSV Loaded"
            loadRooms(filePath)
        }
        load_button.setOnClickListener {
            //open file select
            val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
            fileIntent.type = "file/csv"
            startActivityForResult(fileIntent, 1337)
        }

        loadRooms("")
    }

    fun loadRooms(filePath:String){
        //parse CSV
        /*val settings = CsvParserSettings()
        val processor = BeanListProcessor<RoomItem>(RoomItem::class.java)
        settings.setProcessor(processor)
        val parser = CsvParser(settings)
        parser.parse(File(filePath))
        val rooms:List<RoomItem> = processor.beans*/
        val rooms = listOf(RoomItem("10.166.202.55","2.102"))
        for(room in rooms){
            //launch thread for the room
            //TODO: make sure to enable multiple simultaneous threads
            Thread{
                System.out.println("foobar")
                val socket = Socket(InetAddress.getByName(room.IP), 41794)
                roomSockets[socket] = room
                val input = socket.getInputStream()
                //do whatever this thing does to make it talk to us
                val data = intArrayOf(0x01,0x00,0x0b,0x0a,0xa6,0xca,0x37,0x00,0x72,0x40,0x00,0x00,0xf1,0x01)
                for(i in data ){
                    socket.getOutputStream().write(i)
                }
                socket.getOutputStream().flush()
                //start listening
                while(true){
                    val b = ByteArray(20)
                    input.read(b,0,20)
                    val test = "need help".toByteArray(Charset.forName("US-ASCII"))
                    var teststring = ""
                    for(by in test){
                        //seems to be working but is droping leading zeros!
                        teststring += String.format("%02x",by)
                    }
                    var resultstring = ""
                    for(by in b){
                        //seems to be working but is droping leading zeros!
                        resultstring += String.format("%02x",by)
                    }
                    if(resultstring.contains(teststring)){
                        //System.out.println("Help Request Received")
                        this.runOnUiThread { Toast.makeText(this, "Help Request from "+room.Room,Toast.LENGTH_LONG).show() }
                    }
                    //System.out.println(resultstring)
                }
            }.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent){
        if(requestCode == 1337 && resultCode == Activity.RESULT_OK) {
            val path = data.data.path
            getSharedPreferences("roomview_mobile", Context.MODE_PRIVATE).edit().putString("filePath", path).apply()
            loadRooms(path)
        }
    }
}

class RoomItem constructor(i:String, r: String){
    @Parsed
    val IP:String = i
    val Room:String = r
}