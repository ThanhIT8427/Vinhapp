package com.example.vinhapp

import android.graphics.Bitmap

class RevertImage {
    var listReveer:ArrayList<Bitmap> = ArrayList()
    var count=-1
    fun addStack(bitmap:Bitmap){
        listReveer.add(bitmap)
        count++

    }
    fun getNextImage(): Bitmap? {
        count++
        if(count<0||count>(listReveer.size-1)){
            count--
            return null
        }else{
            return listReveer.get(count)
        }
    }
    fun getBackImage():Bitmap?{
        count--
        if(count<0){
            count++
            return null
        }else{
            return listReveer.get(count)
        }
    }
    fun getCurrentImage():Bitmap?{
        if(count<0){
            return null
        }else{
            return listReveer.get(count)
        }
    }



}