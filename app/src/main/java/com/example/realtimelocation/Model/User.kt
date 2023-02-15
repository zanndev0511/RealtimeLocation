package com.example.realtimelocation.Model

class User {
    var uid:String? = null
    var email:String? = null
    var acceptList:HashMap<String,User>? = null

    constructor(){}

    constructor(uid:String, email:String){
        this.uid = uid
        this.email = email
        acceptList = HashMap()
    }
}