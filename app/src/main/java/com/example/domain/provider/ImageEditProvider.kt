package com.example.domain.provider

import com.example.domain.model.EditRequest
import com.example.domain.model.EditResponse

interface ImageEditProvider {
    suspend fun editImage(request: EditRequest): EditResponse
}
