package tech.teksavvy.roomviewmobile

import android.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.room_fragment.*
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.ExecutorService

class RoomFragment: Fragment(){
    var state = true
    var fragRoom:RoomItem? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater!!.inflate(R.layout.room_fragment, container, false)
    }
    fun init(room:RoomItem,teststring:String, initdata:IntArray, listener:MainActivity, executor:ExecutorService){
        fragRoom = room
        executor.submit{
            val socket = Socket(InetAddress.getByName(room.ip), 41794)
            try {
                state = true
                listener.runOnUiThread{textView.setTextColor(Color.BLACK)}
                val input = socket.getInputStream()
                //do whatever this thing does to make it talk to us
                for(i in initdata){
                    socket.getOutputStream().write(i)
                }
                socket.getOutputStream().flush()
                //start listening
                listener.runOnUiThread{textView.text = "Room " + room.room +" Connected"}
                while(true){
                    val b = ByteArray(20)
                    input.read(b,0,20)
                    var resultstring = ""
                    for(by in b){
                        resultstring += String.format("%02x",by)
                    }
                    //System.out.println("room "+ room.room +"sent "+resultstring)
                    //System.out.println(teststring)
                    if(resultstring.contains(teststring)){
                        listener.runOnUiThread {
                            listener.dialog(room)
                            //Toast.makeText(listener, "Help Request from "+room.room, Toast.LENGTH_LONG).show()
                            System.out.println("Help Request from "+room.room)
                        }
                    }
                }
            }catch(e: IOException){
                listener.runOnUiThread{textView.setTextColor(Color.RED)}
                state = false
            }
        }
    }

}