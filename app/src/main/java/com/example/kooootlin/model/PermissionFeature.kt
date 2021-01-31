package com.example.kooootlin.model

import java.io.Serializable

class PermissionFeature : Serializable {
    var featureKey: String
    var intentRequestCode = 0
    var permissions: List<String>? = null
    var messageAskPermission: String? = null

    constructor(featureKey: String) {
        this.featureKey = featureKey
    }

    constructor(
        featureKey: String,
        intentRequestCode: Int,
        permissions: List<String>?,
        messageAskPermission: String?
    ) {
        this.featureKey = featureKey
        this.intentRequestCode = intentRequestCode
        this.permissions = permissions
        this.messageAskPermission = messageAskPermission
    }
}