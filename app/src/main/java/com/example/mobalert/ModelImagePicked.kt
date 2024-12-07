package com.example.mobalert

import android.net.Uri

class ModelImagePicked {
    var id = 0
    var imageUri: Uri? = null
    constructor()

    constructor(id: Int, imageUri: Uri?) {
        this.id = id
        this.imageUri = imageUri
    }
}